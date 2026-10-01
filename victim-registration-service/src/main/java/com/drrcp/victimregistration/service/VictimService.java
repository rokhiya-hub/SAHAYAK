package com.drrcp.victimregistration.service;

import com.drrcp.victimregistration.domain.Victim;
import com.drrcp.victimregistration.domain.VictimStatus;
import com.drrcp.victimregistration.dto.VictimPageResponse;
import com.drrcp.victimregistration.dto.VictimRequest;
import com.drrcp.victimregistration.dto.VictimResponse;
import com.drrcp.victimregistration.exception.DuplicateAadharException;
import com.drrcp.victimregistration.exception.ResourceNotFoundException;
import com.drrcp.victimregistration.exception.ShelterFullException;
import com.drrcp.victimregistration.exception.ShelterServiceException;
import com.drrcp.victimregistration.exception.ShelterServiceUnavailableException;
import com.drrcp.victimregistration.exception.VictimConflictException;
import com.drrcp.victimregistration.repository.VictimRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatusCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Service
@RequiredArgsConstructor
public class VictimService {

    private final VictimRepository victimRepository;
    private final WebClient shelterWebClient;

    public VictimPageResponse getAllVictims(String search, Pageable pageable) {
        Specification<Victim> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("fullName")), like));
        }

        Page<Victim> page = victimRepository.findAll(spec, pageable);
        return VictimPageResponse.from(page);
    }

    public VictimResponse getVictimById(Long victimId) {
        return VictimResponse.from(findVictimById(victimId));
    }

    @Transactional
    public VictimResponse createVictim(VictimRequest request) {
        if (victimRepository.existsByAadharNumber(request.getAadharNumber())) {
            throw new DuplicateAadharException("A victim with this Aadhaar number is already registered");
        }

        Victim victim = Victim.builder()
                .fullName(request.getFullName())
                .aadharNumber(request.getAadharNumber())
                .phoneNumber(request.getPhoneNumber())
                .age(request.getAge())
                .familySize(request.getFamilySize())
                .medicalNeeds(request.getMedicalNeeds())
                .status(VictimStatus.REGISTERED)
                .build();
        try {
            return VictimResponse.from(victimRepository.saveAndFlush(victim));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateAadharException("A victim with this Aadhaar number is already registered");
        }
    }

    @Transactional
    public VictimResponse updateVictim(Long victimId, VictimRequest request) {
        Victim victim = findVictimById(victimId);
        if (!victim.getAadharNumber().equals(request.getAadharNumber())
                && victimRepository.existsByAadharNumber(request.getAadharNumber())) {
            throw new DuplicateAadharException("A victim with this Aadhaar number is already registered");
        }

        victim.setFullName(request.getFullName());
        victim.setAadharNumber(request.getAadharNumber());
        victim.setPhoneNumber(request.getPhoneNumber());
        victim.setAge(request.getAge());
        victim.setFamilySize(request.getFamilySize());
        victim.setMedicalNeeds(request.getMedicalNeeds());
        return VictimResponse.from(victimRepository.save(victim));
    }

    @Transactional
    public void deleteVictim(Long victimId) {
        victimRepository.delete(findVictimById(victimId));
    }

    @Transactional
    public VictimResponse assignShelter(Long victimId, Long shelterId, String callerToken) {
        Victim victim = victimRepository.findByIdForUpdate(victimId)
                .orElseThrow(() -> new ResourceNotFoundException("Victim with id " + victimId + " not found"));

        try {
            shelterWebClient.post()
                    .uri("/api/shelters/{id}/checkin", shelterId)
                    .headers(headers -> headers.setBearerAuth(callerToken))
                    .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(JsonNode.class)
                            .defaultIfEmpty(JsonNodeFactory.instance.objectNode())
                            .map(body -> response.statusCode().value() == 409
                                ? new ShelterFullException(errorMessage(body, "Shelter rejected the check-in"))
                                : new ShelterServiceException("Shelter service rejected check-in with HTTP "
                                + response.statusCode().value())))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> response.createException()
                            .map(ex -> new ShelterServiceException("Shelter service returned an error")))
                    .toBodilessEntity()
                    .block();
        } catch (ShelterFullException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw new ShelterServiceUnavailableException("Shelter service is unavailable or timed out", ex);
        } catch (ShelterServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ShelterServiceException("Unable to complete shelter check-in", ex);
        }

        victim.setCurrentShelterId(shelterId);
        victim.setStatus(VictimStatus.ASSIGNED_SHELTER);
        try {
            return VictimResponse.from(victimRepository.saveAndFlush(victim));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new VictimConflictException("Victim was updated concurrently; please retry");
        }
    }

    private String errorMessage(JsonNode body, String fallback) {
        JsonNode message = body == null ? null : body.get("message");
        return message == null || message.asText().isBlank() ? fallback : message.asText();
    }

    private Victim findVictimById(Long victimId) {
        return victimRepository.findById(victimId)
                .orElseThrow(() -> new ResourceNotFoundException("Victim with id " + victimId + " not found"));
    }
}