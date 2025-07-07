package com.example.lunchapp.service;

import com.example.lunchapp.model.dto.OrderRequestDto;
import com.example.lunchapp.model.dto.RevenueByDate;
import com.example.lunchapp.model.dto.TopFoodItem;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    Order placeOrder(Long userId, OrderRequestDto orderRequestDto);
    Order placeOrderAsAdmin(Long adminUserId, OrderRequestDto orderRequestDto);
    List<Order> getOrderHistory(Long userId);
    List<Order> getTodaysOrdersByPlacingUser(Long placingUserId, LocalDate date);
    List<Order> getOrdersByDate(LocalDate date);
    List<Order> getAllOrders();
    void deleteOrderById(Long orderId);
    void cancelOrderByIdAndRefund(Long orderId, Long currentUserId);
    void markOrderAsPaid(Long orderId);
    void markOrderAsUnpaid(Long orderId);
    List<RevenueByDate> getRevenueLast7Days();
    List<TopFoodItem> getTop5SellingFoodItemsLast30Days();
}