package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.OrderRequestDto;
import com.example.lunchapp.model.dto.SelectedFoodItemDto;
import com.example.lunchapp.model.dto.UserDto;
import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.repository.UserRepository;
import com.example.lunchapp.service.AppSettingService;
import com.example.lunchapp.service.FoodItemService;
import com.example.lunchapp.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";

    private final FoodItemService foodItemService;
    private final OrderService orderService;
    private final AppSettingService appSettingService;
    private final UserRepository userRepository;

    @Autowired
    public OrderController(FoodItemService foodItemService,
                           OrderService orderService,
                           AppSettingService appSettingService,
                           UserRepository userRepository) {
        this.foodItemService = foodItemService;
        this.orderService = orderService;
        this.appSettingService = appSettingService;
        this.userRepository = userRepository;
    }

    private String getOrderStatus() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = appSettingService.getOrderStartTime();
        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();
        if (now.isBefore(startTime)) {
            return "TOO_EARLY";
        }
        if (now.isAfter(cutoffTime)) {
            return "CLOSED";
        }
        return "OPEN";
    }

    @GetMapping("/menu")
    public String showMenu(Model model, HttpSession session) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Map<Category, List<FoodItem>> groupedFoodItems = foodItemService.getGroupedAvailableFoodItemsForToday();
        model.addAttribute("groupedFoodItems", groupedFoodItems);

        if (!model.containsAttribute("orderRequestDto")) {
            model.addAttribute("orderRequestDto", new OrderRequestDto());
        }

        LocalTime startTime = appSettingService.getOrderStartTime();
        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();
        String orderStatus = getOrderStatus();

        model.addAttribute("orderStatus", orderStatus);
        model.addAttribute("startTime", startTime);
        model.addAttribute("cutoffTime", cutoffTime);

        List<Order> todaysPlacedOrders = orderService.getTodaysOrdersByPlacingUser(currentUser.getId(), LocalDate.now());
        model.addAttribute("todaysPlacedOrders", todaysPlacedOrders == null ? Collections.emptyList() : todaysPlacedOrders);

        if (orderStatus.equals("OPEN")) {
            long secondsRemaining = java.time.Duration.between(LocalTime.now(), cutoffTime).getSeconds();
            model.addAttribute("countdownSeconds", Math.max(0, secondsRemaining));
            model.addAttribute("countdownTarget", "cutoff");
        } else if (orderStatus.equals("TOO_EARLY")) {
            long secondsRemaining = java.time.Duration.between(LocalTime.now(), startTime).getSeconds();
            model.addAttribute("countdownSeconds", Math.max(0, secondsRemaining));
            model.addAttribute("countdownTarget", "start");
        } else {
            model.addAttribute("countdownSeconds", 0L);
            if (todaysPlacedOrders.isEmpty()){
                model.addAttribute("noOrderTodayMessage", "Bạn chưa đặt món nào hôm nay và đã hết giờ đặt.");
            }
        }
        return "order/menu";
    }

    @PostMapping("/place")
    public String placeOrder(
            @RequestParam(name = "selectedItemCheck", required = false) List<Long> selectedFoodItemIds,
            @RequestParam(name = "note", required = false) String note,
            @RequestParam(name = "recipientName", required = false) String recipientName,
            @RequestParam(name = "mealPrice", required = false) BigDecimal mealPrice,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (selectedFoodItemIds == null || selectedFoodItemIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một món ăn.");
            return "redirect:/order/menu";
        }

        OrderRequestDto orderRequestDto = new OrderRequestDto();
        List<SelectedFoodItemDto> selectedItemsList = new ArrayList<>();
        for (Long foodId : selectedFoodItemIds) {
            SelectedFoodItemDto itemDto = new SelectedFoodItemDto();
            itemDto.setFoodItemId(foodId);
            itemDto.setQuantity(1);
            selectedItemsList.add(itemDto);
        }
        orderRequestDto.setSelectedItems(selectedItemsList);
        orderRequestDto.setNote(note);
        orderRequestDto.setRecipientName(recipientName);
        orderRequestDto.setMealPrice(mealPrice);

        try {
            Order placedOrder = orderService.placeOrder(currentUser.getId(), orderRequestDto);
            UserDto userAfterOrder = new UserDto(userRepository.findById(currentUser.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi đặt hàng")));
            session.setAttribute(LOGGED_IN_USER_SESSION_KEY, userAfterOrder);

            String successMessageRecipientPart = (placedOrder.getRecipientName() != null && !placedOrder.getRecipientName().isEmpty())
                    ? "cho " + placedOrder.getRecipientName()
                    : "cho bạn";

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Đặt món thành công %s! Mã đơn hàng: #%d. Tổng tiền: %.0f VND.",
                            successMessageRecipientPart,
                            placedOrder.getDailyOrderNumber(),
                            placedOrder.getTotalAmount()
                    ));
        } catch (Exception e) {
            logger.error("Lỗi khi người dùng {} đặt món: {}", currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt món: " + e.getMessage());
            redirectAttributes.addFlashAttribute("orderRequestDto", orderRequestDto);
        }
        return "redirect:/order/menu";
    }

    @PostMapping("/cancel/{orderId}")
    public String cancelSpecificOrder(@PathVariable("orderId") Long orderId,
                                      HttpSession session, RedirectAttributes redirectAttributes) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            orderService.cancelOrderByIdAndRefund(orderId, currentUser.getId());
            UserDto userAfterCancel = new UserDto(userRepository.findById(currentUser.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi hủy đơn")));
            session.setAttribute(LOGGED_IN_USER_SESSION_KEY, userAfterCancel);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công.");
        } catch (Exception e) {
            logger.error("Lỗi khi người dùng {} hủy đơn hàng ID {}: {}", currentUser.getUsername(), orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
        return "redirect:/order/menu";
    }
}