package com.drrcp.victimregistration.repository;

import com.drrcp.victimregistration.domain.Victim;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VictimRepository extends JpaRepository<Victim, Long>, JpaSpecificationExecutor<Victim> {

    boolean existsByAadharNumber(String aadharNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select victim from Victim victim where victim.id = :id")
    Optional<Victim> findByIdForUpdate(@Param("id") Long id);
}