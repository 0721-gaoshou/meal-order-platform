package com.mealorder.common.config;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * knife4j 4.5.0 的 addOrderExtension 调用 springdoc 2.8.x 已移除的
 * SpringDocConfigProperties.getGroupConfigs()，致 /v3/api-docs 抛 NoSuchMethodError、doc.html 空页。
 * 自动配置该 Bean 带 @ConditionalOnMissingBean，故此处顶替：保留 x-openapi 扩展，仅跳过不兼容的排序扩展。
 */
@Configuration
@EnableConfigurationProperties(Knife4jProperties.class)
public class Knife4jCompatConfig {

    @Bean
    public Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(Knife4jProperties knife4jProperties,
                                                             SpringDocConfigProperties springDocConfigProperties) {
        return new Knife4jOpenApiCustomizer(knife4jProperties, springDocConfigProperties) {
            @Override
            public void customise(OpenAPI openAPI) {
                if (!knife4jProperties.isEnable()) {
                    return;
                }
                OpenApiExtensionResolver resolver =
                        new OpenApiExtensionResolver(knife4jProperties.getSetting(), knife4jProperties.getDocuments());
                resolver.start();
                Map<String, Object> extensions = new HashMap<>();
                extensions.put("x-setting", knife4jProperties.getSetting());
                extensions.put("x-markdownFiles", resolver.getMarkdownFiles());
                openAPI.addExtension("x-openapi", extensions);
            }
        };
    }
}