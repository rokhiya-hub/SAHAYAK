package com.drrcp.victimregistration.controller;

import com.drrcp.victimregistration.dto.VictimPageResponse;
import com.drrcp.victimregistration.dto.VictimRequest;
import com.drrcp.victimregistration.dto.VictimResponse;
import com.drrcp.victimregistration.service.VictimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/victims")
@RequiredArgsConstructor
public class VictimController {

    private final VictimService victimService;

    @GetMapping
    public VictimPageResponse getAllVictims(@RequestParam(required = false) String search,
                                            @PageableDefault(size = 10, sort = "fullName") Pageable pageable) {
        return victimService.getAllVictims(search, pageable);
    }

    @GetMapping("/{id}")
    public VictimResponse getVictimById(@PathVariable Long id) {
        return victimService.getVictimById(id);
    }

    @PostMapping
    public ResponseEntity<VictimResponse> createVictim(@Valid @RequestBody VictimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(victimService.createVictim(request));
    }

    @PutMapping("/{id}")
    public VictimResponse updateVictim(@PathVariable Long id, @Valid @RequestBody VictimRequest request) {
        return victimService.updateVictim(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVictim(@PathVariable Long id) {
        victimService.deleteVictim(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-shelter/{shelterId}")
    public VictimResponse assignShelter(@PathVariable Long id,
                                        @PathVariable Long shelterId,
                                        @AuthenticationPrincipal Jwt jwt) {
        return victimService.assignShelter(id, shelterId, jwt.getTokenValue());
    }
}