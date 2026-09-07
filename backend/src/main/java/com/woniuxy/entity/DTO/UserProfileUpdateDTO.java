package com.woniuxy.entity.DTO;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    private String userName;
    private String email;
    private String gender;
    private String introduction;
    private String subject;
    private String avatarUrl;
}