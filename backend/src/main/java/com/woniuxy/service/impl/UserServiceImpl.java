package com.woniuxy.service.impl;

import com.woniuxy.entity.DTO.LoginDto;
import com.woniuxy.entity.DTO.RegisterDTO;
import com.woniuxy.entity.DTO.UserChangePwdDTO;
import com.woniuxy.entity.DTO.UserProfileUpdateDTO;
import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.entity.VO.UserProfileVO;
import com.woniuxy.entity.VO.UserVO;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.service.UserService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public ResponseResult login(LoginDto loginDto) {
        Optional<UserPO> byPhone = userRepository.findByPhone(loginDto.getPhone());
        if (byPhone.isEmpty()) {
            return new ResponseResult(4001, "账号或密码不正确");
        }
        String pwd = DigestUtils.md5DigestAsHex(loginDto.getPassword().getBytes(StandardCharsets.UTF_8));
        UserPO userPO = byPhone.get();
        // 打印日志对比
        if (!userPO.getPassword().equals(pwd)) {
            return new ResponseResult(4001, "账号或密码不正确");
        }
        String token = JWTUtils.generateToken(loginDto.getPhone());
        UserVO userVO = new UserVO();
        userVO.setToken(token);
        userVO.setPhone(loginDto.getPhone());
        return new ResponseResult(200, "登陆成功", userVO);
    }

    @Override
    public ResponseResult register(RegisterDTO registerDTO) {
        //判断手机号是否存在，存在则返回手机号已被注册
        if (userRepository.findByPhone(registerDTO.getPhone()).isPresent()) {
            return ResponseResult.fail("手机号已经被注册");
        }
        //判断两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPwd())) {
            return ResponseResult.fail("两次密码不一致");
        }
        // 后续逻辑：验证码校验

        // 密码加密 → 保存用户
        UserPO userPO = new UserPO();
        userPO.setPhone(registerDTO.getPhone());
        //MD5加密保存密码
        userPO.setPassword(DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes(StandardCharsets.UTF_8)));
        userPO.setUserName(registerDTO.getUserName());
        userPO.setGold(0);      //金币，必须设置！报错就是因为它
        userPO.setScore(0);     //积分
        userPO.setStatus(1);    //用户状态正常

        userRepository.save(userPO);
        return ResponseResult.success("注册成功");
    }

    @Override
    public ResponseResult sendCode(String phone) {
        return null;
    }

    @Override
    public ResponseResult getUserInfo(String token) {
        if(token == null || token.isBlank()){
            return ResponseResult.fail("未登录，请登录账号");
        }
        String phone;
        try {
            phone = JWTUtils.getUid(token);
        } catch (Exception e) {
            return new ResponseResult(401, "token无效或已过期");
        }
        //根据uid查询用户
        UserPO userPO = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserProfileVO userProfileVO = new UserProfileVO();
        userProfileVO.setUserName(userPO.getUserName());
        userProfileVO.setSubject(userPO.getSubject());//分区
        userProfileVO.setGender(userPO.getGender());
        userProfileVO.setIntroduction(userPO.getIntroduction());//个人签名
        userProfileVO.setPhone(userPO.getPhone());
        userProfileVO.setAvatarUrl(userPO.getAvatarUrl());
        userProfileVO.setEmail(userPO.getEmail());
        userProfileVO.setScore(userPO.getScore());
        userProfileVO.setGold(userPO.getGold() != null ? userPO.getGold() : 0);
        return  ResponseResult.success(userProfileVO);
    }

    @Override
    public ResponseResult updateUserInfo(String token, UserProfileUpdateDTO dto) {
        String phone;
        try {
            phone = JWTUtils.getUid(token);
        } catch (Exception e) {
            return new ResponseResult(401, "token无效或已过期");
        }
        //根据uid查询用户
        UserPO userPO = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 赋值更新字段
        userPO.setUserName(dto.getUserName());
        userPO.setEmail(dto.getEmail());
        userPO.setGender(dto.getGender());
        userPO.setIntroduction(dto.getIntroduction());
        userPO.setSubject(dto.getSubject());
        userPO.setAvatarUrl(dto.getAvatarUrl());
        userRepository.save(userPO);

        UserProfileVO vo = new UserProfileVO();
        vo.setPhone(userPO.getPhone());
        vo.setUserName(userPO.getUserName());
        vo.setEmail(userPO.getEmail());
        vo.setGender(userPO.getGender());
        vo.setIntroduction(userPO.getIntroduction());
        vo.setSubject(userPO.getSubject());
        vo.setAvatarUrl(userPO.getAvatarUrl());
        return  ResponseResult.success(vo);
    }

    @Override
    public ResponseResult updateUserPwd(String token, UserChangePwdDTO dto) {
        String phone;
        try {
            phone = JWTUtils.getUid(token);
        } catch (Exception e) {
            return new ResponseResult(401, "token无效或已过期");
        }
        //根据uid查询用户
        UserPO userPO = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        //检验新密码与确认的密码是否一致
        if (!dto.getNewPwd().equals(dto.getConfirmPwd())){
            return new ResponseResult(400,"两次密码不一致");
        }
        //判断旧密码是否正确
        String oldPwdCheck=DigestUtils.md5DigestAsHex(dto.getOldPwd().getBytes(StandardCharsets.UTF_8));
        if (!oldPwdCheck.equals(userPO.getPassword())){
            return new ResponseResult(400,"旧密码不正确");
        }
        //新密码加密后存入数据库
        String newPassword=DigestUtils.md5DigestAsHex(dto.getNewPwd().getBytes(StandardCharsets.UTF_8));
        userPO.setPassword(newPassword);
        userRepository.save(userPO);
        return ResponseResult.success("修改密码成功");
    }

}
