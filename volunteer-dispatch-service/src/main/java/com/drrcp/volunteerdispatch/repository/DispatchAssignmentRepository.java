package com.drrcp.volunteerdispatch.repository;

import com.drrcp.volunteerdispatch.domain.DispatchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    List<DispatchAssignment> findByVolunteer_IdOrderByAssignedAtDesc(Long volunteerId);
}