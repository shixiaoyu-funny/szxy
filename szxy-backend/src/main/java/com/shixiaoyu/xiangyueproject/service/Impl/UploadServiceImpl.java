package com.shixiaoyu.xiangyueproject.service.Impl;

import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.service.UploadService;
import com.shixiaoyu.xiangyueproject.utils.UploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final UploadUtil uploadUtil;

    @Override
    public String upload(MultipartFile file) {
        log.info("文件上传：{}", file.getOriginalFilename());
        try {
            byte[] bytes = file.getBytes();
            String name = file.getOriginalFilename();
            return uploadUtil.upload(bytes, name);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }
}
