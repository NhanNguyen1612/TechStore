package com.techstore.admin.service;

import com.techstore.admin.dto.request.AdminRequests;
import com.techstore.admin.dto.response.AdminResponses;
import com.techstore.auth.entity.Role;
import com.techstore.brand.dto.request.CreateBrandRequest;
import com.techstore.brand.dto.request.UpdateBrandRequest;
import com.techstore.category.dto.request.CreateCategoryRequest;
import com.techstore.category.dto.request.UpdateCategoryRequest;
import com.techstore.order.entity.OrderStatus;
import com.techstore.payment.entity.PaymentStatus;
import com.techstore.product.dto.request.CreateProductRequest;
import com.techstore.product.dto.request.UpdateProductRequest;
import com.techstore.review.entity.ReviewStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AdminManagementService {

    AdminResponses.PageData<AdminResponses.UserData> users(
            String search, Role role, Boolean active, int page, int size, String sort
    );

    AdminResponses.UserData user(Long id);

    AdminResponses.UserData createUser(AdminRequests.CreateUser request);

    AdminResponses.UserData updateUser(Long id, AdminRequests.UpdateUser request);

    AdminResponses.UserData updateUserStatus(Long id, boolean active);

    AdminResponses.UserData updateUserRole(Long id, Role role);

    void deleteUser(Long id);

    AdminResponses.PageData<AdminResponses.ProductData> products(
            String search, Long categoryId, Long brandId, Boolean active,
            int page, int size, String sort
    );

    AdminResponses.ProductData product(Long id);

    AdminResponses.ProductData createProduct(
            CreateProductRequest request, List<MultipartFile> images
    );

    AdminResponses.ProductData updateProduct(
            Long id, UpdateProductRequest request, List<MultipartFile> images
    );

    AdminResponses.ProductData updateStock(Long id, int stock);

    AdminResponses.ProductData updateProductStatus(Long id, boolean active);

    void deleteProduct(Long id);

    AdminResponses.PageData<AdminResponses.TaxonomyData> categories(
            String search, int page, int size, String sort
    );

    AdminResponses.TaxonomyData category(Long id);

    AdminResponses.TaxonomyData createCategory(CreateCategoryRequest request);

    AdminResponses.TaxonomyData updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);

    AdminResponses.PageData<AdminResponses.TaxonomyData> brands(
            String search, int page, int size, String sort
    );

    AdminResponses.TaxonomyData brand(Long id);

    AdminResponses.TaxonomyData createBrand(CreateBrandRequest request);

    AdminResponses.TaxonomyData updateBrand(Long id, UpdateBrandRequest request);

    void deleteBrand(Long id);

    AdminResponses.PageData<AdminResponses.OrderData> orders(
            OrderStatus status, String paymentMethod, Instant from, Instant to,
            int page, int size, String sort
    );

    AdminResponses.OrderData order(Long id);

    AdminResponses.OrderData transitionOrder(Long id, OrderStatus target);

    AdminResponses.PageData<AdminResponses.PaymentData> payments(
            PaymentStatus status, Instant from, Instant to, int page, int size, String sort
    );

    AdminResponses.PaymentData payment(Long id);

    AdminResponses.PaymentData paymentByOrder(Long orderId);

    AdminResponses.PaymentData updatePaymentStatus(Long id, PaymentStatus status);

    AdminResponses.PageData<AdminResponses.CouponData> coupons(
            String search, Boolean active, int page, int size, String sort
    );

    AdminResponses.CouponData coupon(Long id);

    AdminResponses.CouponData createCoupon(AdminRequests.UpsertCoupon request);

    AdminResponses.CouponData updateCoupon(Long id, AdminRequests.UpsertCoupon request);

    AdminResponses.CouponData updateCouponStatus(Long id, boolean active);

    void deleteCoupon(Long id);

    AdminResponses.PageData<AdminResponses.ReviewData> reviews(
            Integer rating, ReviewStatus status, int page, int size, String sort
    );

    AdminResponses.ReviewData review(Long id);

    AdminResponses.ReviewData moderateReview(Long id, ReviewStatus status, Long adminId);

    void deleteReview(Long id);

    AdminResponses.PageData<AdminResponses.ConversationData> conversations(
            Boolean closed, int page, int size, String sort
    );

    AdminResponses.ConversationData conversation(Long id);

    AdminResponses.PageData<AdminResponses.MessageData> messages(
            Long conversationId, int page, int size
    );

    AdminResponses.ConversationData assignStaff(
            Long conversationId, Long staffId
    );

    AdminResponses.ConversationData closeConversation(Long conversationId);

    AdminResponses.PageData<AdminResponses.NotificationData> notifications(
            int page, int size
    );

    AdminResponses.NotificationData createNotification(
            AdminRequests.CreateNotification request, Long adminId
    );

    void deleteNotification(Long id);
}
