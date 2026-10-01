package com.drrcp.sheltermanagement.repository;

import com.drrcp.sheltermanagement.domain.Shelter;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long>, JpaSpecificationExecutor<Shelter> {

    List<Shelter> findByStatusNot(ShelterStatus status);

    @Query(value = """
            SELECT s.* FROM shelters s
            WHERE s.status <> 'CLOSED'
              AND (s.total_capacity - s.current_occupancy) >= :minCapacity
              AND 6371.0 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS((s.latitude - :lat) / 2)), 2) +
                    COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                    POWER(SIN(RADIANS((s.longitude - :lng) / 2)), 2)
                  )) <= :radiusKm
            """, nativeQuery = true)
    List<Shelter> findNearbyOpenShelters(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("radiusKm") double radiusKm,
                                         @Param("minCapacity") Integer minCapacity);
}