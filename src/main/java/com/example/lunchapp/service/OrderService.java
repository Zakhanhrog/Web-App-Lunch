package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.OrderRequestDto;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrderForUser(Long userId, OrderRequestDto orderRequestDto);
    Order placeOrderAsAdmin(Long adminUserId, OrderRequestDto orderRequestDto);
    Order placeOrderForOtherByRegularUser(Long placingUserId, OrderRequestDto orderRequestDto);
    List<Order> getOrderHistory(Long userId); // Lấy toàn bộ lịch sử
    List<Order> getTodaysOrdersByPlacingUser(Long placingUserId, LocalDate date); // Lấy đơn trong ngày của user thực hiện
    List<Order> getOrdersByDate(LocalDate date); // Lấy tất cả đơn theo ngày (cho admin)
    List<Order> getAllOrders();
    void deleteOrderById(Long orderId); // Dùng chung cho admin xóa hoặc khi user hủy
    void cancelOrderByIdAndRefund(Long orderId, Long currentUserId); // User hủy đơn cụ thể của mình
}