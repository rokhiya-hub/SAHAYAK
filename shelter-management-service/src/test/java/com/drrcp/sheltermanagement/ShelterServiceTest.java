package com.drrcp.sheltermanagement;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.dto.CreateShelterRequest;
import com.drrcp.sheltermanagement.dto.ShelterPageResponse;
import com.drrcp.sheltermanagement.dto.ShelterResponse;
import com.drrcp.sheltermanagement.dto.UpdateShelterRequest;
import com.drrcp.sheltermanagement.exception.ResourceNotFoundException;
import com.drrcp.sheltermanagement.exception.ShelterFullException;
import com.drrcp.sheltermanagement.repository.ShelterRepository;
import com.drrcp.sheltermanagement.service.ShelterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShelterServiceTest {

    @Autowired
    private ShelterService shelterService;

    @Autowired
    private ShelterRepository shelterRepository;

    @BeforeEach
    void setUp() {
        shelterRepository.deleteAll();
    }

    private Shelter createPersisted(int totalCapacity, int currentOccupancy) {
        return shelterRepository.save(Shelter.builder()
                .name("Test Shelter")
                .address("Test Address")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .totalCapacity(totalCapacity)
                .currentOccupancy(currentOccupancy)
                .status(ShelterStatus.OPEN)
                .resourcesAvailable(List.of("Food"))
                .build());
    }

    @Test
    void createShelter_persistsShelterWithOpenStatus() {
        CreateShelterRequest request = CreateShelterRequest.builder()
                .name("Hillside Shelter")
                .address("Kambathalli, Mysuru")
                .latitude(new BigDecimal("12.3375"))
                .longitude(new BigDecimal("76.6266"))
                .totalCapacity(120)
                .resourcesAvailable(List.of("Food", "Tents"))
                .build();

        ShelterResponse response = shelterService.createShelter(request);

        assertNotNull(response.getId());
        assertEquals("Hillside Shelter", response.getName());
        assertEquals(ShelterStatus.OPEN, response.getStatus());
        assertEquals(0, response.getCurrentOccupancy());
        assertEquals(120, response.getAvailableCapacity());
    }

    @Test
    void getShelterById_returnsShelter() {
        Shelter shelter = createPersisted(100, 10);

        ShelterResponse response = shelterService.getShelterById(shelter.getId());

        assertEquals(shelter.getId(), response.getId());
        assertEquals(shelter.getName(), response.getName());
    }

    @Test
    void getShelterById_whenMissing_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> shelterService.getShelterById(999L));
    }

    @Test
    void getAllShelters_returnsAllSheltersPaged() {
        createPersisted(100, 0);
        createPersisted(50, 0);

        ShelterPageResponse page = shelterService.getAllShelters(null, null, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void getAllShelters_filtersByNameAndAddress() {
        createPersisted(100, 0);            // "Test Shelter"
        createShelterAt("Hillside Shelter", "12.3375", "76.6266", 10, 0);

        ShelterPageResponse byName = shelterService.getAllShelters("hillside", null, PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());
        assertEquals("Hillside Shelter", byName.getContent().get(0).getName());

        ShelterPageResponse byAddress = shelterService.getAllShelters("test address", null, PageRequest.of(0, 10));
        assertEquals(2, byAddress.getTotalElements());
    }

    @Test
    void getAllShelters_filtersByStatus() {
        createPersisted(100, 100);          // FULL
        createPersisted(50, 0);             // OPEN

        ShelterPageResponse page = shelterService.getAllShelters(null, ShelterStatus.OPEN, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(ShelterStatus.OPEN, page.getContent().get(0).getStatus());
    }

    @Test
    void updateShelter_updatesFields() {
        Shelter shelter = createPersisted(100, 10);
        UpdateShelterRequest request = UpdateShelterRequest.builder()
                .name("Renamed Shelter")
                .address("New Address")
                .latitude(new BigDecimal("12.9700"))
                .longitude(new BigDecimal("77.5900"))
                .totalCapacity(200)
                .currentOccupancy(20)
                .resourcesAvailable(List.of("Water"))
                .build();

        ShelterResponse response = shelterService.updateShelter(shelter.getId(), request);

        assertEquals("Renamed Shelter", response.getName());
        assertEquals(200, response.getTotalCapacity());
        assertEquals(20, response.getCurrentOccupancy());
        assertEquals(180, response.getAvailableCapacity());
    }

    @Test
    void updateShelter_whenOccupancyExceedsCapacity_throws() {
        Shelter shelter = createPersisted(100, 10);
        UpdateShelterRequest request = UpdateShelterRequest.builder()
                .name("Test Shelter")
                .address("Test Address")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .totalCapacity(5)
                .currentOccupancy(10)
                .resourcesAvailable(List.of("Food"))
                .build();

        assertThrows(IllegalStateException.class, () -> shelterService.updateShelter(shelter.getId(), request));
    }

    @Test
    void updateShelter_syncsFullStatusWhenFilled() {
        Shelter shelter = createPersisted(100, 10);
        UpdateShelterRequest request = UpdateShelterRequest.builder()
                .name("Test Shelter")
                .address("Test Address")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .totalCapacity(10)
                .currentOccupancy(10)
                .resourcesAvailable(List.of("Food"))
                .build();

        ShelterResponse response = shelterService.updateShelter(shelter.getId(), request);

        assertEquals(ShelterStatus.FULL, response.getStatus());
        assertEquals(0, response.getAvailableCapacity());
    }

    @Test
    void deleteShelter_removesShelter() {
        Shelter shelter = createPersisted(100, 0);

        shelterService.deleteShelter(shelter.getId());

        assertFalse(shelterRepository.existsById(shelter.getId()));
    }

    @Test
    void checkIn_incrementsOccupancyAndStaysOpen() {
        Shelter shelter = createPersisted(100, 5);

        shelterService.checkIn(shelter.getId());

        Shelter updated = shelterRepository.findById(shelter.getId()).orElseThrow();
        assertEquals(6, updated.getCurrentOccupancy());
        assertEquals(ShelterStatus.OPEN, updated.getStatus());
    }

    @Test
    void checkIn_flipsStatusToFullAtCapacity() {
        Shelter shelter = createPersisted(1, 0);

        shelterService.checkIn(shelter.getId());

        Shelter updated = shelterRepository.findById(shelter.getId()).orElseThrow();
        assertEquals(1, updated.getCurrentOccupancy());
        assertEquals(ShelterStatus.FULL, updated.getStatus());
    }

    @Test
    void checkIn_whenAtCapacity_throwsShelterFull() {
        Shelter shelter = createPersisted(1, 1);

        assertThrows(ShelterFullException.class, () -> shelterService.checkIn(shelter.getId()));

        Shelter updated = shelterRepository.findById(shelter.getId()).orElseThrow();
        assertEquals(ShelterStatus.FULL, updated.getStatus());
    }

    @Test
    void checkIn_whenClosed_throwsIllegalState() {
        Shelter shelter = createPersisted(100, 0);
        shelter.setStatus(ShelterStatus.CLOSED);
        shelterRepository.save(shelter);

        assertThrows(IllegalStateException.class, () -> shelterService.checkIn(shelter.getId()));
    }

    @Test
    void checkOut_decrementsOccupancy() {
        Shelter shelter = createPersisted(100, 5);

        shelterService.checkOut(shelter.getId());

        Shelter updated = shelterRepository.findById(shelter.getId()).orElseThrow();
        assertEquals(4, updated.getCurrentOccupancy());
    }

    @Test
    void checkOut_whenEmpty_throwsIllegalState() {
        Shelter shelter = createPersisted(100, 0);

        assertThrows(IllegalStateException.class, () -> shelterService.checkOut(shelter.getId()));
    }

    @Test
    void getAvailableShelters_filtersByCapacityAndExcludesClosed() {
        createPersisted(100, 10);       // 90 available
        createPersisted(10, 10);        // full
        Shelter closed = createPersisted(100, 0);
        closed.setStatus(ShelterStatus.CLOSED);
        shelterRepository.save(closed);

        List<ShelterResponse> available = shelterService.getAvailableShelters(50, null, null, null);

        assertEquals(1, available.size());
        assertEquals(90, available.get(0).getAvailableCapacity());
        assertNull(available.get(0).getDistanceKm());
    }

    @Test
    void getAvailableShelters_withoutLocation_sortsByAvailableCapacityDescending() {
        createPersisted(50, 0);         // 50 available
        createPersisted(200, 0);        // 200 available
        createPersisted(100, 0);        // 100 available

        List<ShelterResponse> available = shelterService.getAvailableShelters(0, null, null, null);

        assertEquals(3, available.size());
        assertEquals(200, available.get(0).getAvailableCapacity());
        assertEquals(100, available.get(1).getAvailableCapacity());
        assertEquals(50, available.get(2).getAvailableCapacity());
    }

    @Test
    void getAvailableShelters_withLocation_sortsNearestFirstWithinRadius() {
        createShelterAt("Bengaluru Centre", "12.9716", "77.5946", 100, 0);
        createShelterAt("Ring Road East", "12.9716", "77.9000", 50, 0);
        createShelterAt("Hyderabad North", "17.3850", "78.4867", 80, 0);

        List<ShelterResponse> available = shelterService.getAvailableShelters(0, 12.9716, 77.5946, 100.0);

        assertEquals(2, available.size());
        assertEquals("Bengaluru Centre", available.get(0).getName());
        assertEquals("Ring Road East", available.get(1).getName());
        assertEquals(0.0, available.get(0).getDistanceKm(), 1.0);
        assertTrue(available.get(1).getDistanceKm() <= 100.0);
    }

    @Test
    void getAvailableShelters_withLocation_excludesSheltersOutsideRadius() {
        createShelterAt("Bengaluru Centre", "12.9716", "77.5946", 100, 0);
        createShelterAt("Ring Road East", "12.9716", "77.9000", 50, 0);
        createShelterAt("Hyderabad North", "17.3850", "78.4867", 80, 0);

        List<ShelterResponse> available = shelterService.getAvailableShelters(0, 12.9716, 77.5946, 30.0);

        assertEquals(1, available.size());
        assertEquals("Bengaluru Centre", available.get(0).getName());
    }

    private void createShelterAt(String name, String lat, String lng, int capacity, int occupancy) {
        shelterRepository.save(Shelter.builder()
                .name(name)
                .address("Test Address")
                .latitude(new BigDecimal(lat))
                .longitude(new BigDecimal(lng))
                .totalCapacity(capacity)
                .currentOccupancy(occupancy)
                .status(ShelterStatus.OPEN)
                .resourcesAvailable(List.of("Food"))
                .build());
    }
}