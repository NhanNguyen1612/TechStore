package com.techstore.product.service.impl;

import com.techstore.brand.entity.Brand;
import com.techstore.brand.repository.BrandRepository;
import com.techstore.category.entity.Category;
import com.techstore.category.repository.CategoryRepository;
import com.techstore.product.config.ProductImageProperties;
import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.product.dto.response.ProductDetailResponse;
import com.techstore.product.dto.response.ProductImageResponse;
import com.techstore.product.dto.response.ProductPageResponse;
import com.techstore.product.dto.response.ProductSummaryResponse;
import com.techstore.product.entity.Product;
import com.techstore.product.entity.ProductImage;
import com.techstore.product.exception.ProductErrorCode;
import com.techstore.product.exception.ProductException;
import com.techstore.product.model.ProductSort;
import com.techstore.product.repository.ProductRepository;
import com.techstore.product.repository.ProductSpecifications;
import com.techstore.product.service.ProductService;
import com.techstore.product.storage.ProductImageStorage;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageStorage imageStorage;
    private final ProductImageProperties imageProperties;
    private final Clock clock;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductImageStorage imageStorage,
            ProductImageProperties imageProperties,
            Clock clock
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.imageStorage = imageStorage;
        this.imageProperties = imageProperties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductSort sort,
            int page,
            int size
    ) {
        validateQuery(minPrice, maxPrice, page, size);
        ProductSort safeSort = sort == null ? ProductSort.NEWEST : sort;
        Specification<Product> specification = ProductSpecifications.availableProducts()
                .and(ProductSpecifications.matches(keyword))
                .and(ProductSpecifications.hasCategory(categoryId))
                .and(ProductSpecifications.hasBrand(brandId))
                .and(ProductSpecifications.priceAtLeast(minPrice))
                .and(ProductSpecifications.priceAtMost(maxPrice));
        Page<ProductSummaryResponse> products = productRepository
                .findAll(specification, PageRequest.of(page, size, toSort(safeSort)))
                .map(this::toSummary);
        return ProductPageResponse.from(products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(Long id) {
        return toDetail(findProduct(id));
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(
            CreateProductRequest request,
            List<MultipartFile> images
    ) {
        List<MultipartFile> safeImages = normalizeImages(images);
        validateImages(safeImages, 0, false);
        String sku = normalizeSku(request.sku());
        ensureUniqueSku(sku, null);

        Category category = findCategory(request.categoryId());
        Brand brand = findBrand(request.brandId());
        String name = normalizeName(request.name());
        Product product = new Product(
                name,
                generateUniqueSlug(name, null),
                sku,
                normalizeDescription(request.description()),
                request.price(),
                request.stockQuantity(),
                category,
                brand
        );

        List<ProductImageStorage.StoredImage> uploaded = imageStorage.upload(safeImages);
        registerImageCleanup(List.of(), publicIds(uploaded));
        addImages(product, uploaded);
        Product saved = productRepository.saveAndFlush(product);
        return toDetail(saved);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(
            Long id,
            UpdateProductRequest request,
            List<MultipartFile> images
    ) {
        Product product = findProduct(id);
        List<MultipartFile> safeImages = normalizeImages(images);
        validateImages(safeImages, product.getImages().size(), request.replaceImages());
        String sku = normalizeSku(request.sku());
        ensureUniqueSku(sku, id);

        Category category = findCategory(request.categoryId());
        Brand brand = findBrand(request.brandId());
        String name = normalizeName(request.name());
        String slug = product.getName().equals(name)
                ? product.getSlug()
                : generateUniqueSlug(name, id);
        product.update(
                name,
                slug,
                sku,
                normalizeDescription(request.description()),
                request.price(),
                request.stockQuantity(),
                category,
                brand
        );

        List<String> oldPublicIds = request.replaceImages()
                ? product.getImages().stream().map(ProductImage::getPublicId).toList()
                : List.of();
        List<ProductImageStorage.StoredImage> uploaded = imageStorage.upload(safeImages);
        registerImageCleanup(oldPublicIds, publicIds(uploaded));

        if (request.replaceImages()) {
            product.clearImages();
        }
        addImages(product, uploaded);
        product.updateThumbnail();
        Product saved = productRepository.saveAndFlush(product);
        return toDetail(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        product.softDelete(clock.instant());
        productRepository.saveAndFlush(product);
    }

    private void addImages(
            Product product,
            List<ProductImageStorage.StoredImage> uploaded
    ) {
        int startOrder = product.getImages().size();
        for (int index = 0; index < uploaded.size(); index++) {
            ProductImageStorage.StoredImage image = uploaded.get(index);
            product.addImage(
                    image.url(),
                    image.publicId(),
                    startOrder + index,
                    startOrder == 0 && index == 0
            );
        }
    }

    private void registerImageCleanup(
            List<String> oldPublicIds,
            List<String> newPublicIds
    ) {
        if (oldPublicIds.isEmpty() && newPublicIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        List<String> publicIds = status == STATUS_COMMITTED
                                ? oldPublicIds
                                : newPublicIds;
                        publicIds.forEach(imageStorage::delete);
                    }
                }
        );
    }

    private List<String> publicIds(List<ProductImageStorage.StoredImage> images) {
        return images.stream().map(ProductImageStorage.StoredImage::publicId).toList();
    }

    private void validateQuery(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ProductException(ProductErrorCode.INVALID_PRODUCT_QUERY);
        }
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new ProductException(ProductErrorCode.INVALID_PRODUCT_QUERY);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ProductException(ProductErrorCode.INVALID_PRICE_RANGE);
        }
    }

    private void validateImages(
            List<MultipartFile> images,
            int existingCount,
            boolean replaceImages
    ) {
        int totalCount = replaceImages ? images.size() : existingCount + images.size();
        if (totalCount > imageProperties.maxFiles()) {
            throw new ProductException(ProductErrorCode.TOO_MANY_IMAGES);
        }
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new ProductException(ProductErrorCode.IMAGE_REQUIRED);
            }
            if (image.getSize() > imageProperties.maxFileSize().toBytes()) {
                throw new ProductException(ProductErrorCode.IMAGE_TOO_LARGE);
            }
            String contentType = image.getContentType() == null
                    ? ""
                    : image.getContentType().toLowerCase(Locale.ROOT);
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new ProductException(ProductErrorCode.INVALID_IMAGE_TYPE);
            }
        }
    }

    private List<MultipartFile> normalizeImages(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return new ArrayList<>(images);
    }

    private Product findProduct(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));
    }

    private Brand findBrand(Long id) {
        return brandRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));
    }

    private void ensureUniqueSku(String sku, Long excludedId) {
        boolean exists = excludedId == null
                ? productRepository.existsBySkuIgnoreCaseAndDeletedFalse(sku)
                : productRepository.existsBySkuIgnoreCaseAndDeletedFalseAndIdNot(
                        sku,
                        excludedId
                );
        if (exists) {
            throw new ProductException(ProductErrorCode.SKU_ALREADY_EXISTS);
        }
    }

    private String generateUniqueSlug(String name, Long excludedId) {
        String baseSlug = toSlug(name);
        String candidate = baseSlug;
        int suffix = 2;
        while (slugExists(candidate, excludedId)) {
            String suffixText = "-" + suffix++;
            int maxBaseLength = 180 - suffixText.length();
            candidate = baseSlug.substring(0, Math.min(baseSlug.length(), maxBaseLength))
                    + suffixText;
        }
        return candidate;
    }

    private boolean slugExists(String slug, Long excludedId) {
        return excludedId == null
                ? productRepository.existsBySlugIgnoreCaseAndDeletedFalse(slug)
                : productRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(
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
            slug = "product";
        }
        return slug.substring(0, Math.min(slug.length(), 180));
    }

    private Sort toSort(ProductSort sort) {
        return switch (sort) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case BEST_SELLER -> Sort.by(Sort.Direction.DESC, "soldCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank()
                ? null
                : description.trim();
    }

    private ProductSummaryResponse toSummary(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSoldCount(),
                product.getThumbnailUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getCreatedAt()
        );
    }

    private ProductDetailResponse toDetail(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
                .map(image -> new ProductImageResponse(
                        image.getId(),
                        image.getUrl(),
                        image.getSortOrder(),
                        image.isPrimary()
                ))
                .toList();
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSoldCount(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getCreatedBy(),
                product.getUpdatedBy()
        );
    }
}
