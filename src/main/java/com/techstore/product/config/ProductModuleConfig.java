package com.techstore.product.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProductImageProperties.class)
public class ProductModuleConfig {
}
