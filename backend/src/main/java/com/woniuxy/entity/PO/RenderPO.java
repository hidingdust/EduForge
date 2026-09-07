package com.woniuxy.entity.PO;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_task")
public class RenderPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //任务唯一标识
    @Column(unique = true, nullable = false)
    private String taskId;

    //原始文档存储地址
    private String docUrl;

    // 用户原始需求描述
    @Column(columnDefinition = "TEXT")
    private String userRequirements;

    // 任务状态：WAIT/RUNNING/SUCCESS/FAIL
    private String taskStatus;

    // 工作流全局状态JSON快照（持久化断点）
    @Column(columnDefinition = "TEXT")
    private String stateSnapshot;

    // 最终产出结果JSON
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
