package com.techstore.payment;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import com.techstore.order.repository.OrderRepository;
import com.techstore.payment.entity.Payment;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.payment.entity.PaymentTransaction;
import com.techstore.payment.entity.PaymentTransactionType;
import com.techstore.payment.repository.MomoCallbackRepository;
import com.techstore.payment.repository.PaymentRepository;
import com.techstore.payment.repository.PaymentTransactionRepository;
import com.techstore.product.entity.Product;
import com.techstore.product.repository.ProductRepository;
import com.techstore.user.repository.UserManagementRepository;
import com.techstore.user.repository.UserProfileRepository;
import com.techstore.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
class PaymentControllerIntegrationTest {

    private static final String PASSWORD = "Password123";
    private static final String PARTNER_CODE = "TEST_PARTNER";
    private static final String ACCESS_KEY = "TEST_ACCESS_KEY";
    private static final String SECRET_KEY = "TEST_SECRET_KEY";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private MomoCallbackRepository callbackRepository;

    @Autowired
    private OrderRepository orderRepository;

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
    void validIpnMarksPaymentAndOrderPaidAndSavesHistory() throws Exception {
        PaymentFixture fixture = createPaymentFixture("paid@payment.test");
        Map<String, Object> callback = resultPayload(fixture, 0, "Successful.");

        mockMvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isNoContent());

