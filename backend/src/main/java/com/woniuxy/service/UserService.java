package com.woniuxy.service;

import com.woniuxy.entity.DTO.LoginDto;
import com.woniuxy.entity.DTO.RegisterDTO;
import com.woniuxy.entity.DTO.UserChangePwdDTO;
import com.woniuxy.entity.DTO.UserProfileUpdateDTO;
import com.woniuxy.utils.ResponseResult;

public interface UserService {
    ResponseResult login(LoginDto loginDto);
    ResponseResult register(RegisterDTO registerDTO);

    ResponseResult sendCode(String phone);

    ResponseResult getUserInfo(String token);

    ResponseResult updateUserInfo(String token, UserProfileUpdateDTO dto);

    ResponseResult updateUserPwd(String token, UserChangePwdDTO dto);
}
