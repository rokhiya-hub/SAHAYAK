package com.drrcp.volunteerdispatch.service;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.domain.DispatchAssignment;
import com.drrcp.volunteerdispatch.domain.DispatchStatus;
import com.drrcp.volunteerdispatch.domain.Volunteer;
import com.drrcp.volunteerdispatch.domain.VolunteerSkill;
import com.drrcp.volunteerdispatch.dto.DispatchRequest;
import com.drrcp.volunteerdispatch.dto.DispatchResponse;
import com.drrcp.volunteerdispatch.dto.ShelterResponse;
import com.drrcp.volunteerdispatch.dto.VolunteerPageResponse;
import com.drrcp.volunteerdispatch.dto.VolunteerRequest;
import com.drrcp.volunteerdispatch.dto.VolunteerResponse;
import com.drrcp.volunteerdispatch.dto.VolunteerSuggestion;
import com.drrcp.volunteerdispatch.exception.DispatchConflictException;
import com.drrcp.volunteerdispatch.exception.ResourceNotFoundException;
import com.drrcp.volunteerdispatch.exception.ShelterServiceUnavailableException;
import com.drrcp.volunteerdispatch.repository.DispatchAssignmentRepository;
import com.drrcp.volunteerdispatch.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatusCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final VolunteerRepository volunteerRepository;
    private final DispatchAssignmentRepository dispatchAssignmentRepository;
    private final WebClient shelterWebClient;

    public VolunteerPageResponse getAllVolunteers(String search, AvailabilityStatus availabilityStatus,
                                                   Pageable pageable) {
        Specification<Volunteer> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (availabilityStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("availabilityStatus"), availabilityStatus));
        }

        Page<Volunteer> page = volunteerRepository.findAll(spec, pageable);
        return VolunteerPageResponse.from(page);
    }

    public VolunteerResponse getVolunteerById(Long volunteerId) {
        return VolunteerResponse.from(findVolunteerById(volunteerId));
    }

    @Transactional
    public VolunteerResponse createVolunteer(VolunteerRequest request) {
        Volunteer volunteer = Volunteer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .skills(new HashSet<>(request.getSkills()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .availabilityStatus(request.getAvailabilityStatus() == null
                        ? AvailabilityStatus.AVAILABLE : request.getAvailabilityStatus())
                .build();
        return VolunteerResponse.from(volunteerRepository.saveAndFlush(volunteer));
    }

    @Transactional
    public VolunteerResponse updateVolunteer(Long volunteerId, VolunteerRequest request) {
        Volunteer volunteer = findVolunteerById(volunteerId);
        volunteer.setName(request.getName());
        volunteer.setPhone(request.getPhone());
        volunteer.getSkills().clear();
        volunteer.getSkills().addAll(request.getSkills());
        volunteer.setLatitude(request.getLatitude());
        volunteer.setLongitude(request.getLongitude());
        if (request.getAvailabilityStatus() != null) {
            volunteer.setAvailabilityStatus(request.getAvailabilityStatus());
        }
        volunteerRepository.flush();
        return VolunteerResponse.from(volunteer);
    }

    @Transactional
    public void deleteVolunteer(Long volunteerId) {
        volunteerRepository.delete(findVolunteerById(volunteerId));
    }

    @Transactional
    public DispatchResponse assign(DispatchRequest request, String callerToken) {
        Volunteer volunteer = volunteerRepository.findByIdForUpdate(request.getVolunteerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Volunteer with id " + request.getVolunteerId() + " not found"));

        if (volunteer.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new DispatchConflictException("Volunteer " + volunteer.getId() + " is not available");
        }
        if (!volunteer.getSkills().contains(request.getRequiredSkill())) {
            throw new DispatchConflictException("Volunteer does not have the required skill "
                    + request.getRequiredSkill());
        }

        verifyShelter(request.getShelterId(), callerToken, false);
        volunteer.setAvailabilityStatus(AvailabilityStatus.DISPATCHED);

        DispatchAssignment assignment = DispatchAssignment.builder()
                .volunteer(volunteer)
                .shelterId(request.getShelterId())
                .task(request.getRequiredSkill().name())
                .status(DispatchStatus.ASSIGNED)
                .build();
        try {
            volunteerRepository.flush();
            return DispatchResponse.from(dispatchAssignmentRepository.saveAndFlush(assignment));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new DispatchConflictException("Volunteer was updated concurrently; please retry");
        }
    }

    public List<VolunteerSuggestion> suggest(Long shelterId, VolunteerSkill skill, String callerToken) {
        ShelterResponse shelter = verifyShelter(shelterId, callerToken, true);
        return volunteerRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE).stream()
                .map(volunteer -> suggestion(volunteer, shelter, skill))
                .sorted(Comparator.comparingDouble(VolunteerSuggestion::getScore).reversed()
                        .thenComparingDouble(VolunteerSuggestion::getDistanceKm))
                .toList();
    }

    private VolunteerSuggestion suggestion(Volunteer volunteer, ShelterResponse shelter, VolunteerSkill skill) {
        double distanceKm = distanceKm(
                volunteer.getLatitude().doubleValue(), volunteer.getLongitude().doubleValue(),
                shelter.getLatitude().doubleValue(), shelter.getLongitude().doubleValue());
        double skillBonus = volunteer.getSkills().contains(skill) ? 10.0 : 0.0;
        double distanceBonus = 1.0 / (1.0 + distanceKm);
        return VolunteerSuggestion.builder()
                .volunteer(VolunteerResponse.from(volunteer))
                .distanceKm(distanceKm)
                .score(skillBonus + distanceBonus)
                .build();
    }

    private ShelterResponse verifyShelter(Long shelterId, String callerToken, boolean requireCoordinates) {
        ShelterResponse shelter;
        try {
            shelter = shelterWebClient.get()
                    .uri("/api/shelters/{id}", shelterId)
                    .headers(headers -> headers.setBearerAuth(callerToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.createException()
                            .map(ex -> new ShelterServiceUnavailableException(
                                    "Shelter service returned HTTP " + response.statusCode().value(), ex)))
                    .bodyToMono(ShelterResponse.class)
                    .block();
        } catch (ShelterServiceUnavailableException ex) {
            throw ex;
        } catch (WebClientRequestException | WebClientResponseException ex) {
            throw new ShelterServiceUnavailableException("Shelter service is unavailable or timed out", ex);
        } catch (RuntimeException ex) {
            throw new ShelterServiceUnavailableException("Unable to verify shelter availability", ex);
        }

        if (shelter == null || shelter.getStatus() == null || shelter.getAvailableCapacity() == null
                || (requireCoordinates && (shelter.getLatitude() == null || shelter.getLongitude() == null))) {
            throw new ShelterServiceUnavailableException("Shelter service returned incomplete shelter details", null);
        }
        if (!"OPEN".equalsIgnoreCase(shelter.getStatus()) || shelter.getAvailableCapacity() <= 0) {
            throw new DispatchConflictException("Shelter " + shelterId + " is not open or has no available capacity");
        }
        return shelter;
    }

    private double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Volunteer findVolunteerById(Long volunteerId) {
        return volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer with id " + volunteerId + " not found"));
    }
}