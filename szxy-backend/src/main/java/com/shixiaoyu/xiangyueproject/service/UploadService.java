package com.shixiaoyu.xiangyueproject.service;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    Result<String> upload(MultipartFile file);
}
