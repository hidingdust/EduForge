package com.woniuxy.controller;

import com.woniuxy.constant.Auth;
import com.woniuxy.entity.PO.UserPO;
import com.woniuxy.entity.VO.UserDownloadWorkVO;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.service.UserDownloadService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mydownload")
@RequiredArgsConstructor
public class UserDownloadedWorkController {
    private final UserDownloadService userDownloadService;
    private final UserRepository userRepository;

    @GetMapping("/myDownload")
    public ResponseResult getMyDownload(
            @RequestHeader(Auth.AUTHORIZATION) String token) {
        // token 中存的是手机号，需先查出对应的 user_id
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        UserPO user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        List<UserDownloadWorkVO> voList = userDownloadService.listMyDownload(user.getUserId());
        return new ResponseResult(200, "查询成功", voList);
    }

    /**
     * 记录用户下载行为
     * 前端在 WorkDetail 页面下载 PPTX / 视频时调用此接口，
     * 后端根据 taskId 定位 works，再 upsert user_downloaded_works 表
     */
    @PostMapping("/record")
    public ResponseResult recordDownload(
            @RequestHeader(Auth.AUTHORIZATION) String token,
            @RequestBody Map<String, String> body) {
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        UserPO user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String taskId = body.get("taskId");
        if (taskId == null || taskId.isBlank()) {
            return ResponseResult.fail("taskId 不能为空");
        }
        boolean ok = userDownloadService.recordDownload(taskId, user.getUserId());
        if (ok) {
            return ResponseResult.success("记录下载成功", null);
        } else {
            return ResponseResult.fail("未找到对应的课堂资源");
        }
    }
}
