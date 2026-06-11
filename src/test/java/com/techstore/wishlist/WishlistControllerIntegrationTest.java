package com.techstore.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WishlistControllerIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @BeforeEach
    void cleanBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfter() {
        cleanDatabase();
    }

    @Test
    void authenticatedUserCanAddListAndRemoveWishlistProduct() throws Exception {
        String token = createUserAndLogin("wishlist@test.local", Role.ROLE_CUSTOMER);
        Product product = createProduct("Gaming Laptop", "WISH-1");

        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        mockMvc.perform(post("/api/wishlist/{productId}", product.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.productName").value("Gaming Laptop"))
                .andExpect(jsonPath("$.data.wishlisted").value(true));

        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.data.items[0].wishlisted").value(true));

        mockMvc.perform(delete("/api/wishlist/{productId}", product.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.wishlisted").value(false));

        assertThat(wishlistRepository.count()).isZero();
    }

    @Test
    void duplicateProductIsRejectedAndStoredOnlyOnce() throws Exception {
        String token = createUserAndLogin("duplicate@test.local", Role.ROLE_CUSTOMER);
        Product product = createProduct("Phone", "WISH-2");

        mockMvc.perform(post("/api/wishlist/{productId}", product.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/wishlist/{productId}", product.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_WISHLISTED"));

        assertThat(wishlistRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsMissingAndDeletedProductsAndMissingWishlistItems() throws Exception {
        String token = createUserAndLogin("invalid@test.local", Role.ROLE_CUSTOMER);
        Product deletedProduct = createProduct("Deleted Product", "WISH-3");
        deletedProduct.softDelete(Instant.now());
        productRepository.saveAndFlush(deletedProduct);

        mockMvc.perform(post("/api/wishlist/{productId}", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(post(
                                "/api/wishlist/{productId}",
                                deletedProduct.getId()
                        )
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(delete("/api/wishlist/{productId}", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WISHLIST_ITEM_NOT_FOUND"));
    }

    @Test
    void wishlistsAreIsolatedPerUserAndAuthenticationIsRequired() throws Exception {
        String firstToken = createUserAndLogin(
                "first@wishlist.test",
                Role.ROLE_CUSTOMER
        );
        String secondToken = createUserAndLogin(
                "second@wishlist.test",
                Role.ROLE_STAFF
        );
        Product product = createProduct("Private Product", "WISH-4");

        mockMvc.perform(post("/api/wishlist/{productId}", product.getId())
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    private Product createProduct(String name, String sku) {
        Category category = categoryRepository.saveAndFlush(
                new Category(
                        "Wishlist Category " + sku,
                        ("wishlist-category-" + sku).toLowerCase(),
                        null
                )
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand(
                        "Wishlist Brand " + sku,
                        ("wishlist-brand-" + sku).toLowerCase(),
                        null
                )
        );
        return productRepository.saveAndFlush(new Product(
                name,
                name.toLowerCase().replace(' ', '-'),
                sku,
                null,
                new BigDecimal("100.00"),
                10,
                category,
                brand
        ));
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Wishlist User",
                null,
                role
        ));
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
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
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
