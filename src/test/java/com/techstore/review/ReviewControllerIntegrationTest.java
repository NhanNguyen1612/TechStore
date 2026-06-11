package com.techstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.auth.entity.Role;
import com.techstore.auth.entity.User;
import com.techstore.auth.repository.RefreshTokenRepository;
import com.techstore.brand.entity.Brand;
import com.techstore.brand.repository.BrandRepository;
import com.techstore.cart.repository.CartRepository;
import com.techstore.category.entity.Category;
import com.techstore.category.repository.CategoryRepository;
import com.techstore.order.entity.Order;
import com.techstore.order.repository.OrderRepository;
import com.techstore.payment.repository.MomoCallbackRepository;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.payment.repository.PaymentTransactionRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.review.entity.Review;
import com.techstore.review.entity.ReviewStatus;
import com.techstore.review.repository.ReviewRepository;
import com.techstore.review.storage.ReviewImageStorage;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MomoCallbackRepository callbackRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserManagementRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ReviewImageStorage imageStorage;

    @BeforeEach
    void cleanBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfter() {
        cleanDatabase();
    }

    @Test
    void onlyDeliveredCustomerCanReviewAndDuplicateIsRejected() throws Exception {
        User customer = createUser("buyer@review.test", Role.ROLE_CUSTOMER);
        String token = login(customer.getEmail());
        Product product = createProduct();

        createReviewJson(token, product.getId(), 5, "Excellent product")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_PURCHASED"));

        createDeliveredOrder(customer, product);
        createReviewJson(token, product.getId(), 6, "Invalid rating")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        createReviewJson(token, product.getId(), 5, "Excellent product")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.verifiedPurchase").value(true));

        createReviewJson(token, product.getId(), 4, "Second review")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    @Test
    void reviewIsHiddenUntilAdminApprovalThenAppearsPublicly() throws Exception {
        User customer = createUser("approved@review.test", Role.ROLE_CUSTOMER);
        String customerToken = login(customer.getEmail());
        String adminToken = createUserAndLogin(
                "admin@review.test",
                Role.ROLE_ADMIN
        );
        Product product = createProduct();
        createDeliveredOrder(customer, product);

        long reviewId = responseData(createReviewJson(
                customerToken,
                product.getId(),
                4,
                "Very good"
        ).andExpect(status().isCreated()).andReturn()).path("id").longValue();

        mockMvc.perform(get("/api/reviews/product/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.reviews").isEmpty());

        mockMvc.perform(put("/api/reviews/{id}/approve", reviewId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").isNotEmpty());

        mockMvc.perform(get("/api/reviews/product/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.reviews[0].comment").value("Very good"));

        mockMvc.perform(get("/api/reviews/product/{id}", product.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void multipartImagesCanBeCreatedAndReplacedAndUpdateNeedsReapproval()
            throws Exception {
        User customer = createUser("images@review.test", Role.ROLE_CUSTOMER);
        String customerToken = login(customer.getEmail());
        String adminToken = createUserAndLogin(
                "image-admin@review.test",
                Role.ROLE_ADMIN
        );
        Product product = createProduct();
        createDeliveredOrder(customer, product);
        when(imageStorage.upload(anyList()))
                .thenReturn(List.of(
                        new ReviewImageStorage.StoredImage(
                                "https://cloudinary.test/review-1.webp",
                                "reviews/review-1"
                        ),
                        new ReviewImageStorage.StoredImage(
                                "https://cloudinary.test/review-2.webp",
                                "reviews/review-2"
                        )
                ))
                .thenReturn(List.of(
                        new ReviewImageStorage.StoredImage(
                                "https://cloudinary.test/review-new.webp",
                                "reviews/review-new"
                        )
                ));

        long reviewId = responseData(createReviewMultipart(
                customerToken,
                product.getId(),
                5,
                "Great with photos",
                image("one.png"),
                image("two.png")
        )).path("id").longValue();

        mockMvc.perform(put("/api/reviews/{id}/approve", reviewId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        JsonNode updated = responseData(updateReviewMultipart(
                customerToken,
                reviewId,
                3,
                "Updated review",
                true,
                image("new.png")
        ));
        assertThat(updated.path("status").asText()).isEqualTo("PENDING");
        assertThat(updated.path("images").size()).isEqualTo(1);
        assertThat(updated.path("images").get(0).path("url").asText())
                .isEqualTo("https://cloudinary.test/review-new.webp");
        verify(imageStorage).delete("reviews/review-1");
        verify(imageStorage).delete("reviews/review-2");
    }

    @Test
    void ownerOrAdminCanDeleteButOtherCustomerCannotModifyReview()
            throws Exception {
        User owner = createUser("owner@review.test", Role.ROLE_CUSTOMER);
        String ownerToken = login(owner.getEmail());
        String otherToken = createUserAndLogin(
                "other@review.test",
                Role.ROLE_CUSTOMER
        );
        String adminToken = createUserAndLogin(
                "delete-admin@review.test",
                Role.ROLE_ADMIN
        );
        Product product = createProduct();
        createDeliveredOrder(owner, product);
        long reviewId = responseData(createReviewJson(
                ownerToken,
                product.getId(),
                5,
                "Owner review"
        ).andReturn()).path("id").longValue();

        mockMvc.perform(put("/api/reviews/{id}", reviewId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(4, "Unauthorized update", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));

        mockMvc.perform(delete("/api/reviews/{id}", reviewId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reviews/{id}", reviewId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        assertThat(reviewRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions createReviewJson(
            String token,
            Long productId,
            int rating,
            String comment
    ) throws Exception {
        return mockMvc.perform(post("/api/reviews")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(productId, rating, comment)));
    }

    private MvcResult createReviewMultipart(
            String token,
            Long productId,
            int rating,
            String comment,
            MockMultipartFile... images
    ) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/reviews");
        builder.file(requestPart(createBody(productId, rating, comment)));
        builder.header("Authorization", bearer(token));
        for (MockMultipartFile image : images) {
            builder.file(image);
        }
        return mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.images.length()").value(images.length))
                .andReturn();
    }

    private MvcResult updateReviewMultipart(
            String token,
            long reviewId,
            int rating,
            String comment,
            boolean replaceImages,
            MockMultipartFile... images
    ) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart(
                "/api/reviews/{id}",
                reviewId
        );
        builder.file(requestPart(updateBody(rating, comment, replaceImages)));
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        builder.header("Authorization", bearer(token));
        for (MockMultipartFile image : images) {
            builder.file(image);
        }
        return mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
    }

    private String createBody(Long productId, int rating, String comment)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "rating", rating,
                "comment", comment
        ));
    }

    private String updateBody(int rating, String comment, boolean replaceImages)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "rating", rating,
                "comment", comment,
                "replaceImages", replaceImages
        ));
    }

    private MockMultipartFile requestPart(String json) {
        return new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile(
                "images",
                filename,
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
    }

    private Product createProduct() {
        Category category = categoryRepository.saveAndFlush(
                new Category("Review Category", "review-category", null)
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand("Review Brand", "review-brand", null)
        );
        return productRepository.saveAndFlush(new Product(
                "Review Product",
                "review-product",
                "REVIEW-PRODUCT",
                null,
                new BigDecimal("1000.00"),
                20,
                category,
                brand
        ));
    }

    private void createDeliveredOrder(User user, Product product) {
        Order order = new Order(
                "ORD-REVIEW-" + user.getId() + "-" + System.nanoTime(),
                user
        );
        order.addItem(product, 1);
        order.markDelivered(Instant.now());
        orderRepository.saveAndFlush(order);
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        User user = createUser(email, role);
        return login(user.getEmail());
    }

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Review User",
                null,
                role
        ));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        reviewRepository.deleteAll();
        callbackRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        wishlistRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
