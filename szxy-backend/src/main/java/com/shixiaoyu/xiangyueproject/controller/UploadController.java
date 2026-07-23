package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.UploadService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Slf4j
@RequestMapping("/upload")
@RestController
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;
    @PostMapping
    public Result<String> upload(@Parameter(description = "文件",name = "file",required = true) @NotNull @RequestParam MultipartFile file) {
        return uploadService.upload(file);
    }
}
