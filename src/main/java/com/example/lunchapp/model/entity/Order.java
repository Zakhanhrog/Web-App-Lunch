package com.example.lunchapp.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Thêm import này

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by_admin_id", nullable = true)
    private User orderedByAdmin;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
        // Tính toán totalAmount nên được thực hiện cẩn thận hơn,
        // ví dụ: duyệt qua toàn bộ orderItems mỗi khi có thay đổi
        // Hoặc chỉ cộng/trừ phần thay đổi. Hiện tại đang cộng dồn có thể sai nếu gọi nhiều lần.
        // Để đơn giản, tạm thời giữ nguyên, nhưng đây là điểm cần xem xét.
        BigDecimal currentTotal = BigDecimal.ZERO;
        for(OrderItem oi : this.orderItems){
            currentTotal = currentTotal.add(oi.getPrice().multiply(new BigDecimal(oi.getQuantity())));
        }
        this.totalAmount = currentTotal;
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
        BigDecimal currentTotal = BigDecimal.ZERO;
        for(OrderItem oi : this.orderItems){
            currentTotal = currentTotal.add(oi.getPrice().multiply(new BigDecimal(oi.getQuantity())));
        }
        this.totalAmount = currentTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}