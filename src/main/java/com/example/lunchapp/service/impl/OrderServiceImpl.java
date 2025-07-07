package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.OrderRequestDto;
import com.example.lunchapp.model.dto.RevenueByDate;
import com.example.lunchapp.model.dto.SelectedFoodItemDto;
import com.example.lunchapp.model.dto.TopFoodItem;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.OrderItem;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.repository.FoodItemRepository;
import com.example.lunchapp.repository.OrderRepository;
import com.example.lunchapp.repository.UserRepository;
import com.example.lunchapp.service.AppSettingService;
import com.example.lunchapp.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final AppSettingService appSettingService;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            FoodItemRepository foodItemRepository,
                            AppSettingService appSettingService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.appSettingService = appSettingService;
    }

    private void checkOrderingWindow() {
        LocalTime startTime = appSettingService.getOrderStartTime();
        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();
        LocalTime now = LocalTime.now();

        if (now.isBefore(startTime)) {
            throw new RuntimeException("Chưa đến giờ đặt món. Hệ thống mở lúc " + startTime.toString());
        }
        if (now.isAfter(cutoffTime)) {
            throw new RuntimeException("Đã hết giờ đặt món cho hôm nay. Vui lòng đặt trước " + cutoffTime.toString());
        }
    }

    private void assignDailyOrderNumber(Order order) {
        LocalDate today = order.getOrderDate().toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Optional<Long> maxDailyNumber = orderRepository.findMaxDailyOrderNumberByDate(startOfDay, endOfDay);

        long newDailyNumber = maxDailyNumber.map(max -> max + 1).orElse(1L);

        order.setDailyOrderNumber(newDailyNumber);
    }

    @Override
    @Transactional
    public Order placeOrder(Long userId, OrderRequestDto orderRequestDto) {
        checkOrderingWindow();
        User placingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        Order order = new Order();
        order.setUser(placingUser);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderedByAdmin(null);

        if (orderRequestDto.getNote() != null && !orderRequestDto.getNote().trim().isEmpty()) {
            order.setNote(orderRequestDto.getNote().trim());
        }
        if (orderRequestDto.getRecipientName() != null && !orderRequestDto.getRecipientName().trim().isEmpty()) {
            order.setRecipientName(orderRequestDto.getRecipientName().trim());
        }

        BigDecimal amountToCharge = processOrderItemsAndCalculateTotal(order, orderRequestDto);
        chargeUser(placingUser, amountToCharge);

        assignDailyOrderNumber(order);

        logger.info("Người dùng {} (ID: {}) đặt món thành công. Người nhận: '{}'. Tổng tiền: {}.",
                placingUser.getUsername(), userId,
                order.getRecipientName() != null ? order.getRecipientName() : "Bản thân",
                amountToCharge);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order placeOrderAsAdmin(Long adminUserId, OrderRequestDto orderRequestDto) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin với ID: " + adminUserId));

        Order order = new Order();
        order.setOrderedByAdmin(adminUser);
        order.setOrderDate(LocalDateTime.now());

        if (orderRequestDto.getNote() != null && !orderRequestDto.getNote().trim().isEmpty()) {
            order.setNote(orderRequestDto.getNote().trim());
        }

        User targetUserForOrder = null;
        if (orderRequestDto.getTargetUserId() != null) {
            targetUserForOrder = userRepository.findById(orderRequestDto.getTargetUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng mục tiêu với ID: " + orderRequestDto.getTargetUserId()));
            order.setUser(targetUserForOrder);
            order.setRecipientName(null);
        } else if (orderRequestDto.getRecipientName() != null && !orderRequestDto.getRecipientName().trim().isEmpty()) {
            order.setRecipientName(orderRequestDto.getRecipientName().trim());
            order.setUser(null);
        } else {
            throw new IllegalArgumentException("Phải cung cấp tên người nhận hoặc ID người dùng mục tiêu khi Admin đặt hộ.");
        }

        BigDecimal amountToCharge = processOrderItemsAndCalculateTotal(order, orderRequestDto);
        chargeUser(adminUser, amountToCharge);

        assignDailyOrderNumber(order);

        logger.info("Admin {} đặt món thành công. Người nhận: {}. Tổng tiền: {}.",
                adminUserId,
                targetUserForOrder != null ? targetUserForOrder.getUsername() : order.getRecipientName(),
                amountToCharge);
        return orderRepository.save(order);
    }

    private void chargeUser(User user, BigDecimal amount) {
        if (user.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Không đủ số dư (" + user.getUsername() + "). Vui lòng nạp thêm tiền.");
        }
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
    }

    private BigDecimal processOrderItemsAndCalculateTotal(Order order, OrderRequestDto orderRequestDto) {
        List<SelectedFoodItemDto> selectedItems = orderRequestDto.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn món.");
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        for (SelectedFoodItemDto selectedItemDto : selectedItems) {
            FoodItem foodItem = foodItemRepository.findById(selectedItemDto.getFoodItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + selectedItemDto.getFoodItemId()));

            if (!foodItem.isAvailableToday()) {
                throw new RuntimeException("Món ăn '" + foodItem.getName() + "' không có trong thực đơn hôm nay.");
            }
            if (foodItem.getDailyQuantity() == null || foodItem.getDailyQuantity() < selectedItemDto.getQuantity()) {
                throw new RuntimeException("Không đủ số lượng cho món '" + foodItem.getName() + "'. Hiện có: " + (foodItem.getDailyQuantity() != null ? foodItem.getDailyQuantity() : 0));
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setFoodItem(foodItem);
            orderItem.setQuantity(selectedItemDto.getQuantity());
            orderItem.setPrice(foodItem.getPrice());
            order.addOrderItem(orderItem);

            calculatedTotal = calculatedTotal.add(foodItem.getPrice().multiply(BigDecimal.valueOf(selectedItemDto.getQuantity())));

            foodItem.setDailyQuantity(foodItem.getDailyQuantity() - selectedItemDto.getQuantity());
            foodItemRepository.save(foodItem);
        }

        if (orderRequestDto.getMealPrice() != null && orderRequestDto.getMealPrice().compareTo(BigDecimal.ZERO) > 0) {
            logger.info("Đây là đơn hàng 'Đặt Suất'. Ghi đè tổng tiền từ {} thành {}.", calculatedTotal, orderRequestDto.getMealPrice());
            order.setTotalAmount(orderRequestDto.getMealPrice());

            String mealNote = "(suất " + orderRequestDto.getMealPrice().toBigInteger() + ")";
            String currentNote = order.getNote() != null ? order.getNote() : "";
            order.setNote(currentNote.isEmpty() ? mealNote : currentNote + " " + mealNote);
        } else {
            logger.info("Đây là đơn hàng 'Đặt Ngay'. Tổng tiền được tính từ các món đã chọn: {}.", calculatedTotal);
            order.setTotalAmount(calculatedTotal);
        }

        return order.getTotalAmount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        return orderRepository.findAllOrdersPlacedByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getTodaysOrdersByPlacingUser(Long placingUserId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return orderRepository.findAllByUser_IdAndOrderDateBetweenAndOrderedByAdminIsNullOrderByOrderDateDesc(placingUserId, startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return orderRepository.findByOrderDateBetweenFetchingAll(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private void refundAndRestoreStock(Order order) {
        User userToRefund = order.getOrderedByAdmin() != null ? order.getOrderedByAdmin() : order.getUser();

        if (userToRefund != null) {
            logger.info("Hoàn tiền {} cho người dùng '{}' (ID: {})", order.getTotalAmount(), userToRefund.getUsername(), userToRefund.getId());
            userToRefund.setBalance(userToRefund.getBalance().add(order.getTotalAmount()));
            userRepository.save(userToRefund);
        } else {
            logger.warn("Không tìm thấy người dùng để hoàn tiền cho đơn hàng ID: {}", order.getId());
        }

        for (OrderItem item : order.getOrderItems()) {
            FoodItem foodItem = item.getFoodItem();
            if (foodItem != null && foodItem.getDailyQuantity() != null) {
                Integer currentDailyQuantity = foodItem.getDailyQuantity();
                foodItem.setDailyQuantity(currentDailyQuantity + item.getQuantity());
                foodItemRepository.save(foodItem);
                logger.info("Khôi phục số lượng món '{}' (ID: {}) thêm {}", foodItem.getName(), foodItem.getId(), item.getQuantity());
            }
        }
    }

    @Override
    @Transactional
    public void deleteOrderById(Long orderId) {
        Order order = orderRepository.findByIdFetchingAll(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        refundAndRestoreStock(order);

        logger.info("Xóa đơn hàng ID: {}", orderId);
        orderRepository.delete(order);
    }


    @Override
    @Transactional
    public void cancelOrderByIdAndRefund(Long orderId, Long currentUserId) {
        Order orderToCancel = orderRepository.findByIdFetchingAll(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId + " để hủy."));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại."));

        if (orderToCancel.getUser() == null || !orderToCancel.getUser().getId().equals(currentUserId) || orderToCancel.getOrderedByAdmin() != null) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này.");
        }
        if (orderToCancel.getOrderDate().toLocalDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể hủy đơn hàng của ngày cũ.");
        }

        LocalTime now = LocalTime.now();
        LocalTime startTime = appSettingService.getOrderStartTime();
        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();

        if ((now.isAfter(cutoffTime) || now.isBefore(startTime)) && orderToCancel.getOrderDate().toLocalDate().isEqual(LocalDate.now())) {
            throw new RuntimeException("Đã ngoài thời gian cho phép hủy đơn hàng hôm nay.");
        }

        refundAndRestoreStock(orderToCancel);

        logger.info("Người dùng '{}' hủy đơn hàng ID: {}", currentUser.getUsername(), orderId);
        orderRepository.delete(orderToCancel);
    }

    @Override
    @Transactional
    public void markOrderAsPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        order.setPaid(true);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void markOrderAsUnpaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        order.setPaid(false);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByDate> getRevenueLast7Days() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7).with(LocalTime.MIN);
        List<Object[]> results = orderRepository.findRevenueByDateInRange(startDate, endDate);
        return results.stream()
                .map(result -> new RevenueByDate(
                        ((Date) result[0]).toLocalDate(),
                        (BigDecimal) result[1]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopFoodItem> getTop5SellingFoodItemsLast30Days() {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, 5);
        List<Object[]> results = orderRepository.findTopSellingFoodItems(sinceDate, pageable);
        return results.stream()
                .map(result -> new TopFoodItem((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
    }
}