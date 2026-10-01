package com.drrcp.sheltermanagement;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.exception.ShelterFullException;
import com.drrcp.sheltermanagement.repository.ShelterRepository;
import com.drrcp.sheltermanagement.service.ShelterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShelterServiceConcurrencyTest {

    @Autowired
    private ShelterService shelterService;

    @Autowired
    private ShelterRepository shelterRepository;

    @BeforeEach
    void setUp() {
        shelterRepository.deleteAll();
    }

    @Test
    void checkInShouldAllowOnlyOneConcurrentEntryWhenCapacityIsFull() throws InterruptedException, ExecutionException {
        Shelter shelter = shelterRepository.save(Shelter.builder()
                .name("Central Shelter")
                .address("Test Address")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .totalCapacity(1)
                .currentOccupancy(0)
                .status(ShelterStatus.OPEN)
                .resourcesAvailable(List.of("Food", "Blankets"))
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            tasks.add(() -> {
                try {
                    shelterService.checkIn(shelter.getId());
                    return true;
                } catch (ShelterFullException ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int successfulReservations = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulReservations++;
            }
        }

        assertEquals(1, successfulReservations);
        Shelter updated = shelterRepository.findById(shelter.getId()).orElseThrow();
        assertEquals(1, updated.getCurrentOccupancy());
    }
}
