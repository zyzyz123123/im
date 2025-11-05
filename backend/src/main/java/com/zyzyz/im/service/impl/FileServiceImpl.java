package com.zyzyz.im.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.zyzyz.im.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;
import com.zyzyz.im.config.MinioConfig;
import java.util.UUID;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;
    
    @Value("${minio.publicUrl:}")
    private String minioPublicUrl;
    
    /**
     * 初始化 MinIO bucket，设置为公开访问
     */
    @PostConstruct
    public void initBucket() {
        try {
            String bucketName = minioConfig.getBucketName();
            
            // 检查 bucket 是否存在
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            // 如果不存在，创建 bucket
            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                System.out.println("创建 MinIO bucket: " + bucketName);
            }
            
            // 设置 bucket 为公开可读（允许匿名访问）
            String policy = String.format("""
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """, bucketName);
            
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build()
            );
            
            System.out.println("MinIO bucket 配置完成，已设置为公开访问: " + bucketName);
        } catch (Exception e) {
            System.err.println("初始化 MinIO bucket 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String fileType) throws Exception {
        String fileName = file.getOriginalFilename();
        
        // 🔥 使用规范的路径生成
        String objectKey = generateObjectKey(fileType, fileName);
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        // 返回通过 Nginx 代理的公开访问 URL
        // 格式：/minio/bucket-name/object-key
        String url;
        if (minioPublicUrl != null && !minioPublicUrl.isEmpty()) {
            // 如果配置了公开访问地址，使用配置的地址（生产环境可用完整域名）
            url = minioPublicUrl + "/" + minioConfig.getBucketName() + "/" + objectKey;
        } else {
            // 否则返回相对路径，通过 Nginx 代理访问
            url = "/minio/" + minioConfig.getBucketName() + "/" + objectKey;
        }
        
        return url;
    }
    
    /**
     * 根据文件类型生成规范的对象键
     * @param fileType 文件类型：avatar(头像), image(聊天图片), file(文档)
     * @param originalFilename 原始文件名
     * @return 规范的对象键，例如：avatars/2024/01/15/abc123.jpg
     */
    private String generateObjectKey(String fileType, String originalFilename) {
        // 提取文件扩展名
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 生成唯一标识
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 生成日期路径：yyyy/MM/dd
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 根据文件类型分配目录
        String prefix = switch (fileType) {
            case "avatar" -> "avatars";       // 头像
            case "image" -> "chat-images";    // 聊天图片
            case "file" -> "documents";       // 文档文件
            case "voice" -> "voice";          // 语音（预留）
            default -> "others";              // 其他
        };
        
        // 返回规范路径：prefix/yyyy/MM/dd/uuid.ext
        // 例如：avatars/2024/01/15/abc123def456.jpg
        return String.format("%s/%s/%s%s", prefix, datePath, uuid, fileExtension);
    }
    
}
