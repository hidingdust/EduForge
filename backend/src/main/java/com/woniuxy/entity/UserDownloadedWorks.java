package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_downloaded_works",
        // 唯一索引：一个用户针对同一个资源仅存在一条下载记录
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "works_id"})
        })
public class UserDownloadedWorks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 用户ID，关联 user 表 user_id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // 资源ID，关联 works 表 works_id
    @Column(name = "works_id", nullable = false)
    private Long worksId;

    // 用户最新一次下载时间（前端页面展示：下载时间）
    @Column(name = "download_time", nullable = false)
    private LocalDateTime downloadTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.downloadTime = now;
        this.deleted = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}