package com.techstore.product;

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
import com.techstore.category.entity.Category;
import com.techstore.category.repository.CategoryRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.product.storage.ProductImageStorage;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class ProductControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@techstore.test";
    private static final String CUSTOMER_EMAIL = "customer@techstore.test";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private ProductImageStorage imageStorage;

    @BeforeEach
    void cleanBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfter() {
        cleanDatabase();
    }

    @Test
    void productCatalogIsPublicButWritesRequireAdministrator() throws Exception {
        Catalog catalog = createCatalog();
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        JsonNode product = createProductJson(
                adminToken,
                "Dell XPS 15",
                "DELL-XPS-15",
                "Premium laptop",
                "1899.00",
                catalog.laptop().getId(),
                catalog.dell().getId()
        );

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Dell XPS 15"));

        mockMvc.perform(get("/api/products/{id}", product.path("id").longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("DELL-XPS-15"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(
                                "MSI Raider",
                                "MSI-RAIDER",
                                "Gaming laptop",
                                "2499.00",
                                catalog.laptop().getId(),
                                catalog.msi().getId(),
                                false
                        )))
                .andExpect(status().isUnauthorized());

        String customerToken = createUserAndLogin(
                CUSTOMER_EMAIL,
                Role.ROLE_CUSTOMER
        );
        mockMvc.perform(delete("/api/products/{id}", product.path("id").longValue())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void administratorCanCreateProductWithMultipleImagesAndUpdateThem() throws Exception {
        Catalog catalog = createCatalog();
        User admin = createUser(ADMIN_EMAIL, Role.ROLE_ADMIN);
        String adminToken = login(ADMIN_EMAIL);
        when(imageStorage.upload(anyList()))
                .thenReturn(List.of(
                        new ProductImageStorage.StoredImage(
                                "https://cloudinary.test/dell-front.webp",
                                "products/dell-front"
                        ),
                        new ProductImageStorage.StoredImage(
                                "https://cloudinary.test/dell-side.webp",
                                "products/dell-side"
                        )
                ))
                .thenReturn(List.of(
                        new ProductImageStorage.StoredImage(
                                "https://cloudinary.test/dell-new.webp",
                                "products/dell-new"
                        )
                ));

        JsonNode created = createProductMultipart(
                adminToken,
                "Dell XPS 15",
                "DELL-XPS-15",
                "Premium laptop",
                "1899.00",
                catalog.laptop().getId(),
                catalog.dell().getId(),
                false,
                image("front.png"),
                image("side.png")
        );
        long productId = created.path("id").longValue();
        assertThat(created.path("images").size()).isEqualTo(2);
        assertThat(created.path("images").get(0).path("primary").asBoolean()).isTrue();
        assertThat(created.path("createdBy").longValue()).isEqualTo(admin.getId());

        JsonNode updated = updateProductMultipart(
                adminToken,
                productId,
                "Dell XPS 16",
                "DELL-XPS-16",
                "Updated laptop",
                "2099.00",
                catalog.laptop().getId(),
                catalog.dell().getId(),
                true,
                image("new.png")
        );
        assertThat(updated.path("images").size()).isEqualTo(1);
        assertThat(updated.path("images").get(0).path("url").asText())
                .isEqualTo("https://cloudinary.test/dell-new.webp");
        assertThat(updated.path("updatedBy").longValue()).isEqualTo(admin.getId());
        verify(imageStorage).delete("products/dell-front");
        verify(imageStorage).delete("products/dell-side");
    }

    @Test
    void listSearchFilterAndSortProducts() throws Exception {
        Catalog catalog = createCatalog();
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        long iphoneId = createProductJson(
                adminToken,
                "iPhone 17 Pro",
                "IPHONE-17-PRO",
                "Flagship smartphone",
                "999.00",
                catalog.phone().getId(),
                catalog.apple().getId()
        ).path("id").longValue();
        long galaxyId = createProductJson(
                adminToken,
                "Galaxy S26",
                "GALAXY-S26",
                "Android smartphone",
                "799.00",
                catalog.phone().getId(),
                catalog.samsung().getId()
        ).path("id").longValue();
        long rogId = createProductJson(
                adminToken,
                "ROG Strix",
                "ROG-STRIX",
                "Powerful gaming laptop",
                "1999.00",
                catalog.laptop().getId(),
                catalog.asus().getId()
        ).path("id").longValue();

        increaseSoldCount(galaxyId, 10);
        increaseSoldCount(rogId, 4);

        mockMvc.perform(get("/api/products")
                        .param("categoryId", catalog.phone().getId().toString())
                        .param("minPrice", "800")
                        .param("maxPrice", "1200")
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(iphoneId));

        mockMvc.perform(get("/api/products")
                        .param("brandId", catalog.samsung().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(galaxyId));

        mockMvc.perform(get("/api/products/search")
                        .param("q", "gaming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(rogId));

        mockMvc.perform(get("/api/products").param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(galaxyId))
                .andExpect(jsonPath("$.data.content[1].id").value(iphoneId))
                .andExpect(jsonPath("$.data.content[2].id").value(rogId));

        mockMvc.perform(get("/api/products").param("sort", "PRICE_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(rogId));

        mockMvc.perform(get("/api/products").param("sort", "BEST_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(galaxyId))
                .andExpect(jsonPath("$.data.content[1].id").value(rogId));

        mockMvc.perform(get("/api/products").param("sort", "NEWEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(rogId));
    }

    @Test
    void validatesProductDataFiltersAndImages() throws Exception {
        Catalog catalog = createCatalog();
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        createProductJson(
                adminToken,
                "Dell XPS",
                "DELL-XPS",
                "Laptop",
                "1499.00",
                catalog.laptop().getId(),
                catalog.dell().getId()
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(
                                "Another Dell",
                                "dell-xps",
                                "Duplicate SKU",
                                "999.00",
                                catalog.laptop().getId(),
                                catalog.dell().getId(),
                                false
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(
                                "",
                                "X",
                                "Invalid",
                                "-1",
                                catalog.laptop().getId(),
                                catalog.dell().getId(),
                                false
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "2000")
                        .param("maxPrice", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));

        mockMvc.perform(get("/api/products").param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_QUERY"));

        MockMultipartFile textImage = new MockMultipartFile(
                "images",
                "product.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "invalid".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/products")
                        .file(requestPart(productBody(
                                "Invalid Image Product",
                                "INVALID-IMAGE",
                                "Invalid image",
                                "500.00",
                                catalog.laptop().getId(),
                                catalog.dell().getId(),
                                false
                        )))
                        .file(textImage)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE_TYPE"));
    }

    @Test
    void deleteIsSoftAndProductDisappearsFromDetailListAndSearch() throws Exception {
        Catalog catalog = createCatalog();
        String adminToken = createUserAndLogin(ADMIN_EMAIL, Role.ROLE_ADMIN);
        long productId = createProductJson(
                adminToken,
                "Lenovo ThinkPad",
                "LENOVO-THINKPAD",
                "Business laptop",
                "1299.00",
                catalog.laptop().getId(),
                catalog.lenovo().getId()
        ).path("id").longValue();

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        Product deletedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(deletedProduct.isDeleted()).isTrue();
        assertThat(deletedProduct.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/products/search").param("q", "ThinkPad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private JsonNode createProductJson(
            String token,
            String name,
            String sku,
            String description,
            String price,
            Long categoryId,
            Long brandId
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(
                                name,
                                sku,
                                description,
                                price,
                                categoryId,
                                brandId,
                                false
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return responseData(result);
    }

    private JsonNode createProductMultipart(
            String token,
            String name,
            String sku,
            String description,
            String price,
            Long categoryId,
            Long brandId,
            boolean replaceImages,
            MockMultipartFile... images
    ) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/products");
        builder.file(requestPart(productBody(
                name,
                sku,
                description,
                price,
                categoryId,
                brandId,
                replaceImages
        )));
        builder.header("Authorization", bearer(token));
        for (MockMultipartFile image : images) {
            builder.file(image);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andReturn();
        return responseData(result);
    }

    private JsonNode updateProductMultipart(
            String token,
            long productId,
            String name,
            String sku,
            String description,
            String price,
            Long categoryId,
            Long brandId,
            boolean replaceImages,
            MockMultipartFile... images
    ) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart(
                "/api/products/{id}",
                productId
        );
        builder.file(requestPart(productBody(
                name,
                sku,
                description,
                price,
                categoryId,
                brandId,
                replaceImages
        )));
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        builder.header("Authorization", bearer(token));
        for (MockMultipartFile image : images) {
            builder.file(image);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result);
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

    private String productBody(
            String name,
            String sku,
            String description,
            String price,
            Long categoryId,
            Long brandId,
            boolean replaceImages
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "sku", sku,
                "description", description,
                "price", new BigDecimal(price),
                "stockQuantity", 10,
                "categoryId", categoryId,
                "brandId", brandId,
                "replaceImages", replaceImages
        ));
    }

    private void increaseSoldCount(long productId, long quantity) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.increaseSoldCount(quantity);
        productRepository.saveAndFlush(product);
    }

    private Catalog createCatalog() {
        Category laptop = categoryRepository.saveAndFlush(
                new Category("Laptop", "laptop", "Laptop computers")
        );
        Category phone = categoryRepository.saveAndFlush(
                new Category("Phone", "phone", "Smartphones")
        );
        Brand apple = brandRepository.saveAndFlush(new Brand("Apple", "apple", null));
        Brand samsung = brandRepository.saveAndFlush(
                new Brand("Samsung", "samsung", null)
        );
        Brand asus = brandRepository.saveAndFlush(new Brand("Asus", "asus", null));
        Brand dell = brandRepository.saveAndFlush(new Brand("Dell", "dell", null));
        Brand msi = brandRepository.saveAndFlush(new Brand("MSI", "msi", null));
        Brand lenovo = brandRepository.saveAndFlush(new Brand("Lenovo", "lenovo", null));
        return new Catalog(laptop, phone, apple, samsung, asus, dell, msi, lenovo);
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        createUser(email, role);
        return login(email);
    }

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                role == Role.ROLE_ADMIN ? "Product Admin" : "Product Customer",
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

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record Catalog(
            Category laptop,
            Category phone,
            Brand apple,
            Brand samsung,
            Brand asus,
            Brand dell,
            Brand msi,
            Brand lenovo
    ) {
    }
}
