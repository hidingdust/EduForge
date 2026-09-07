package com.woniuxy.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RenderVO {
    //任务唯一标识
    private String taskId;
    //任务状态
    private String taskStatus;
    //文件路径
    private String docUrl;
    //最终结果的json
    private String resultJson;
    //创建时间
    private LocalDateTime createTime;
    //修改时间
    private LocalDateTime updateTime;
}
