    package com.woniuxy.entity;

    import jakarta.persistence.*;
    import lombok.Data;
    import java.time.LocalDateTime;

    @Data
    @Entity
    @Table(name = "user")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "avatar_url")
        private String avatarUrl;

        @Column(name = "create_time")
        private LocalDateTime createTime;

        @Column(name = "deleted")
        private Boolean deleted;

        @Column(name = "email")
        private String email;

        @Column(name = "introduction")
        private String introduction;

        @Column(name = "password", nullable = false)
        private String password;

        @Column(name = "phone", nullable = false, unique = true)
        private String phone;

        @Column(name = "status")
        private Integer status;

        @Column(name = "update_time")
        private LocalDateTime updateTime;

        @Column(name = "user_name")
        private String userName;

        @Column(name = "gender")
        private String gender;

        @Column(name = "subject")
        private String subject;

        // 积分字段 数据库score
        @Column(name = "score", columnDefinition = "int default 0")
        private Integer score;

        @Column(name = "gold")
        private Integer gold;
    }