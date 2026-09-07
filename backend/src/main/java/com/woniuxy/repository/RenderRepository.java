package com.woniuxy.repository;

import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.projection.RenderTaskSimpleProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RenderRepository extends JpaRepository<RenderPO, String> {

    /**
     * 仅查询 docUrl、userRequirements，性能更好
     */
    Optional<RenderTaskSimpleProjection> findSimpleByTaskId(String taskId);


    Optional<RenderPO> findByTaskId(String taskId);

//    @Query("select r.docUrl, r.userRequirements from RenderPO r where r.taskId = :taskId")
//    Optional<Object[]> findDocAndReqByTaskId(@Param("taskId") String taskId);
}