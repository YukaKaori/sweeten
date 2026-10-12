package com.itheima.controller;

import com.itheima.annotation.RequirePermission;
import com.itheima.exception.BusinessException;
import com.itheima.pojo.Result;
import com.itheima.utils.AliyunOSSOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "07-文件上传", description = "文件上传至阿里云OSS接口")
public class UploadController {

    static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final AliyunOSSOperator aliyunOSSOperator;

    /** 识别出的图片类型：扩展名 + MIME */
    record ImageType(String extension, String contentType) {
    }

    @Operation(summary = "上传图片", description = "上传甜品图/员工头像至阿里云OSS，支持jpg/png/gif/webp，最大5MB。按文件内容识别真实类型，需要甜品或员工的新增/修改权限")
    @RequirePermission({"dessert:dessert:create", "dessert:dessert:update", "emp:emp:create", "emp:emp:update"})
    @PostMapping("/upload")
    public Result upload(@Parameter(description = "上传的图片文件") MultipartFile file) throws IOException {
        // 1. 非空校验
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 2. 大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过5MB");
        }

        // 3. 类型校验：以文件头魔数为准（Content-Type 与文件名都由客户端决定，可被伪造）
        byte[] content = file.getBytes();
        ImageType type = detectImageType(content);
        if (type == null) {
            return Result.error("不支持的文件类型，仅允许上传图片（jpg/png/gif/webp）");
        }

        String url;
        try {
            url = aliyunOSSOperator.upload(content, type.extension(), type.contentType());
        } catch (Exception e) {
            // OSS 不可用/凭证缺失等：记录原因，对用户只给可操作的提示，不透出内部信息
            log.error("图片上传 OSS 失败: {}", e.getMessage());
            throw new BusinessException(503, 503, "图片服务暂不可用，请稍后重试");
        }
        log.info("文件上传OSS成功, size={}, type={}, url={}", content.length, type.contentType(), url);
        return Result.success(url);
    }

    /**
     * 按文件头魔数识别图片类型；不是 jpg/png/gif/webp 时返回 null
     */
    static ImageType detectImageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new ImageType(".jpg", "image/jpeg");
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return new ImageType(".png", "image/png");
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8' && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return new ImageType(".gif", "image/gif");
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return new ImageType(".webp", "image/webp");
        }
        return null;
    }
}
