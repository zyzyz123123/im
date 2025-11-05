package com.zyzyz.im.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.zyzyz.im.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import com.zyzyz.im.config.MinioConfig;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

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
        
        // 生成7天有效期的预签名URL
        String url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectKey)
                .method(Method.valueOf("GET"))
                .expiry(7, TimeUnit.DAYS)  // 7天有效期
                .build()
        ).toString();
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
