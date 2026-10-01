package com.drrcp.sheltermanagement.controller;

import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.dto.CreateShelterRequest;
import com.drrcp.sheltermanagement.dto.ShelterPageResponse;
import com.drrcp.sheltermanagement.dto.ShelterResponse;
import com.drrcp.sheltermanagement.dto.UpdateShelterRequest;
import com.drrcp.sheltermanagement.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    @GetMapping
    public ShelterPageResponse getAllShelters(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) ShelterStatus status,
                                              @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return shelterService.getAllShelters(search, status, pageable);
    }

    @GetMapping("/{id}")
    public ShelterResponse getShelterById(@PathVariable Long id) {
        return shelterService.getShelterById(id);
    }

    @PostMapping
    public ResponseEntity<ShelterResponse> createShelter(@Valid @RequestBody CreateShelterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shelterService.createShelter(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShelterResponse> updateShelter(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateShelterRequest request) {
        return ResponseEntity.ok(shelterService.updateShelter(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelter(@PathVariable Long id) {
        shelterService.deleteShelter(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<ShelterResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(shelterService.checkIn(id));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ShelterResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(shelterService.checkOut(id));
    }

    @GetMapping("/available")
    public List<ShelterResponse> getAvailableShelters(@RequestParam(defaultValue = "0") Integer minCapacity,
                                                    @RequestParam(required = false) Double lat,
                                                    @RequestParam(required = false) Double lng,
                                                    @RequestParam(defaultValue = "100.0") Double radiusKm) {
        return shelterService.getAvailableShelters(minCapacity, lat, lng, radiusKm);
    }
}
