package com.techstore.brand.service;

import com.techstore.brand.dto.request.CreateBrandRequest;
import com.techstore.brand.dto.request.UpdateBrandRequest;
import com.techstore.brand.dto.response.BrandPageResponse;
import com.techstore.brand.dto.response.BrandResponse;

public interface BrandService {

    BrandPageResponse getBrands(
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    );

    BrandResponse getBrand(Long id);

    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(Long id, UpdateBrandRequest request);

    void deleteBrand(Long id);
}
