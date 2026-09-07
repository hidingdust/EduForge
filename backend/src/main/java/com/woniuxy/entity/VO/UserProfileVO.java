package com.woniuxy.entity.VO;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long userId;
    private String userName;
    private String phone;
    private String email;
    private String gender;
    private String introduction;
    private String subject;
    private String avatarUrl;
    private Integer score;
    private Integer gold;
}