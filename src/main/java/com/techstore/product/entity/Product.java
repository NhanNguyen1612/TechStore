package com.techstore.product.entity;

import com.techstore.brand.entity.Brand;
import com.techstore.category.entity.Category;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "sold_count", nullable = false)
    private long soldCount;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC, id ASC")
    private final List<ProductImage> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    private long version;

    protected Product() {
    }

    public Product(
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            int stockQuantity,
            Category category,
            Brand brand
    ) {
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.brand = brand;
    }

    public void update(
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            int stockQuantity,
            Category category,
            Brand brand
    ) {
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.brand = brand;
    }

    public void addImage(String url, String publicId, int sortOrder, boolean primary) {
        if (primary) {
            images.forEach(image -> image.setPrimary(false));
        }
        ProductImage image = new ProductImage(
                this,
                url,
                publicId,
                sortOrder,
                primary
        );
        images.add(image);
        updateThumbnail();
    }

    public void clearImages() {
        images.clear();
        thumbnailUrl = null;
    }

    public void softDelete(Instant deletedAt) {
        this.deleted = true;
        this.active = false;
        this.deletedAt = deletedAt;
    }

    public void updateStock(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative");
        }
        this.stockQuantity = stockQuantity;
    }

    public void changeStatus(boolean active) {
        if (deleted && active) {
            throw new IllegalStateException("Deleted product cannot be activated");
        }
        this.active = active;
    }

    public void increaseSoldCount(long quantity) {
        if (quantity > 0) {
            this.soldCount += quantity;
        }
    }

    public void decreaseStock(int quantity) {
        if (quantity < 1 || quantity > stockQuantity) {
            throw new IllegalArgumentException("Invalid stock quantity");
        }
        stockQuantity -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Invalid stock quantity");
        }
        stockQuantity = Math.addExact(stockQuantity, quantity);
    }

    public void updateThumbnail() {
        thumbnailUrl = images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public long getSoldCount() {
        return soldCount;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Category getCategory() {
        return category;
    }

    public Brand getBrand() {
        return brand;
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
