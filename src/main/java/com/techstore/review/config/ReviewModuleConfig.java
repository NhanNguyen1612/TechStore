package com.techstore.review.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReviewImageProperties.class)
public class ReviewModuleConfig {
}
