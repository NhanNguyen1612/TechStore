package com.techstore.category.service;

import com.techstore.category.dto.request.CreateCategoryRequest;
import com.techstore.category.dto.request.UpdateCategoryRequest;
import com.techstore.category.dto.response.CategoryResponse;
import com.techstore.category.dto.response.PageResponse;

public interface CategoryService {

    PageResponse<CategoryResponse> getCategories(
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    );

    CategoryResponse getCategory(Long id);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}
