package com.techstore.brand.service.impl;

import com.techstore.brand.dto.request.CreateBrandRequest;
import com.techstore.brand.dto.request.UpdateBrandRequest;
import com.techstore.brand.dto.response.BrandPageResponse;
import com.techstore.brand.dto.response.BrandResponse;
import com.techstore.brand.entity.Brand;
import com.techstore.brand.exception.BrandErrorCode;
import com.techstore.brand.exception.BrandException;
import com.techstore.brand.repository.BrandRepository;
import com.techstore.brand.service.BrandService;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandServiceImpl implements BrandService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "createdAt",
            "updatedAt"
    );
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

    private final BrandRepository brandRepository;
    private final Clock clock;

    public BrandServiceImpl(BrandRepository brandRepository, Clock clock) {
        this.brandRepository = brandRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandPageResponse getBrands(
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        validateQuery(page, size, sortBy, direction);
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy).and(Sort.by(Sort.Direction.ASC, "id"))
        );
        Page<BrandResponse> brands = brandRepository
                .searchActive(normalizeSearch(search), pageable)
                .map(this::toResponse);
        return BrandPageResponse.from(brands);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrand(Long id) {
        return toResponse(findActiveBrand(id));
    }

    @Override
    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        String name = normalizeName(request.name());
        ensureUniqueName(name, null);

        Brand brand = new Brand(
                name,
                generateUniqueSlug(name, null),
                normalizeDescription(request.description())
        );
        return toResponse(brandRepository.saveAndFlush(brand));
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, UpdateBrandRequest request) {
        Brand brand = findActiveBrand(id);
        String name = normalizeName(request.name());
        ensureUniqueName(name, id);

        String slug = brand.getName().equals(name)
                ? brand.getSlug()
                : generateUniqueSlug(name, id);
        brand.update(name, slug, normalizeDescription(request.description()));
        return toResponse(brandRepository.saveAndFlush(brand));
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findActiveBrand(id);
        brand.softDelete(clock.instant());
        brandRepository.saveAndFlush(brand);
    }

    private void validateQuery(int page, int size, String sortBy, String direction) {
        boolean invalid = page < 0
                || size < 1
                || size > 100
                || !ALLOWED_SORT_FIELDS.contains(sortBy)
                || (!"ASC".equalsIgnoreCase(direction)
                    && !"DESC".equalsIgnoreCase(direction));
        if (invalid) {
            throw new BrandException(BrandErrorCode.INVALID_BRAND_QUERY);
        }
    }

    private Brand findActiveBrand(Long id) {
        return brandRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BrandException(BrandErrorCode.BRAND_NOT_FOUND));
    }

    private void ensureUniqueName(String name, Long excludedId) {
        boolean exists = excludedId == null
                ? brandRepository.existsByNameIgnoreCaseAndDeletedFalse(name)
                : brandRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                        name,
                        excludedId
                );
        if (exists) {
            throw new BrandException(BrandErrorCode.BRAND_NAME_EXISTS);
        }
    }

    private String generateUniqueSlug(String name, Long excludedId) {
        String baseSlug = toSlug(name);
        String candidate = baseSlug;
        int suffix = 2;
        while (slugExists(candidate, excludedId)) {
            String suffixText = "-" + suffix++;
            int maxBaseLength = 120 - suffixText.length();
            candidate = baseSlug.substring(0, Math.min(baseSlug.length(), maxBaseLength))
                    + suffixText;
        }
        return candidate;
    }

    private boolean slugExists(String slug, Long excludedId) {
        return excludedId == null
                ? brandRepository.existsBySlugIgnoreCaseAndDeletedFalse(slug)
                : brandRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(
                        slug,
                        excludedId
                );
    }

    private String toSlug(String name) {
        String normalized = Normalizer.normalize(
                name.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String slug = NON_SLUG_CHARACTERS.matcher(withoutDiacritics)
                .replaceAll("-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) {
            slug = "brand";
        }
        return slug.substring(0, Math.min(slug.length(), 120));
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getDescription(),
                brand.getCreatedAt(),
                brand.getUpdatedAt(),
                brand.getCreatedBy(),
                brand.getUpdatedBy()
        );
    }
}
