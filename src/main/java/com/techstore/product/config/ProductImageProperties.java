package com.techstore.product.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.product-images")
public record ProductImageProperties(
        @NotBlank @DefaultValue("techstore/products") String folder,
        @NotNull @DefaultValue("5MB") DataSize maxFileSize,
        @Min(1) @Max(20) @DefaultValue("10") int maxFiles
) {
}