        Payment payment = paymentRepository.findById(fixture.payment().getId())
                .orElseThrow();
        Order order = orderRepository.findById(fixture.order().getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getMomoTransactionId()).isEqualTo(987654321L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(callbackRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isNoContent());

        assertThat(callbackRepository.count()).isEqualTo(2);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow()
                .getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void invalidSignatureIsRejectedButCallbackHistoryIsSaved() throws Exception {
        PaymentFixture fixture = createPaymentFixture("invalid@payment.test");
        Map<String, Object> callback = resultPayload(fixture, 0, "Successful.");
        callback.put("signature", "invalid-signature");

        mockMvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isBadRequest());

        assertThat(callbackRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isZero();
        assertThat(paymentRepository.findById(fixture.payment().getId())
                .orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderRepository.findById(fixture.order().getId())
                .orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void validSignatureWithMismatchedAmountCannotUpdatePayment() throws Exception {
        PaymentFixture fixture = createPaymentFixture("mismatch@payment.test");
        Map<String, Object> callback = resultPayload(fixture, 0, "Successful.");
        callback.put("amount", fixture.payment().getAmount() + 1);
        callback.put("signature", resultSignature(callback));

        mockMvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isBadRequest());

        assertThat(callbackRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isZero();
        assertThat(paymentRepository.findById(fixture.payment().getId())
                .orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void returnIsVerifiedWithoutChangingStateAndStatusRespectsOwnership()
            throws Exception {
        PaymentFixture fixture = createPaymentFixture("owner@payment.test");
        String ownerToken = login("owner@payment.test");
        String otherToken = createUserAndLogin(
                "other@payment.test",
                Role.ROLE_CUSTOMER
        );
        String adminToken = createUserAndLogin(
                "admin@payment.test",
                Role.ROLE_ADMIN
        );
        Map<String, Object> callback = resultPayload(fixture, 0, "Successful.");

        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                get("/api/payments/momo/return");
        callback.forEach((key, value) -> request.param(key, value.toString()));
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signatureValid").value(true))
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        assertThat(callbackRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findById(fixture.order().getId())
                .orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        mockMvc.perform(get(
                                "/api/payments/{orderId}/status",
                                fixture.order().getId()
                        )
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get(
                                "/api/payments/{orderId}/status",
                                fixture.order().getId()
                        )
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYMENT_ACCESS_DENIED"));

        mockMvc.perform(get(
                                "/api/payments/{orderId}/status",
                                fixture.order().getId()
                        )
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                "/api/payments/{orderId}/status",
                fixture.order().getId()
        )).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredQrMarksPaymentFailedAndReturnsOrderToPending() throws Exception {
        PaymentFixture fixture = createPaymentFixture("expired@payment.test");
        String ownerToken = login("expired@payment.test");
        Payment payment = fixture.payment();
        payment.beginAttempt(
                payment.getRequestId(),
                payment.getMomoOrderId(),
                Instant.now().minusSeconds(1)
        );
        payment.acceptCreateResponse(
                "https://test-payment.momo.vn/pay",
                "momo://pay",
                "qr-data",
                0,
                "Successful."
        );
        paymentRepository.saveAndFlush(payment);

        mockMvc.perform(get(
                                "/api/payments/{orderId}/status",
                                fixture.order().getId()
                        )
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.message").value("MoMo QR expired"))
                .andExpect(jsonPath("$.data.payUrl").isEmpty())
                .andExpect(jsonPath("$.data.qrCodeUrl").isEmpty());

        assertThat(orderRepository.findById(fixture.order().getId())
                .orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void successfulIpnFromPreviousQrAttemptStillMarksOrderPaid()
            throws Exception {
        PaymentFixture fixture = createPaymentFixture("late-ipn@payment.test");
        Payment payment = fixture.payment();
        String oldRequestId = payment.getRequestId();
        String oldMomoOrderId = payment.getMomoOrderId();
        transactionRepository.saveAndFlush(new PaymentTransaction(
                payment,
                PaymentTransactionType.CREATE,
                oldRequestId,
                oldMomoOrderId,
                null,
                payment.getAmount(),
                0,
                "Successful.",
                "{}",
                "{}"
        ));
        payment.beginAttempt(
                "REQ-NEW-ATTEMPT",
                "MOMO-NEW-ATTEMPT",
                Instant.now().plusSeconds(10)
        );
        paymentRepository.saveAndFlush(payment);

        Map<String, Object> callback = resultPayload(
                oldMomoOrderId,
                oldRequestId,
                payment.getAmount(),
                0,
                "Successful."
        );
        mockMvc.perform(post("/api/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isNoContent());

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow()
                .getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(orderRepository.findById(fixture.order().getId()).orElseThrow()
                .getStatus()).isEqualTo(OrderStatus.PAID);
    }

    private PaymentFixture createPaymentFixture(String email) {
        User user = createUser(email, Role.ROLE_CUSTOMER);
        Category category = categoryRepository.saveAndFlush(
                new Category("Payment Category", "payment-category", null)
        );
        Brand brand = brandRepository.saveAndFlush(
                new Brand("Payment Brand", "payment-brand", null)
        );
        Product product = productRepository.saveAndFlush(new Product(
                "Payment Product",
                "payment-product",
                "PAYMENT-PRODUCT",
                null,
                new BigDecimal("100000"),
                10,
                category,
                brand
        ));
        Order order = new Order("ORD-PAYMENT-" + user.getId(), user);
        order.addItem(product, 1);
        order.markPendingPayment();
        order = orderRepository.saveAndFlush(order);

        Payment payment = new Payment(order, 100000L);
        payment.beginAttempt(
                "REQ-PAYMENT-" + user.getId(),
                "MOMO-PAYMENT-" + user.getId()
        );
        payment = paymentRepository.saveAndFlush(payment);
        return new PaymentFixture(order, payment);
    }

    private Map<String, Object> resultPayload(
            PaymentFixture fixture,
            int resultCode,
            String message
    ) throws Exception {
        return resultPayload(
                fixture.payment().getMomoOrderId(),
                fixture.payment().getRequestId(),
                fixture.payment().getAmount(),
                resultCode,
                message
        );
    }

    private Map<String, Object> resultPayload(
            String momoOrderId,
            String requestId,
            long amount,
            int resultCode,
            String message
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", PARTNER_CODE);
        payload.put("orderId", momoOrderId);
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderInfo", "Thanh toan don hang");
        payload.put("orderType", "momo_wallet");
        payload.put("transId", 987654321L);
        payload.put("resultCode", resultCode);
        payload.put("message", message);
        payload.put("payType", "qr");
        payload.put("responseTime", 1780830000000L);
        payload.put("extraData", "");
        payload.put("signature", resultSignature(payload));
        return payload;
    }

    private String resultSignature(Map<String, Object> payload) throws Exception {
        String raw = "accessKey=" + ACCESS_KEY
                + "&amount=" + value(payload.get("amount"))
                + "&extraData=" + value(payload.get("extraData"))
                + "&message=" + value(payload.get("message"))
                + "&orderId=" + value(payload.get("orderId"))
                + "&orderInfo=" + value(payload.get("orderInfo"))
                + "&orderType=" + value(payload.get("orderType"))
                + "&partnerCode=" + value(payload.get("partnerCode"))
                + "&payType=" + value(payload.get("payType"))
                + "&requestId=" + value(payload.get("requestId"))
                + "&responseTime=" + value(payload.get("responseTime"))
                + "&resultCode=" + value(payload.get("resultCode"))
                + "&transId=" + value(payload.get("transId"));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return HexFormat.of().formatHex(
                mac.doFinal(raw.getBytes(StandardCharsets.UTF_8))
        );
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String createUserAndLogin(String email, Role role) throws Exception {
        createUser(email, role);
        return login(email);
    }

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(PASSWORD),
                "Payment User",
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
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
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

    private record PaymentFixture(Order order, Payment payment) {
    }
}
