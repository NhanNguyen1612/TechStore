package com.techstore.product.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.product.dto.response.ProductDetailResponse;
import com.techstore.product.dto.response.ProductPageResponse;
import com.techstore.product.model.ProductSort;
import com.techstore.product.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<ProductPageResponse> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "Products retrieved",
                productService.getProducts(
                        null,
                        categoryId,
                        brandId,
                        minPrice,
                        maxPrice,
                        sort,
                        page,
                        size
                )
        );
    }

    @GetMapping("/search")
    public ApiResponse<ProductPageResponse> searchProducts(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "Products found",
                productService.getProducts(
                        q,
                        categoryId,
                        brandId,
                        minPrice,
                        maxPrice,
                        sort,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.success("Product retrieved", productService.getProduct(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProductJson(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return created(productService.createProduct(request, List.of()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProductMultipart(
            @Valid @RequestPart("request") CreateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return created(productService.createProduct(request, images));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductDetailResponse> updateProductJson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return updated(productService.updateProduct(id, request, List.of()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductDetailResponse> updateProductMultipart(
            @PathVariable Long id,
            @Valid @RequestPart("request") UpdateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return updated(productService.updateProduct(id, request, images));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success("Product deleted");
    }

    private ResponseEntity<ApiResponse<ProductDetailResponse>> created(
            ProductDetailResponse product
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", product));
    }

    private ApiResponse<ProductDetailResponse> updated(ProductDetailResponse product) {
        return ApiResponse.success("Product updated", product);
    }
}
