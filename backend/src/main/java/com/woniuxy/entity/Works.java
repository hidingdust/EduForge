package com.woniuxy.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "works")
public class Works {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long worksId;
    @Column(name = "task_id")
    private String taskId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title")
    private String title;

    @Column(name = "doc_name")
    private String docName;

    @Column(name = "doc_url")
    private String docUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "mind_map_json", columnDefinition = "LONGTEXT")
    private String mindMapJson;

    @Column(name = "ppt_outline_json", columnDefinition = "LONGTEXT")
    private String pptOutlineJson;

    @Column(name = "animation_script_json", columnDefinition = "LONGTEXT")
    private String animationScriptJson;

    @Column(name = "quiz_json", columnDefinition = "LONGTEXT")
    private String quizJson;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "view_count")
    private Integer viewCount;

    // ============ 新增字段 开始 ============
    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "score")
    private Double score;
    // ============ 新增字段 结束 ============

    @Column(name = "status")
    private Integer status;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.deleted = false;
        // 初始化所有数值字段默认值0
        if (this.likeCount == null) {
            this.likeCount = 0;
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.commentCount == null) {
            this.commentCount = 0;
        }
        if (this.downloadCount == null) {
            this.downloadCount = 0;
        }
        if (this.score == null) {
            this.score = 0.0;
        }
        if (this.isPublic == null) {
            this.isPublic = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}