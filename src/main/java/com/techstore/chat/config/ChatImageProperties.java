package com.techstore.chat.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.chat-images")
public record ChatImageProperties(
        @NotBlank @DefaultValue("techstore/chat") String folder,
        @NotNull @DefaultValue("5MB") DataSize maxFileSize
) {
}
