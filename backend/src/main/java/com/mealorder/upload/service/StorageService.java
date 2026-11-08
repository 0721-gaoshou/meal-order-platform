package com.mealorder.upload.service;

import com.mealorder.common.config.RustFsProperties;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传到 RustFS（S3 兼容）：
 * key = goods/{yyyyMMdd}/{uuid}.{ext}，桶 public-read，前端拿 URL 直连取图
 */
@Slf4j
@Service
public class StorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final S3Client s3Client;
    private final RustFsProperties props;

    public StorageService(S3Client s3Client, RustFsProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    /**
     * 启动时确保桶存在且 public-read（幂等）。
     * 对象存储只是图片依赖，不可用时不能拖垮整个应用启动——失败只告警，
     * 上传路径会按需重试建桶（见 upload 内 NoSuchBucket 处理）。
     */
    @PostConstruct
    public void ensureBucket() {
        try {
            ensureBucketInternal();
        } catch (Exception e) {
            log.warn("RustFS 初始化暂不可用（应用照常启动，上传时将重试）: {}", e.getMessage());
        }
    }

    private void ensureBucketInternal() {
        String bucket = props.getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("RustFS 桶已创建: {}", bucket);
        }
        String policy = """
                {"Version":"2012-10-17","Statement":[{"Sid":"PublicRead","Effect":"Allow",\
                "Principal":{"AWS":["*"]},"Action":["s3:GetObject"],\
                "Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(bucket);
        try {
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucket).policy(policy).build());
        } catch (Exception e) {
            log.warn("RustFS 桶策略设置失败（图片可能无法匿名访问）: {}", e.getMessage());
        }
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片不能超过 5MB");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 jpg/png/webp 图片");
        }
        String key = "goods/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            putObject(key, file.getBytes());
        } catch (NoSuchBucketException e) {
            // 启动期建桶失败的兜底：惰性重试一次
            ensureBucketInternal();
            try {
                putObject(key, file.getBytes());
            } catch (BusinessException be) {
                throw be;
            } catch (Exception retryError) {
                log.error("图片上传 RustFS 失败（重试后）", retryError);
                throw new BusinessException(ResultCode.THIRD_PARTY_ERROR, "图片上传失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("图片上传 RustFS 失败", e);
            throw new BusinessException(ResultCode.THIRD_PARTY_ERROR, "图片上传失败，请稍后重试");
        }
        return props.getPublicUrl() + "/" + key;
    }

    private void putObject(String key, byte[] bytes) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(props.getBucket()).key(key).build(),
                RequestBody.fromBytes(bytes));
    }

    private String extOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
