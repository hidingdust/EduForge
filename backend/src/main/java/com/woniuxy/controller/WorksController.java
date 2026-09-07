package com.woniuxy.controller;

import com.woniuxy.entity.DTO.LikeDTO;
import com.woniuxy.entity.DTO.WorksDTO;
import com.woniuxy.entity.VO.WorksVO;
import com.woniuxy.entity.Works;
import com.woniuxy.repository.UserRepository;
import com.woniuxy.repository.WorksRepository;
import com.woniuxy.service.WorksService;
import com.woniuxy.utils.JWTUtils;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/works")
@RequiredArgsConstructor
public class WorksController {
    private final WorksService worksService;
    private final UserRepository userRepository;
    private final WorksRepository worksRepository;

    @PostMapping("/findByKeyword")
    public ResponseResult findByKeyword(
            @RequestBody WorksDTO worksDTO,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Long loginUserId = null;
        if (token != null) {
            String phone = JWTUtils.getPhoneFromAuthHeader(token);
            loginUserId = Long.valueOf(userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("用户不存在"))
                    .getUserId());
        }
        return ResponseResult.success(worksService.findByKeyWord(worksDTO, loginUserId));
    }

    @PostMapping("/profile/myWorks")
    public ResponseResult myWorks(
            @RequestBody WorksDTO worksDTO,
            @RequestHeader("Authorization") String token
    ) {
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        Long userId = Long.valueOf(userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"))
                .getUserId());
        Page<WorksVO> page = worksService.findMyWorks(worksDTO, userId, userId);
        return ResponseResult.success(page);
    }

    @DeleteMapping("/delete/{worksId}")
    public ResponseResult deleteWorks(
            @PathVariable Long worksId,
            @RequestHeader("Authorization") String token
    ) {
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        Long userId = Long.valueOf(userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"))
                .getUserId());
        worksService.deleteWorks(worksId, userId);
        return ResponseResult.success(null, "删除成功");
    }

    @PostMapping("/like")
    public ResponseResult likeWork(
            @RequestBody LikeDTO likeDTO,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        if (token == null) {
            throw new RuntimeException("请先登录");
        }
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        Long userId = Long.valueOf(
                userRepository.findByPhone(phone)
                        .orElseThrow(() -> new RuntimeException("用户不存在"))
                        .getUserId()
        );

        Works works = worksRepository.findByTaskId(likeDTO.getTaskId())
                .orElseThrow(() -> new RuntimeException("作品不存在"));

        worksService.likeWorks(works.getWorksId(), userId);
        return ResponseResult.success(null, "点赞成功");
    }

    @PostMapping("/cancelLike")
    public ResponseResult cancelLikeWork(
            @RequestBody LikeDTO likeDTO,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        if (token == null) {
            throw new RuntimeException("请先登录");
        }
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        Long userId = Long.valueOf(
                userRepository.findByPhone(phone)
                        .orElseThrow(() -> new RuntimeException("用户不存在"))
                        .getUserId()
        );

        Works works = worksRepository.findByTaskId(likeDTO.getTaskId())
                .orElseThrow(() -> new RuntimeException("作品不存在"));

        worksService.cancelLikeWorks(works.getWorksId(), userId);
        return ResponseResult.success(null, "取消点赞成功");
    }
    @PostMapping("/profile/myLiked")
    public ResponseResult myLikedWorks(
            @RequestBody WorksDTO worksDTO,
            @RequestHeader("Authorization") String token
    ) {
        String phone = JWTUtils.getPhoneFromAuthHeader(token);
        Long userId = Long.valueOf(userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"))
                .getUserId());
        // 参数说明：userId = 当前登录用户，查询该用户点赞的作品
        Page<WorksVO> page = worksService.findMyLikedWorks(worksDTO, userId);
        return ResponseResult.success(page);
    }
}