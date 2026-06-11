package com.techstore.product.service;

import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.product.dto.response.ProductDetailResponse;
import com.techstore.product.dto.response.ProductPageResponse;
import com.techstore.product.model.ProductSort;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    ProductPageResponse getProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductSort sort,
            int page,
            int size
    );

    ProductDetailResponse getProduct(Long id);

    ProductDetailResponse createProduct(
            CreateProductRequest request,
            List<MultipartFile> images
    );

    ProductDetailResponse updateProduct(
            Long id,
            UpdateProductRequest request,
            List<MultipartFile> images
    );

    void deleteProduct(Long id);
}
