package com.drrcp.resourceinventory.repository;

import com.drrcp.resourceinventory.domain.ResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long>, JpaSpecificationExecutor<ResourceItem> {

    @Query("select resource from ResourceItem resource where resource.totalQuantity - resource.reservedQuantity <= :threshold")
    List<ResourceItem> findLowStock(@Param("threshold") Integer threshold);
}