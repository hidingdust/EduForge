package com.woniuxy.service.impl;

import com.woniuxy.entity.Category;
import com.woniuxy.entity.DTO.EduResourcePublishDTO;
import com.woniuxy.entity.DTO.TaskCreateDTO;
import com.woniuxy.entity.PO.RenderPO;
import com.woniuxy.entity.VO.RenderVO;
import com.woniuxy.entity.Works;
import com.woniuxy.entity.projection.RenderTaskSimpleProjection;
import com.woniuxy.repository.RenderRepository;
import com.woniuxy.repository.WorksRepository;
import com.woniuxy.service.CategoryService;
import com.woniuxy.service.DocRenderGraphService;
import com.woniuxy.service.UploadService;
import com.woniuxy.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value; // ✅新增导入
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
// @RequiredArgsConstructor 注意：@Value 不支持该注解注入，移除，改用构造/字段@Value
public class UploadServiceImpl implements UploadService {
    private final DocRenderGraphService docRenderGraphService;
    private final RenderRepository renderRepository;
    private final WorksRepository worksRepository;
    private final CategoryService categoryService;
    private final OssUtil ossUtil;

    // ✅删除硬编码：private static final String LOCAL_SAVE_BASE_PATH = "D:/edu_resource_upload/";
    @Value("${upload.resource-path}") // ✅读取yml配置
    private String localSaveBasePath;

    // OSS 目录约定
    private static final String OSS_DIR_COVER = "ai_cover";
    private static final String OSS_DIR_WORK_DOC = "ai_work_doc";

    // Works 状态
    private static final int WORKS_STATUS_RUNNING = 0;
    private static final int WORKS_STATUS_SUCCESS = 1;
    private static final int WORKS_STATUS_FAIL = 2;

    // ✅构造函数注入（lombok @RequiredArgsConstructor去掉，手动写构造）
    public UploadServiceImpl(DocRenderGraphService docRenderGraphService,
                             RenderRepository renderRepository,
                             WorksRepository worksRepository,
                             CategoryService categoryService,
                             OssUtil ossUtil) {
        this.docRenderGraphService = docRenderGraphService;
        this.renderRepository = renderRepository;
        this.worksRepository = worksRepository;
        this.categoryService = categoryService;
        this.ossUtil = ossUtil;
    }


    @Override
    @Transactional
    public String publishEduResource(Integer userId, EduResourcePublishDTO dto) {
        // 1) 入参防御
        if (dto == null) {
            throw new RuntimeException("请求参数为空");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new RuntimeException("资源标题不能为空");
        }
        if (dto.getCategoryId() == null || dto.getCategoryId().isBlank()) {
            throw new RuntimeException("请选择分区");
        }
        if (dto.getCoverImage() == null || dto.getCoverImage().isEmpty()) {
            throw new RuntimeException("请上传封面图片");
        }
        if (dto.getResourceFile() == null || dto.getResourceFile().isEmpty()) {
            throw new RuntimeException("请上传资源文件");
        }

        // 2) 解析分类字符串 "一级/二级" → 分类 id（Long，允许二级为 null）
        Long categoryId = resolveCategoryId(dto.getCategoryId());

        // 3) 封面上传 OSS
        String coverUrl;
        try {
            coverUrl = ossUtil.uploadMultipartFile(dto.getCoverImage(), OSS_DIR_COVER);
            log.info("封面上传成功，coverUrl={}, userId={}", coverUrl, userId);
        } catch (IOException e) {
            log.error("封面上传OSS失败", e);
            throw new RuntimeException("封面上传失败：" + e.getMessage());
        }

        // 4) 资源文件本地暂存
        String localPath = saveResourceFileLocally(userId, dto.getResourceFile());
        String docName = dto.getResourceFile().getOriginalFilename();

        // 5) 创建 agent_task 任务，拿到 taskId
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO();
        taskCreateDTO.setUserRequirements(buildUserRequirements(dto));
        taskCreateDTO.setDocUrl(localPath);
        RenderVO renderVO = docRenderGraphService.createTask(taskCreateDTO);
        String taskId = renderVO.getTaskId();

        // 6) 写入 works 表（用户发布的资源元数据）
        Works works = new Works();
        works.setTaskId(taskId);
        works.setUserId(userId.longValue());
        works.setCategoryId(categoryId);
        works.setTitle(dto.getTitle().trim());
        works.setDocName(docName);
        works.setDocUrl(coverUrl);
        works.setCoverUrl(coverUrl);
        // 数值字段、status、isPublic、createTime、deleted 由 Works.@PrePersist 兜底
        works.setStatus(WORKS_STATUS_RUNNING);
        works.setIsPublic(true);
        Works saved = worksRepository.save(works);
        log.info("Works 入库成功 worksId={}, taskId={}, coverUrl={}", saved.getWorksId(), taskId, coverUrl);

        return taskId;
    }

