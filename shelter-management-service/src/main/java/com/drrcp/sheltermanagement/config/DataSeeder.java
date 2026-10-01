package com.drrcp.sheltermanagement.config;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ShelterRepository shelterRepository;

    @Override
    public void run(String... args) {
        if (shelterRepository.count() > 0) {
            return;
        }

        shelterRepository.saveAll(List.of(
                Shelter.builder()
                        .name("Central Community Shelter")
                        .address("MG Road, Bengaluru, Karnataka")
                        .latitude(new BigDecimal("12.9716"))
                        .longitude(new BigDecimal("77.5946"))
                        .totalCapacity(200)
                        .currentOccupancy(87)
                        .status(ShelterStatus.OPEN)
                        .resourcesAvailable(List.of("Food", "Drinking Water", "Blankets", "Medical Kit"))
                        .build(),
                Shelter.builder()
                        .name("East End Relief Center")
                        .address("Whitefield Main Road, Bengaluru")
                        .latitude(new BigDecimal("12.9698"))
                        .longitude(new BigDecimal("77.7500"))
                        .totalCapacity(150)
                        .currentOccupancy(150)
                        .status(ShelterStatus.FULL)
                        .resourcesAvailable(List.of("Food", "Water", "Blankets"))
                        .build(),
                Shelter.builder()
                        .name("North Block Shelter")
                        .address("Hebbal, Bengaluru")
                        .latitude(new BigDecimal("13.0358"))
                        .longitude(new BigDecimal("77.5970"))
                        .totalCapacity(100)
                        .currentOccupancy(32)
                        .status(ShelterStatus.OPEN)
                        .resourcesAvailable(List.of("Food", "Blankets", "First Aid"))
                        .build(),
                Shelter.builder()
                        .name("South City Shelter")
                        .address("Jayanagar, Bengaluru")
                        .latitude(new BigDecimal("12.9250"))
                        .longitude(new BigDecimal("77.5938"))
                        .totalCapacity(80)
                        .currentOccupancy(0)
                        .status(ShelterStatus.OPEN)
                        .resourcesAvailable(List.of("Tents", "Drinking Water"))
                        .build(),
                Shelter.builder()
                        .name("Riverside Temporary Camp")
                        .address("Koramangala, Bengaluru")
                        .latitude(new BigDecimal("12.9352"))
                        .longitude(new BigDecimal("77.6245"))
                        .totalCapacity(50)
                        .currentOccupancy(12)
                        .status(ShelterStatus.CLOSED)
                        .resourcesAvailable(List.of("Food"))
                        .build()));
    }
}