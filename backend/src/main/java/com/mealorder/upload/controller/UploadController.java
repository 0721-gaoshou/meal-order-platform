package com.mealorder.upload.controller;

import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.upload.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传（multipart，字段名 file），返回可直接访问的图片 URL
 */
@Tag(name = "图片上传")
@RestController
@RequestMapping("/admin/upload")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "上传图片（jpg/png/webp，≤5MB）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PostMapping("/image")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(Map.of("url", storageService.upload(file)));
    }
}
