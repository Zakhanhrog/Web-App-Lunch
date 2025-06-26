package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.OrderRequestDto;
import com.example.lunchapp.model.dto.SelectedFoodItemDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    private void checkCutoffTime() {
        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();
        if (LocalDateTime.now().toLocalTime().isAfter(cutoffTime)) {
            throw new RuntimeException("Đã hết giờ đặt món cho hôm nay. Vui lòng đặt trước " + cutoffTime.toString());
        }
    }

    @Override
    @Transactional
    public Order placeOrderForUser(Long userId, OrderRequestDto orderRequestDto) {
        checkCutoffTime();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        if (orderRequestDto.getNote() != null && !orderRequestDto.getNote().trim().isEmpty()) {
            order.setNote(orderRequestDto.getNote().trim());
        }

        BigDecimal totalAmount = processOrderItems(order, orderRequestDto.getSelectedItems());
        chargeUser(user, totalAmount);

        logger.info("Đặt món thành công cho người dùng {}. Tổng tiền: {}. Ghi chú: {}", userId, totalAmount, order.getNote());
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order placeOrderAsAdmin(Long adminUserId, OrderRequestDto orderRequestDto) {
        checkCutoffTime();
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

        BigDecimal totalAmount = processOrderItems(order, orderRequestDto.getSelectedItems());
        chargeUser(adminUser, totalAmount);

        logger.info("Admin {} đặt món thành công. Người nhận: {}. Tổng tiền: {}. Ghi chú: {}",
                adminUserId,
                targetUserForOrder != null ? targetUserForOrder.getUsername() : order.getRecipientName(),
                totalAmount,
                order.getNote());
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order placeOrderForOtherByRegularUser(Long placingUserId, OrderRequestDto orderRequestDto) {
        checkCutoffTime();
        User placingUser = userRepository.findById(placingUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng đặt hộ với ID: " + placingUserId));

        if (orderRequestDto.getRecipientName() == null || orderRequestDto.getRecipientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên người nhận không được để trống khi đặt hộ.");
        }

        Order order = new Order();
        order.setUser(placingUser);
        order.setRecipientName(orderRequestDto.getRecipientName().trim());
        order.setOrderedByAdmin(null);
        order.setOrderDate(LocalDateTime.now());

        if (orderRequestDto.getNote() != null && !orderRequestDto.getNote().trim().isEmpty()) {
            order.setNote(orderRequestDto.getNote().trim());
        }

        BigDecimal totalAmount = processOrderItems(order, orderRequestDto.getSelectedItems());
        chargeUser(placingUser, totalAmount);

        logger.info("Người dùng {} (ID: {}) đặt hộ thành công cho '{}'. Tổng tiền: {}. Ghi chú: {}",
                placingUser.getUsername(), placingUserId,
                order.getRecipientName(),
                totalAmount,
                order.getNote());
        return orderRepository.save(order);
    }

    private void chargeUser(User user, BigDecimal amount) {
        if (user.getBalance().compareTo(amount) < 0) {
            logger.warn("Người dùng {} (ID: {}) không đủ số dư. Cần: {}, Hiện có: {}", user.getUsername(), user.getId(), amount, user.getBalance());
            throw new RuntimeException("Không đủ số dư ("+user.getUsername()+"). Vui lòng nạp thêm tiền.");
        }
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
    }

    private BigDecimal processOrderItems(Order order, List<SelectedFoodItemDto> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Không thể đặt đơn hàng trống. Vui lòng chọn món.");
        }
        boolean hasValidItem = false;
        for (SelectedFoodItemDto selectedItemDto : selectedItems) {
            if (selectedItemDto.getQuantity() == null || selectedItemDto.getQuantity() <= 0) {
                logger.debug("Skipping item with ID {} due to zero or null quantity.", selectedItemDto.getFoodItemId());
                continue;
            }
            hasValidItem = true;
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

            foodItem.setDailyQuantity(foodItem.getDailyQuantity() - selectedItemDto.getQuantity());
            foodItemRepository.save(foodItem);
        }
        if (!hasValidItem) {
            throw new IllegalArgumentException("Đơn hàng không có món ăn nào hợp lệ được chọn (số lượng > 0).");
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

    @Override
    @Transactional
    public void deleteOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        User userToRefund = order.getOrderedByAdmin() != null ? order.getOrderedByAdmin() : order.getUser();

        if (userToRefund != null) {
            userToRefund.setBalance(userToRefund.getBalance().add(order.getTotalAmount()));
            userRepository.save(userToRefund);
            logger.info("Hoàn tiền {} cho user {} (ID: {}) khi xóa/hủy đơn hàng ID {}.", order.getTotalAmount(), userToRefund.getUsername(), userToRefund.getId(), orderId);
        } else {
            logger.warn("Không thể hoàn tiền cho đơn hàng ID {} vì không xác định được người đặt hoặc admin đặt hộ.", orderId);
        }

        for (OrderItem item : order.getOrderItems()) {
            FoodItem foodItem = item.getFoodItem();
            if (foodItem != null && foodItem.getDailyQuantity() != null) {
                Integer currentDailyQuantity = foodItem.getDailyQuantity();
                foodItem.setDailyQuantity(currentDailyQuantity + item.getQuantity());
                foodItemRepository.save(foodItem);
            }
        }
        orderRepository.delete(order);
        logger.info("Đơn hàng ID {} đã được xóa và số lượng món ăn đã hoàn lại.", orderId);
    }

    @Override
    @Transactional
    public void cancelOrderByIdAndRefund(Long orderId, Long currentUserId) {
        Order orderToCancel = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId + " để hủy."));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại."));
        if (orderToCancel.getUser() == null || !orderToCancel.getUser().getId().equals(currentUserId) || orderToCancel.getOrderedByAdmin() != null) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này.");
        }
        if (orderToCancel.getOrderDate().toLocalDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể hủy đơn hàng của ngày cũ.");
        }
        if (LocalDateTime.now().toLocalTime().isAfter(appSettingService.getOrderCutoffTime()) && orderToCancel.getOrderDate().toLocalDate().isEqual(LocalDate.now())) {
            throw new RuntimeException("Đã quá thời gian cho phép hủy đơn hàng hôm nay.");
        }

        deleteOrderById(orderId); // Gọi lại logic xóa và hoàn tiền/số lượng
        logger.info("Người dùng {} đã hủy thành công đơn hàng ID {} và đã được hoàn tiền/số lượng.", currentUser.getUsername(), orderId);
    }
}