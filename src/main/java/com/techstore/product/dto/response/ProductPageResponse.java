package com.techstore.product.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record ProductPageResponse(
        List<ProductSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static ProductPageResponse from(Page<ProductSummaryResponse> page) {
        return new ProductPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
