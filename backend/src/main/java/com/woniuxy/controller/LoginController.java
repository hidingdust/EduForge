package com.woniuxy.controller;

import com.woniuxy.entity.DTO.LoginDto;
import com.woniuxy.entity.ResponseEntity;
import com.woniuxy.entity.VO.UserVO;
import com.woniuxy.service.UserService;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class LoginController {
    private final UserService userService;
    @PostMapping("/login")
    public ResponseResult login(@RequestBody LoginDto loginDto){
        return userService.login(loginDto);
    }
}
