package com.drrcp.volunteerdispatch.repository;

import com.drrcp.volunteerdispatch.domain.AvailabilityStatus;
import com.drrcp.volunteerdispatch.domain.Volunteer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long>, JpaSpecificationExecutor<Volunteer> {

    List<Volunteer> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select volunteer from Volunteer volunteer where volunteer.id = :id")
    Optional<Volunteer> findByIdForUpdate(@Param("id") Long id);
}