package com.drrcp.victimregistration;

import com.drrcp.victimregistration.domain.MedicalNeeds;
import com.drrcp.victimregistration.domain.Victim;
import com.drrcp.victimregistration.domain.VictimStatus;
import com.drrcp.victimregistration.exception.ShelterFullException;
import com.drrcp.victimregistration.exception.ShelterServiceUnavailableException;
import com.drrcp.victimregistration.repository.VictimRepository;
import com.drrcp.victimregistration.service.VictimService;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("h2")
class ShelterAssignmentClientTest {

    private static final MockWebServer SHELTER_SERVER = startServer();

    @Autowired
    private VictimService victimService;

    @Autowired
    private VictimRepository victimRepository;

    @DynamicPropertySource
    static void shelterServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("shelter-service.url", () -> SHELTER_SERVER.url("/").toString());
        registry.add("shelter-service.connect-timeout", () -> "500ms");
        registry.add("shelter-service.response-timeout", () -> "300ms");
    }

    @BeforeEach
    void setUp() {
        victimRepository.deleteAll();
    }

    @AfterAll
    static void stopServer() throws IOException {
        SHELTER_SERVER.shutdown();
    }

    @Test
    void assignShelterForwardsCallerTokenAndUpdatesVictim() throws Exception {
        Victim victim = victimRepository.saveAndFlush(victim());
        SHELTER_SERVER.enqueue(new MockResponse().setResponseCode(200));

        var assigned = victimService.assignShelter(victim.getId(), 5L, "caller-jwt");
        RecordedRequest request = SHELTER_SERVER.takeRequest(1, TimeUnit.SECONDS);

        assertEquals("ASSIGNED_SHELTER", assigned.getStatus().name());
        assertEquals(5L, assigned.getCurrentShelterId());
        assertEquals("POST", request.getMethod());
        assertEquals("/api/shelters/5/checkin", request.getPath());
        assertEquals("Bearer caller-jwt", request.getHeader("Authorization"));
    }

    @Test
    void assignShelterPropagatesConflictMessageWithoutChangingVictim() {
        Victim victim = victimRepository.saveAndFlush(victim());
        SHELTER_SERVER.enqueue(new MockResponse()
                .setResponseCode(409)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Shelter 5 is at full capacity\"}"));

        ShelterFullException exception = assertThrows(ShelterFullException.class,
                () -> victimService.assignShelter(victim.getId(), 5L, "caller-jwt"));

        assertEquals("Shelter 5 is at full capacity", exception.getMessage());
        Victim unchanged = victimRepository.findById(victim.getId()).orElseThrow();
        assertEquals(VictimStatus.REGISTERED, unchanged.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(unchanged.getCurrentShelterId());
    }

    @Test
    void assignShelterReturnsUnavailableWhenShelterTimesOut() {
        Victim victim = victimRepository.saveAndFlush(victim());
        SHELTER_SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
            .setHeadersDelay(2, TimeUnit.SECONDS));

        assertThrows(ShelterServiceUnavailableException.class,
                () -> victimService.assignShelter(victim.getId(), 5L, "caller-jwt"));
    }

    private Victim victim() {
        return Victim.builder()
                .fullName("Ananya Rao")
                .aadharNumber("123456789012")
                .phoneNumber("9876543210")
                .age(32)
                .familySize(4)
                .medicalNeeds(MedicalNeeds.NONE)
                .build();
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