package com.woniuxy.controller;

import com.woniuxy.entity.DTO.RegisterDTO;
import com.woniuxy.service.UserService;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class RegisterController {
    @Autowired
    private UserService userService;

    @PostMapping("register")
    public ResponseResult register(@RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    @PostMapping("sendCode")
    public ResponseResult sendCode(@RequestParam String phone) {
        return userService.sendCode(phone);
    }
}