    @Override
    public RenderTaskSimpleProjection getTaskByTaskId(String taskId) {
        return renderRepository.findSimpleByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在,taskId:" + taskId));
    }

    @Override
    public RenderPO getRenderPOByTaskId(String taskId) {
        return renderRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    // ============== 私有工具方法 ==============

    private @NonNull String saveResourceFileLocally(Integer userId, MultipartFile resourceFile) {
        String originalFilename = resourceFile.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID() + suffix;
        File targetDir = new File(localSaveBasePath); // ✅使用配置读取的变量
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("本地存储目录创建失败：" + localSaveBasePath);
        }
        // 拼接完整路径，注意Linux路径分隔符兼容，File会自动处理
        String fullLocalPath = new File(targetDir, uniqueFileName).getAbsolutePath();
        File destFile = new File(fullLocalPath);
        try {
            resourceFile.transferTo(destFile);
            log.info("资源文件本地保存成功 path={}, userId={}", fullLocalPath, userId);
            return fullLocalPath;
        } catch (IOException e) {
            log.error("资源文件本地保存失败", e);
            throw new RuntimeException("资源文件保存失败：" + e.getMessage());
        }
    }

    /**
     * 解析规则：
     *  - 有 "/"：以子分类为主，优先按 (parentName, childName) 查；找不到再退回按子名称查
     *  - 无 "/"：按一级分类名查
     *  - 解析不到 → 抛业务异常（不让脏数据进库）
     */
    private Long resolveCategoryId(String categoryIdStr) {
        String s = categoryIdStr.trim();
        String parentName;
        String childName;
        int slash = s.indexOf('/');
        if (slash >= 0) {
            parentName = s.substring(0, slash).trim();
            childName = s.substring(slash + 1).trim();
        } else {
            parentName = s;
            childName = null;
        }

        Optional<Category> parentOpt = categoryService.findFirstByName(parentName);
        if (parentOpt.isEmpty()) {
            throw new RuntimeException("分类不存在：" + parentName);
        }
        Category parent = parentOpt.get();
        if (childName == null || childName.isEmpty()) {
            return parent.getId() == null ? null : parent.getId().longValue();
        }
        Optional<Category> childOpt = categoryService.findChildByParentIdAndName(parent.getId(), childName);
        if (childOpt.isPresent()) {
            return childOpt.get().getId() == null ? null : childOpt.get().getId().longValue();
        }
        // 子分类找不到，但父分类有效 → 退回父分类
        log.warn("子分类未命中 parentId={}, childName={}, 回退到父分类", parent.getId(), childName);
        return parent.getId() == null ? null : parent.getId().longValue();
    }

    /**
     * 把 description + 分类信息 + 标题拼成更结构化的需求，传给 agent_task.userRequirements
     */
    private static String buildUserRequirements(EduResourcePublishDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            sb.append("资源标题：").append(dto.getTitle().trim()).append('\n');
        }
        sb.append("分类：").append(dto.getCategoryId()).append('\n');
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            sb.append("内容介绍：").append(dto.getDescription().trim());
        }
        return sb.toString();
    }
}