package com.techstore.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        String name,

        @NotBlank(message = "SKU is required")
        @Size(min = 3, max = 50, message = "SKU must contain 3 to 50 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
                message = "SKU may contain letters, digits, dots, underscores, and hyphens"
        )
        String sku,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimals")
        BigDecimal price,

        @Min(value = 0, message = "Stock quantity must not be negative")
        int stockQuantity,

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotNull(message = "Brand is required")
        Long brandId,

        boolean replaceImages
) {
}
