package com.zyzyz.im.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import com.zyzyz.im.service.FileService;
import com.zyzyz.im.common.Result;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("fileType") String fileType) throws Exception {
        try {
            String url = fileService.uploadFile(file, fileType);
            return Result.success("上传成功", url);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
    
}
