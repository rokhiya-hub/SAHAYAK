package com.drrcp.sheltermanagement.service;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.dto.CreateShelterRequest;
import com.drrcp.sheltermanagement.dto.ShelterPageResponse;
import com.drrcp.sheltermanagement.dto.ShelterResponse;
import com.drrcp.sheltermanagement.dto.UpdateShelterRequest;
import com.drrcp.sheltermanagement.exception.ResourceNotFoundException;
import com.drrcp.sheltermanagement.exception.ShelterFullException;
import com.drrcp.sheltermanagement.repository.ShelterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ShelterRepository shelterRepository;

    public ShelterPageResponse getAllShelters(String search, ShelterStatus status, Pageable pageable) {
        Specification<Shelter> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("address")), like)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Shelter> page = spec != null ? shelterRepository.findAll(spec, pageable) : shelterRepository.findAll(pageable);
        return ShelterPageResponse.from(page);
    }

    public ShelterResponse getShelterById(Long shelterId) {
        return ShelterResponse.from(findShelterById(shelterId));
    }

    public ShelterResponse createShelter(CreateShelterRequest request) {
        Shelter shelter = Shelter.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalCapacity(request.getTotalCapacity())
                .currentOccupancy(0)
                .resourcesAvailable(request.getResourcesAvailable() == null ? List.of() : request.getResourcesAvailable())
                .status(request.getStatus() != null ? request.getStatus() : ShelterStatus.OPEN)
                .build();

        shelter.syncStatus();
        return ShelterResponse.from(shelterRepository.save(shelter));
    }

    public ShelterResponse updateShelter(Long shelterId, UpdateShelterRequest request) {
        Shelter shelter = findShelterById(shelterId);

        shelter.setName(request.getName());
        shelter.setAddress(request.getAddress());
        shelter.setLatitude(request.getLatitude());
        shelter.setLongitude(request.getLongitude());
        shelter.setTotalCapacity(request.getTotalCapacity());
        if (request.getCurrentOccupancy() != null) {
            shelter.setCurrentOccupancy(request.getCurrentOccupancy());
        }
        shelter.setResourcesAvailable(request.getResourcesAvailable() == null ? List.of() : request.getResourcesAvailable());
        if (request.getStatus() != null) {
            shelter.setStatus(request.getStatus());
        }

        validateCapacity(shelter);
        shelter.syncStatus();
        return ShelterResponse.from(shelterRepository.save(shelter));
    }

    public void deleteShelter(Long shelterId) {
        Shelter shelter = findShelterById(shelterId);
        shelterRepository.delete(shelter);
    }

    @Transactional
    public ShelterResponse checkIn(Long shelterId) {
        Shelter shelter = findShelterById(shelterId);

        if (shelter.getStatus() == ShelterStatus.CLOSED) {
            throw new IllegalStateException("Shelter is closed and cannot accept new residents");
        }
        if (shelter.getCurrentOccupancy() >= shelter.getTotalCapacity()) {
            shelter.setStatus(ShelterStatus.FULL);
            throw new ShelterFullException("Shelter " + shelterId + " is at full capacity");
        }

        shelter.setCurrentOccupancy(shelter.getCurrentOccupancy() + 1);
        shelter.syncStatus();
        try {
            return ShelterResponse.from(shelterRepository.saveAndFlush(shelter));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ShelterFullException("Shelter " + shelterId + " was updated concurrently, capacity may be full; please retry");
        }
    }

    @Transactional
    public ShelterResponse checkOut(Long shelterId) {
        Shelter shelter = findShelterById(shelterId);

        if (shelter.getCurrentOccupancy() <= 0) {
            throw new IllegalStateException("Shelter is already empty");
        }

        shelter.setCurrentOccupancy(shelter.getCurrentOccupancy() - 1);
        shelter.syncStatus();
        try {
            return ShelterResponse.from(shelterRepository.saveAndFlush(shelter));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new IllegalStateException("Shelter " + shelterId + " was updated concurrently; please retry");
        }
    }

    public List<ShelterResponse> getAvailableShelters(Integer minCapacity, Double lat, Double lng, Double radiusKm) {
        if (lat != null && lng != null) {
            return shelterRepository.findNearbyOpenShelters(lat, lng, radiusKm, minCapacity).stream()
                    .sorted(Comparator.comparingDouble(shelter -> distanceKm(lat, lng, shelter)))
                    .map(shelter -> ShelterResponse.from(shelter, distanceKm(lat, lng, shelter)))
                    .toList();
        }

        return shelterRepository.findByStatusNot(ShelterStatus.CLOSED).stream()
                .filter(shelter -> shelter.getAvailableCapacity() >= minCapacity)
                .sorted(Comparator.comparingInt(Shelter::getAvailableCapacity).reversed())
                .map(ShelterResponse::from)
                .toList();
    }

    private void validateCapacity(Shelter shelter) {
        if (shelter.getCurrentOccupancy() > shelter.getTotalCapacity()) {
            throw new IllegalStateException("Current occupancy cannot exceed total capacity");
        }
    }

    private double distanceKm(Double lat, Double lng, Shelter shelter) {
        return distanceKm(lat, lng,
                shelter.getLatitude().doubleValue(),
                shelter.getLongitude().doubleValue());
    }

    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Shelter findShelterById(Long shelterId) {
        return shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter with id " + shelterId + " not found"));
    }
}
