package com.drrcp.volunteerdispatch;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.domain.Volunteer;
import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import com.drrcp.volunteerdispatch.dto.VolunteerRequest;
import com.drrcp.volunteerdispatch.dto.VolunteerResponse;
import com.drrcp.volunteerdispatch.exception.ResourceNotFoundException;
import com.drrcp.volunteerdispatch.repository.DispatchAssignmentRepository;
import com.drrcp.volunteerdispatch.repository.VolunteerRepository;
import com.drrcp.volunteerdispatch.service.VolunteerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("h2")
class VolunteerServiceTest {

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private DispatchAssignmentRepository dispatchAssignmentRepository;

    @BeforeEach
    void setUp() {
        dispatchAssignmentRepository.deleteAll();
        volunteerRepository.deleteAll();
    }

    @Test
    void createVolunteer_defaultsToAvailable() {
        VolunteerResponse created = volunteerService.createVolunteer(request("Asha Rao", "9876543210",
                Set.of(VolunteerSkill.MEDICAL), "12.9716", "77.5946"));

        assertEquals("Asha Rao", created.getName());
        assertEquals(AvailabilityStatus.AVAILABLE, created.getAvailabilityStatus());
        assertNotNull(created.getVersion());
    }

    @Test
    void listVolunteersFiltersBySearchAndAvailability() {
        volunteerService.createVolunteer(request("Asha Rao", "9876543210", Set.of(VolunteerSkill.MEDICAL),
                "12.9716", "77.5946"));
        volunteerService.createVolunteer(request("Ravi Kumar", "9876543211", Set.of(VolunteerSkill.DRIVING),
                "13.0000", "77.6000"));

        var page = volunteerService.getAllVolunteers("asha", AvailabilityStatus.AVAILABLE, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Asha Rao", page.getContent().getFirst().getName());
    }

    @Test
    void updateVolunteerChangesFields() {
        Volunteer saved = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "9876543210"));

        VolunteerResponse updated = volunteerService.updateVolunteer(saved.getId(), request("Asha Singh", "9876543210",
                Set.of(VolunteerSkill.MEDICAL, VolunteerSkill.LOGISTICS), "12.9800", "77.5900"));

        assertEquals("Asha Singh", updated.getName());
        assertEquals(2, updated.getSkills().size());
        assertEquals(new BigDecimal("12.9800"), updated.getLatitude());
    }

    @Test
    void deleteVolunteerRemovesRecord() {
        Volunteer saved = volunteerRepository.saveAndFlush(volunteer("Asha Rao", "9876543210"));

        volunteerService.deleteVolunteer(saved.getId());

        assertThrows(ResourceNotFoundException.class, () -> volunteerService.getVolunteerById(saved.getId()));
    }

    private VolunteerRequest request(String name, String phone, Set<VolunteerSkill> skills,
                                     String latitude, String longitude) {
        return VolunteerRequest.builder()
                .name(name)
                .phone(phone)
                .skills(skills)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal(longitude))
                .build();
    }

    private Volunteer volunteer(String name, String phone) {
        return Volunteer.builder()
                .name(name)
                .phone(phone)
                .skills(Set.of(VolunteerSkill.MEDICAL))
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .build();
    }
}