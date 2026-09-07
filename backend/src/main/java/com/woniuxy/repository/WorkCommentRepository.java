package com.woniuxy.repository;

import com.woniuxy.entity.WorkComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkCommentRepository extends JpaRepository<WorkComment, Long> {
}