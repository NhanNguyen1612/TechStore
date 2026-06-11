package com.techstore.brand.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record BrandPageResponse(
        List<BrandResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static BrandPageResponse from(Page<BrandResponse> page) {
        return new BrandPageResponse(
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
