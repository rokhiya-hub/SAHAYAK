package com.drrcp.volunteerdispatch;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.domain.DispatchStatus;
import com.drrcp.volunteerdispatch.domain.Volunteer;
import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import com.drrcp.volunteerdispatch.dto.DispatchRequest;
import com.drrcp.volunteerdispatch.exception.DispatchConflictException;
import com.drrcp.volunteerdispatch.exception.ShelterServiceUnavailableException;
import com.drrcp.volunteerdispatch.repository.DispatchAssignmentRepository;
import com.drrcp.volunteerdispatch.repository.VolunteerRepository;
import com.drrcp.volunteerdispatch.service.VolunteerService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("h2")
class DispatchIntegrationTest {

    private static final MockWebServer SHELTER_SERVER = startServer();

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private DispatchAssignmentRepository dispatchAssignmentRepository;

    @DynamicPropertySource
    static void shelterProperties(DynamicPropertyRegistry registry) {
        registry.add("shelter-service.url", () -> SHELTER_SERVER.url("/").toString());
        registry.add("shelter-service.connect-timeout", () -> "500ms");
        registry.add("shelter-service.response-timeout", () -> "300ms");
    }

    @BeforeEach
    void setUp() {
        dispatchAssignmentRepository.deleteAll();
        volunteerRepository.deleteAll();
    }

    @AfterAll
    static void stopServer() throws IOException {
        SHELTER_SERVER.shutdown();
    }

    @Test
    void assignChecksOpenShelterForwardsTokenAndDispatchesVolunteer() throws Exception {
        Volunteer volunteer = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "MEDICAL", 12.9716, 77.5946));
        SHELTER_SERVER.enqueue(shelterResponse("OPEN", 20));

        DispatchRequest request = dispatchRequest(volunteer.getId(), 5L, VolunteerSkill.MEDICAL);
        var assignment = volunteerService.assign(request, "caller-token");
        RecordedRequest upstream = SHELTER_SERVER.takeRequest(1, TimeUnit.SECONDS);
        Volunteer updated = volunteerRepository.findById(volunteer.getId()).orElseThrow();

        assertEquals(DispatchStatus.ASSIGNED, assignment.getStatus());
        assertEquals("MEDICAL", assignment.getTask());
        assertEquals(AvailabilityStatus.DISPATCHED, updated.getAvailabilityStatus());
        assertEquals("GET", upstream.getMethod());
        assertEquals("/api/shelters/5", upstream.getPath());
        assertEquals("Bearer caller-token", upstream.getHeader("Authorization"));
    }

    @Test
    void assignRejectsClosedOrFullShelterWithoutChangingVolunteer() {
        Volunteer volunteer = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "MEDICAL", 12.9716, 77.5946));
        SHELTER_SERVER.enqueue(shelterResponse("FULL", 0));

        assertThrows(DispatchConflictException.class, () -> volunteerService.assign(
                dispatchRequest(volunteer.getId(), 5L, VolunteerSkill.MEDICAL), "caller-token"));

        Volunteer unchanged = volunteerRepository.findById(volunteer.getId()).orElseThrow();
        assertEquals(AvailabilityStatus.AVAILABLE, unchanged.getAvailabilityStatus());
        assertEquals(0, dispatchAssignmentRepository.count());
    }

    @Test
    void assignReturnsUnavailableWhenShelterServiceTimesOut() {
        Volunteer volunteer = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "MEDICAL", 12.9716, 77.5946));
        SHELTER_SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeadersDelay(2, TimeUnit.SECONDS));

        assertThrows(ShelterServiceUnavailableException.class, () -> volunteerService.assign(
                dispatchRequest(volunteer.getId(), 5L, VolunteerSkill.MEDICAL), "caller-token"));
        assertEquals(AvailabilityStatus.AVAILABLE,
                volunteerRepository.findById(volunteer.getId()).orElseThrow().getAvailabilityStatus());
    }

    @Test
    void suggestRanksExactSkillAndCloserVolunteersHigher() {
        volunteerRepository.saveAndFlush(volunteer("Nearby medical", "MEDICAL", 12.9716, 77.5946));
        volunteerRepository.saveAndFlush(volunteer("Nearby driver", "DRIVING", 12.9716, 77.5946));
        volunteerRepository.saveAndFlush(volunteer("Distant medical", "MEDICAL", 13.9716, 77.5946));
        SHELTER_SERVER.enqueue(shelterResponse("OPEN", 20));

        var ranked = volunteerService.suggest(5L, VolunteerSkill.MEDICAL, "caller-token");
        assertEquals(3, ranked.size());
        assertEquals("Nearby medical", ranked.getFirst().getVolunteer().getName());
        assertEquals(0.0, ranked.getFirst().getDistanceKm(), 0.01);
        assertEquals(11.0, ranked.getFirst().getScore(), 0.01);
    }

    @Test
    void shelterServerErrorBecomes503Exception() {
        Volunteer volunteer = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "MEDICAL", 12.9716, 77.5946));
        SHELTER_SERVER.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(ShelterServiceUnavailableException.class, () -> volunteerService.assign(
                dispatchRequest(volunteer.getId(), 5L, VolunteerSkill.MEDICAL), "caller-token"));
    }

    private DispatchRequest dispatchRequest(Long volunteerId, Long shelterId, VolunteerSkill skill) {
        DispatchRequest request = new DispatchRequest();
        request.setVolunteerId(volunteerId);
        request.setShelterId(shelterId);
        request.setRequiredSkill(skill);
        return request;
    }

    private Volunteer volunteer(String name, String skill, double latitude, double longitude) {
        return Volunteer.builder()
                .name(name)
                .phone("9876543210")
                .skills(Set.of(VolunteerSkill.valueOf(skill)))
                .latitude(BigDecimal.valueOf(latitude))
                .longitude(BigDecimal.valueOf(longitude))
                .build();
    }

    private MockResponse shelterResponse(String status, int availableCapacity) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":5,"status":"%s","availableCapacity":%d,"latitude":12.9716,"longitude":77.5946}
                        """.formatted(status, availableCapacity));
    }

    private static MockWebServer startServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}