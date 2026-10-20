package com.citygo.file.storage;

import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * S3 兼容适配器（MinIO 客户端）。
 * 已通过官方固定源码构建的本地 MinIO 真实验收：上传/读取/清理/重启与文件安全。
 * 启动及验收入口见 infra/scripts/start-minio.ps1、test-minio.ps1。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "citygo.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;

    public S3StorageService(@Value("${citygo.storage.s3.endpoint}") String endpoint,
                            @Value("${citygo.storage.s3.access-key}") String accessKey,
                            @Value("${citygo.storage.s3.secret-key}") String secretKey,
                            @Value("${citygo.storage.s3.bucket:citygo}") String bucket) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    @Override
    public void store(MultipartFile file, String storageKey) {
        requireKey(storageKey);
        try (var input=file.getInputStream()) {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storageKey)
                    .stream(input, file.getSize(), -1)
                    .contentType(contentType(storageKey))
                    .build());
        } catch (Exception e) {
            log.warn("s3 store failed: {}",e.getClass().getSimpleName());
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "文件保存暂不可用");
        }
    }

    @Override
    public Resource load(String storageKey) {
        requireKey(storageKey);
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(storageKey).build())) {
            byte[] content=stream.readNBytes((int)com.citygo.file.service.ImageTypeValidator.MAX_SIZE_BYTES+1);
            if(content.length>com.citygo.file.service.ImageTypeValidator.MAX_SIZE_BYTES)throw new BizException(ErrorCode.BAD_REQUEST,"图片超过大小限制");
            return new ByteArrayResource(content);
        } catch (BizException e) {throw e;} catch (io.minio.errors.ErrorResponseException e) {
            if(java.util.Set.of("NoSuchKey","NoSuchObject","NoSuchBucket").contains(e.errorResponse().code()))throw new BizException(ErrorCode.NOT_FOUND,"文件不存在");
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"文件读取暂不可用");
        } catch(Exception e){log.warn("s3 load failed: {}",e.getClass().getSimpleName());throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"文件读取暂不可用");}
    }

    @Override
    public void delete(String storageKey) {
        requireKey(storageKey);
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(storageKey).build());
        } catch (Exception e) {
            log.warn("s3 delete failed: {}", storageKey);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"文件删除暂不可用");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            try{client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}
            catch(io.minio.errors.ErrorResponseException e){if(!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code()))throw e;}
        }
    }
    private void requireKey(String key){if(key==null||!key.matches("^[0-9a-zA-Z/._-]+$")||key.contains("..")||key.startsWith("/"))throw new BizException(ErrorCode.BAD_REQUEST,"非法文件路径");}
    private String contentType(String key){if(key.endsWith(".png"))return "image/png";if(key.endsWith(".webp"))return "image/webp";return "image/jpeg";}
}
