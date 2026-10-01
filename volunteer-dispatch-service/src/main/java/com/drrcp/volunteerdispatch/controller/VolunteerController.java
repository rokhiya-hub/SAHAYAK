package com.drrcp.volunteerdispatch.controller;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.dto.VolunteerPageResponse;
import com.drrcp.volunteerdispatch.dto.VolunteerRequest;
import com.drrcp.volunteerdispatch.dto.VolunteerResponse;
import com.drrcp.volunteerdispatch.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping
    public VolunteerPageResponse getAllVolunteers(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) AvailabilityStatus availabilityStatus,
                                                  @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return volunteerService.getAllVolunteers(search, availabilityStatus, pageable);
    }

    @GetMapping("/{id}")
    public VolunteerResponse getVolunteerById(@PathVariable Long id) {
        return volunteerService.getVolunteerById(id);
    }

    @PostMapping
    public ResponseEntity<VolunteerResponse> createVolunteer(@Valid @RequestBody VolunteerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(volunteerService.createVolunteer(request));
    }

    @PutMapping("/{id}")
    public VolunteerResponse updateVolunteer(@PathVariable Long id, @Valid @RequestBody VolunteerRequest request) {
        return volunteerService.updateVolunteer(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolunteer(@PathVariable Long id) {
        volunteerService.deleteVolunteer(id);
        return ResponseEntity.noContent().build();
    }
}