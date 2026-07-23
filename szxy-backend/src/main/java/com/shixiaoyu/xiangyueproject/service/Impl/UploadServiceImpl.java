package com.shixiaoyu.xiangyueproject.service.Impl;

import com.aliyuncs.exceptions.ClientException;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.UploadService;
import com.shixiaoyu.xiangyueproject.util.UploadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final UploadUtils uploadUtils;
    @Override
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file.getOriginalFilename());
        String src;
        try {
            byte[] bytes = file.getBytes();
            String name=file.getOriginalFilename();
            src =uploadUtils.upload(bytes, name);
        } catch (IOException | ClientException e) {
            throw new RuntimeException(e);
        }
        return Result.ok(src);
    }
}
