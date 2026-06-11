package com.techstore.payment.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.momo")
public record MomoProperties(
        @NotBlank String createEndpoint,
        @NotBlank String partnerCode,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String redirectUrl,
        @NotBlank String ipnUrl,
        @NotBlank @DefaultValue("captureWallet") String requestType,
        @NotBlank @DefaultValue("vi") String language,
        @NotNull @DefaultValue("10s") Duration connectTimeout,
        @NotNull @DefaultValue("35s") Duration readTimeout,
        @NotNull @DefaultValue("10s") Duration qrExpiration
) {
}
