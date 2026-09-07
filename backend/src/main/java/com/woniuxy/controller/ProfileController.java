package com.woniuxy.controller;

import com.woniuxy.constant.Auth;
import com.woniuxy.entity.DTO.UserChangePwdDTO;
import com.woniuxy.entity.DTO.UserProfileUpdateDTO;
import com.woniuxy.service.UserService;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ProfileController {
    private final UserService userService;
    @GetMapping("/profile")
    public ResponseResult getUserInfo(@RequestHeader(value = Auth.AUTHORIZATION,required = false) String token){

        return userService.getUserInfo(token);
    }

    @PostMapping("/update")
    public ResponseResult UserInfoUpdate(@RequestHeader(value = Auth.AUTHORIZATION,required = false) String token,
                                         @RequestBody UserProfileUpdateDTO dto){

        return userService.updateUserInfo(token,dto);
    }

    @PostMapping("/password")
    public ResponseResult UserPwdUpdate(@RequestHeader(value = Auth.AUTHORIZATION,required = false) String token,
                                        @RequestBody UserChangePwdDTO dto){
        return userService.updateUserPwd(token,dto);
    }
}
