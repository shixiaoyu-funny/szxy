package com.shixiaoyu.xiangyueproject.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务（错误时抛 BusinessException）
 */
public interface UploadService {
    String upload(MultipartFile file);
}
