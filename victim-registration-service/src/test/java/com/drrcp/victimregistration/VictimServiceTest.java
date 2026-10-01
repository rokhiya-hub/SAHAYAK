package com.drrcp.victimregistration;

import com.drrcp.victimregistration.domain.MedicalNeeds;
import com.drrcp.victimregistration.domain.Victim;
import com.drrcp.victimregistration.dto.VictimRequest;
import com.drrcp.victimregistration.dto.VictimResponse;
import com.drrcp.victimregistration.exception.DuplicateAadharException;
import com.drrcp.victimregistration.exception.ResourceNotFoundException;
import com.drrcp.victimregistration.repository.VictimRepository;
import com.drrcp.victimregistration.service.VictimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("h2")
class VictimServiceTest {

    @Autowired
    private VictimService victimService;

    @Autowired
    private VictimRepository victimRepository;

    @BeforeEach
    void setUp() {
        victimRepository.deleteAll();
    }

    @Test
    void createVictim_setsRegisteredStatusAndTimestamp() {
        VictimResponse created = victimService.createVictim(request("Ananya Rao", "123456789012"));

        assertEquals("Ananya Rao", created.getFullName());
        assertEquals("123456789012", created.getAadharNumber());
        assertEquals(MedicalNeeds.NONE, created.getMedicalNeeds());
        assertEquals("REGISTERED", created.getStatus().name());
        assertEquals(0L, created.getVersion());
        org.junit.jupiter.api.Assertions.assertNotNull(created.getRegisteredAt());
    }

    @Test
    void createVictim_rejectsDuplicateAadharNumber() {
        victimService.createVictim(request("Ananya Rao", "123456789012"));

        assertThrows(DuplicateAadharException.class,
                () -> victimService.createVictim(request("Ravi Kumar", "123456789012")));
    }

    @Test
    void listVictims_filtersByNameAndReturnsPageMetadata() {
        victimService.createVictim(request("Ananya Rao", "123456789012"));
        victimService.createVictim(request("Ravi Kumar", "234567890123"));

        var page = victimService.getAllVictims("ananya", PageRequest.of(0, 5));

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Ananya Rao", page.getContent().getFirst().getFullName());
    }

    @Test
    void updateVictim_updatesEditableFieldsWithoutClearingShelter() {
        Victim created = victimRepository.saveAndFlush(victim("123456789012", "Ananya Rao"));
        created.setCurrentShelterId(8L);
        created = victimRepository.saveAndFlush(created);

        VictimResponse updated = victimService.updateVictim(created.getId(), request("Ananya Singh", "123456789012"));

        assertEquals("Ananya Singh", updated.getFullName());
        assertEquals(8L, updated.getCurrentShelterId());
    }

    @Test
    void deleteVictim_removesRecord() {
        Victim created = victimRepository.saveAndFlush(victim("123456789012", "Ananya Rao"));

        victimService.deleteVictim(created.getId());

        assertNull(victimRepository.findById(created.getId()).orElse(null));
    }

    @Test
    void getVictimById_whenMissing_returnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> victimService.getVictimById(999L));
    }

    private VictimRequest request(String name, String aadharNumber) {
        return VictimRequest.builder()
                .fullName(name)
                .aadharNumber(aadharNumber)
                .phoneNumber("9876543210")
                .age(32)
                .familySize(4)
                .medicalNeeds(MedicalNeeds.NONE)
                .build();
    }

    private Victim victim(String aadharNumber, String name) {
        return Victim.builder()
                .fullName(name)
                .aadharNumber(aadharNumber)
                .phoneNumber("9876543210")
                .age(32)
                .familySize(4)
                .medicalNeeds(MedicalNeeds.NONE)
                .build();
    }
}