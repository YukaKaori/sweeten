package com.itheima.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 操作工具 — OSS 客户端单例复用，避免每次上传新建连接
 * <p>
 * 访问凭证只从环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET 读取（EnvironmentVariableCredentialsProvider），
 * 不进配置文件、不进代码。
 */
@Slf4j
@Component
public class AliyunOSSOperator {

    /**
     * 对象名是随机 UUID、内容永不覆盖，可让浏览器/CDN 长期缓存（1 年 + immutable），
     * x-oss-process 缩放后的衍生图同样受益于浏览器缓存
     */
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    @Value("${aliyun.oss.region}")
    private String region;

    private OSS ossClient;

    /**
     * 获取或创建 OSS 客户端（懒加载 + 单例）
     */
    private synchronized OSS getOssClient() throws Exception {
        if (ossClient == null) {
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            ClientBuilderConfiguration config = new ClientBuilderConfiguration();
            config.setSignatureVersion(SignVersion.V4);
            ossClient = OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(config)
                    .region(region)
                    .build();
            log.info("OSS 客户端已初始化");
        }
        return ossClient;
    }

    /**
     * 上传文件
     *
     * @param content     文件内容
     * @param extension   扩展名（含点，如 ".png"），由调用方根据文件真实类型决定，不信任客户端文件名
     * @param contentType MIME 类型，写入对象元数据
     * @return 对象的公网访问 URL
     */
    public String upload(byte[] content, String extension, String contentType) throws Exception {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = dir + "/" + UUID.randomUUID() + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(content.length);
        metadata.setCacheControl(CACHE_CONTROL);

        getOssClient().putObject(bucketName, objectName, new ByteArrayInputStream(content), metadata);

        // 构建访问 URL
        String host = bucketName + "." + endpoint.replace("https://", "").replace("http://", "");
        return "https://" + host + "/" + objectName;
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }
}
