package com.woniuxy.entity.VO;

import lombok.Data;

@Data
public class WorksVO {
    // 数据库主键id（前端v-for当key用，稳定不为空）
    private Long id;
    // 作品业务id
    private String taskId;
    // 分类id
    private Long categoryId;
    // 文件地址
    private String docUrl;
    // 封面图片地址
    private String coverUrl;
    // 标题
    private String title;
    // 点赞数
    private Integer likeCount;
    // 浏览数
    private Integer viewCount;
    // 评论数
    private Integer commentCount;
    // 下载数
    private Integer downloadCount;

    private Boolean liked;
}