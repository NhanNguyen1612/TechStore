package com.techstore.category.service.impl;

import com.techstore.category.dto.request.CreateCategoryRequest;
import com.techstore.category.dto.request.UpdateCategoryRequest;
import com.techstore.category.dto.response.CategoryResponse;
import com.techstore.category.dto.response.PageResponse;
import com.techstore.category.entity.Category;
import com.techstore.category.exception.CategoryErrorCode;
import com.techstore.category.exception.CategoryException;
import com.techstore.category.repository.CategoryRepository;
import com.techstore.category.service.CategoryService;
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
public class CategoryServiceImpl implements CategoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "createdAt",
            "updatedAt"
    );
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public CategoryServiceImpl(CategoryRepository categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction safeDirection = Sort.Direction.fromString(direction);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(safeDirection, safeSortBy).and(Sort.by(Sort.Direction.ASC, "id"))
        );
        Page<CategoryResponse> categories = categoryRepository
                .searchActive(normalizeSearch(search), pageable)
                .map(this::toResponse);
        return PageResponse.from(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return toResponse(findActiveCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String name = normalizeName(request.name());
        ensureUniqueName(name, null);

        Category category = new Category(
                name,
                generateUniqueSlug(name, null),
                normalizeDescription(request.description())
        );
        return toResponse(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = findActiveCategory(id);
        String name = normalizeName(request.name());
        ensureUniqueName(name, id);

        String slug = category.getName().equals(name)
                ? category.getSlug()
                : generateUniqueSlug(name, id);
        category.update(name, slug, normalizeDescription(request.description()));
        return toResponse(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findActiveCategory(id);
        category.softDelete(clock.instant());
        categoryRepository.saveAndFlush(category);
    }

    private Category findActiveCategory(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CategoryException(
                        CategoryErrorCode.CATEGORY_NOT_FOUND
                ));
    }

    private void ensureUniqueName(String name, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(name)
                : categoryRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                        name,
                        excludedId
                );
        if (exists) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_NAME_EXISTS);
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
                ? categoryRepository.existsBySlugIgnoreCaseAndDeletedFalse(slug)
                : categoryRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(
                        slug,
                        excludedId
                );
    }

    private String toSlug(String name) {
        String normalized = Normalizer.normalize(
                name.toLowerCase(Locale.ROOT).replace('đ', 'd'),
                Normalizer.Form.NFD
        );
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String slug = NON_SLUG_CHARACTERS.matcher(withoutDiacritics)
                .replaceAll("-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) {
            slug = "category";
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

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCreatedBy(),
                category.getUpdatedBy()
        );
    }
}
