package com.woniuxy.entity.PO;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class UserPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password")
    private String password;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "email")
    private String email;

    // 性别
    @Column(name = "gender")
    private String gender;

    //所属分区 小学 中学。。。
    @Column(name = "subject")
    private String subject;

    @Column(name = "introduction")
    private String introduction; //个人签名

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "score")
    private Integer score;

    @Column(name = "gold")
    private Integer gold;

    // 新增自动填充
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.deleted = false;
        if(this.status == null){
            this.status = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}