package com.woniuxy.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDownloadWorkVO {
    /**
     * 课堂资源主键
     */
    private Long worksId;

    /**
     * 课堂标题
     */
    private String title;

    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 拼接完成的分类展示名称：父分类名/子分类名（前端直接渲染）
     */
    private String categoryFullName;

    /**
     * 用户最近一次下载时间
     */
    private LocalDateTime downloadTime;

    /**
     * 任务ID，用于 classroomRender 页面查询课堂渲染数据
     */
    private String taskId;
}