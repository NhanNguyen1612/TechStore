package com.techstore.review.service.impl;

import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.repository.OrderRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.review.config.ReviewImageProperties;
import com.techstore.review.dto.request.CreateReviewRequest;
import com.techstore.review.dto.request.UpdateReviewRequest;
import com.techstore.review.dto.response.ProductReviewsResponse;
import com.techstore.review.dto.response.ReviewImageResponse;
import com.techstore.review.dto.response.ReviewResponse;
import com.techstore.review.entity.Review;
import com.techstore.review.entity.ReviewImage;
import com.techstore.review.entity.ReviewStatus;
import com.techstore.review.exception.ReviewErrorCode;
import com.techstore.review.exception.ReviewException;
import com.techstore.review.repository.ReviewRepository;
import com.techstore.review.service.ReviewService;
import com.techstore.review.storage.ReviewImageStorage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Set<OrderStatus> PURCHASED_STATUSES = Set.of(
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED
    );
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewImageStorage imageStorage;
    private final ReviewImageProperties imageProperties;
    private final Clock clock;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ReviewImageStorage imageStorage,
            ReviewImageProperties imageProperties,
            Clock clock
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.imageStorage = imageStorage;
        this.imageProperties = imageProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(
            Long userId,
            CreateReviewRequest request,
            List<MultipartFile> images
    ) {
        User user = findCustomer(userId);
        Product product = findProduct(request.productId());
        if (!orderRepository.hasPurchasedProduct(
                userId,
                product.getId(),
                PURCHASED_STATUSES
        )) {
            throw new ReviewException(ReviewErrorCode.PRODUCT_NOT_PURCHASED);
        }
        if (reviewRepository.existsByUserIdAndProductId(userId, product.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        List<MultipartFile> safeImages = normalizeImages(images);
        validateImages(safeImages, 0, false);
        List<ReviewImageStorage.StoredImage> uploaded = imageStorage.upload(safeImages);
        registerImageCleanup(List.of(), publicIds(uploaded));

        Review review = new Review(
                user,
                product,
                request.rating(),
                normalizeComment(request.comment())
        );
        addImages(review, uploaded);
        return toResponse(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(Long productId) {
        findProduct(productId);
        List<ReviewResponse> reviews = reviewRepository
                .findAllByProductIdAndStatusOrderByCreatedAtDesc(
                        productId,
                        ReviewStatus.APPROVED
                )
                .stream()
                .map(this::toResponse)
                .toList();
        BigDecimal average = reviews.isEmpty()
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(
                        reviews.stream().mapToInt(ReviewResponse::rating).average().orElse(0)
                ).setScale(1, RoundingMode.HALF_UP);
        return new ProductReviewsResponse(
                productId,
                reviews.size(),
                average,
                reviews
        );
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(
            Long userId,
            Long reviewId,
            UpdateReviewRequest request,
            List<MultipartFile> images
    ) {
        Review review = findReview(reviewId);
        verifyOwner(userId, review);
        List<MultipartFile> safeImages = normalizeImages(images);
        validateImages(
                safeImages,
                review.getImages().size(),
                request.replaceImages()
        );

        List<String> oldPublicIds = request.replaceImages()
                ? review.getImages().stream().map(ReviewImage::getPublicId).toList()
                : List.of();
        List<ReviewImageStorage.StoredImage> uploaded = imageStorage.upload(safeImages);
        registerImageCleanup(oldPublicIds, publicIds(uploaded));

        review.update(request.rating(), normalizeComment(request.comment()));
        if (request.replaceImages()) {
            review.clearImages();
        }
        addImages(review, uploaded);
        return toResponse(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        User user = findUser(userId);
        Review review = findReview(reviewId);
        boolean owner = review.getUser().getId().equals(userId);
        if (!owner && user.getRole() != Role.ROLE_ADMIN) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ACCESS_DENIED);
        }

        List<String> publicIds = review.getImages().stream()
                .map(ReviewImage::getPublicId)
                .toList();
        reviewRepository.delete(review);
        reviewRepository.flush();
        registerDeleteAfterCommit(publicIds);
    }

    @Override
    @Transactional
    public ReviewResponse approveReview(Long adminId, Long reviewId) {
        User admin = findUser(adminId);
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ACCESS_DENIED);
        }
        Review review = findReview(reviewId);
        review.approve(adminId, clock.instant());
        return toResponse(reviewRepository.saveAndFlush(review));
    }

    private void addImages(
            Review review,
            List<ReviewImageStorage.StoredImage> uploaded
    ) {
        int startOrder = review.getImages().size();
        for (int index = 0; index < uploaded.size(); index++) {
            ReviewImageStorage.StoredImage image = uploaded.get(index);
            review.addImage(image.url(), image.publicId(), startOrder + index);
        }
    }

    private void validateImages(
            List<MultipartFile> images,
            int existingCount,
            boolean replaceImages
    ) {
        int totalCount = replaceImages ? images.size() : existingCount + images.size();
        if (totalCount > imageProperties.maxFiles()) {
            throw new ReviewException(ReviewErrorCode.TOO_MANY_IMAGES);
        }
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new ReviewException(ReviewErrorCode.IMAGE_REQUIRED);
            }
            if (image.getSize() > imageProperties.maxFileSize().toBytes()) {
                throw new ReviewException(ReviewErrorCode.IMAGE_TOO_LARGE);
            }
            String contentType = Optional.ofNullable(image.getContentType())
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new ReviewException(ReviewErrorCode.INVALID_IMAGE_TYPE);
            }
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

    private void registerDeleteAfterCommit(List<String> publicIds) {
        if (publicIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publicIds.forEach(imageStorage::delete);
                    }
                }
        );
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ReviewException(
                        ReviewErrorCode.PRODUCT_NOT_FOUND
                ));
    }

    private User findCustomer(Long userId) {
        User user = findUser(userId);
        if (user.getRole() != Role.ROLE_CUSTOMER) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ACCESS_DENIED);
        }
        return user;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ReviewException(
                        ReviewErrorCode.USER_NOT_FOUND
                ));
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findDetailById(reviewId)
                .orElseThrow(() -> new ReviewException(
                        ReviewErrorCode.REVIEW_NOT_FOUND
                ));
    }

    private void verifyOwner(Long userId, Review review) {
        if (!review.getUser().getId().equals(userId)) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ACCESS_DENIED);
        }
    }

    private List<MultipartFile> normalizeImages(List<MultipartFile> images) {
        return images == null ? List.of() : new ArrayList<>(images);
    }

    private String normalizeComment(String comment) {
        return comment.trim().replaceAll("\\s+", " ");
    }

    private List<String> publicIds(
            List<ReviewImageStorage.StoredImage> images
    ) {
        return images.stream().map(ReviewImageStorage.StoredImage::publicId).toList();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                true,
                review.getStatus(),
                review.getImages().stream()
                        .map(image -> new ReviewImageResponse(
                                image.getId(),
                                image.getUrl(),
                                image.getSortOrder()
                        ))
                        .toList(),
                review.getApprovedAt(),
                review.getApprovedBy(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
