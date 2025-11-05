package com.zyzyz.im.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String uploadFile(MultipartFile file, String fileType) throws Exception;

}
