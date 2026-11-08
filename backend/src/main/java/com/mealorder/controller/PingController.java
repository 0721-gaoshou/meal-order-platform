package com.mealorder.controller;

import com.mealorder.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "健康检查")
@RestController
public class PingController {

    @Operation(summary = "服务健康检查")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of(
                "service", "meal-order-backend",
                "time", LocalDateTime.now().toString()
        ));
    }
}
