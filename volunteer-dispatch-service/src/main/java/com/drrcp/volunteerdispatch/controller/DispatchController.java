package com.drrcp.volunteerdispatch.controller;

import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import com.drrcp.volunteerdispatch.dto.DispatchRequest;
import com.drrcp.volunteerdispatch.dto.DispatchResponse;
import com.drrcp.volunteerdispatch.dto.VolunteerSuggestion;
import com.drrcp.volunteerdispatch.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final VolunteerService volunteerService;

    @PostMapping("/assign")
    public ResponseEntity<DispatchResponse> assign(@Valid @RequestBody DispatchRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(volunteerService.assign(request, jwt.getTokenValue()));
    }

    @GetMapping("/suggest")
    public List<VolunteerSuggestion> suggest(@RequestParam Long shelterId,
                                             @RequestParam VolunteerSkill skill,
                                             @AuthenticationPrincipal Jwt jwt) {
        return volunteerService.suggest(shelterId, skill, jwt.getTokenValue());
    }
}