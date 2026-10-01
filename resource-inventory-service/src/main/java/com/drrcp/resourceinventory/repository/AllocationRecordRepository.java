package com.drrcp.resourceinventory.repository;

import com.drrcp.resourceinventory.domain.AllocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationRecordRepository extends JpaRepository<AllocationRecord, Long> {

    List<AllocationRecord> findByResourceItem_IdOrderByAllocatedAtDesc(Long resourceItemId);
}