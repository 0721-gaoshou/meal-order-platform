package com.mealorder.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RustFS（S3 兼容对象存储）配置项（application.yml rustfs.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "rustfs")
public class RustFsProperties {

    /** S3 API 地址，如 http://localhost:9000 */
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    /** 返回给前端的图片访问地址前缀（桶 public-read，直连对象存储取图，不经过后端） */
    private String publicUrl;
}
