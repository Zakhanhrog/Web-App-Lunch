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
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";

    private final FoodItemService foodItemService;
    private final OrderService orderService;
    private final AppSettingService appSettingService;
    private final UserRepository userRepository;
    private final Validator validator;

    @Autowired
    public OrderController(FoodItemService foodItemService,
                           OrderService orderService,
                           AppSettingService appSettingService,
                           UserRepository userRepository,
                           Validator validator) {
        this.foodItemService = foodItemService;
        this.orderService = orderService;
        this.appSettingService = appSettingService;
        this.userRepository = userRepository;
        this.validator = validator;
    }

    private boolean isOrderTimeAllowed() {
        return LocalDateTime.now().toLocalTime().isBefore(appSettingService.getOrderCutoffTime());
    }

    @GetMapping("/menu")
    public String showMenu(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Map<Category, List<FoodItem>> groupedFoodItems = foodItemService.getGroupedAvailableFoodItemsForToday();
        model.addAttribute("groupedFoodItems", groupedFoodItems);

        if (!model.containsAttribute("orderRequestDto")) { // Check if not already added by redirect
            model.addAttribute("orderRequestDto", new OrderRequestDto());
        }


        LocalTime cutoffTime = appSettingService.getOrderCutoffTime();
        boolean orderTimeAllowed = isOrderTimeAllowed();
        model.addAttribute("orderTimeAllowed", orderTimeAllowed);
        model.addAttribute("cutoffTime", cutoffTime);

        List<Order> todaysPlacedOrders = orderService.getTodaysOrdersByPlacingUser(currentUser.getId(), LocalDate.now());
        model.addAttribute("todaysPlacedOrders", todaysPlacedOrders == null ? Collections.emptyList() : todaysPlacedOrders);


        if (orderTimeAllowed) {
            long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), cutoffTime.atDate(LocalDate.now())).getSeconds();
            model.addAttribute("countdownSeconds", Math.max(0, secondsRemaining));
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
            @RequestParam(name = "note", required = false) String noteFromForm,
            @RequestParam(name = "recipientName", required = false) String recipientNameFromParam,
            HttpSession session,
            RedirectAttributes redirectAttributes, Model model) {

        logger.info("OrderController @PostMapping(\"/place\") INVOKED.");
        logger.info("Received selectedFoodItemIds: {}", selectedFoodItemIds);
        logger.info("Received noteFromForm: {}", noteFromForm);
        logger.info("Received recipientNameFromParam: {}", recipientNameFromParam);

        UserDto currentUser = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        if (selectedFoodItemIds == null || selectedFoodItemIds.isEmpty()) {
            logger.warn("User {} did not select any items.", currentUser.getUsername());
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một món ăn.");
            return "redirect:/order/menu";
        }

        OrderRequestDto constructedOrderRequestDto = new OrderRequestDto();
        List<SelectedFoodItemDto> selectedItemsList = new ArrayList<>();
        for (Long foodId : selectedFoodItemIds) {
            SelectedFoodItemDto itemDto = new SelectedFoodItemDto();
            itemDto.setFoodItemId(foodId);
            itemDto.setQuantity(1);
            selectedItemsList.add(itemDto);
        }
        constructedOrderRequestDto.setSelectedItems(selectedItemsList);
        constructedOrderRequestDto.setNote(noteFromForm);

        if (recipientNameFromParam != null && !recipientNameFromParam.trim().isEmpty()) {
            constructedOrderRequestDto.setRecipientName(recipientNameFromParam.trim());
        }

        BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(constructedOrderRequestDto, "orderRequestDto");
        validator.validate(constructedOrderRequestDto, bindingResult);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors for user {} placing order: {}", currentUser.getUsername(), bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("orderRequestDto", constructedOrderRequestDto);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.orderRequestDto", bindingResult);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu đơn hàng không hợp lệ. Vui lòng thử lại.");
            return "redirect:/order/menu";
        }

        try {
            Order placedOrder;
            String successMessageRecipientPart;

            if (constructedOrderRequestDto.getRecipientName() != null && !constructedOrderRequestDto.getRecipientName().trim().isEmpty()) {
                logger.info("User {} is placing a proxy order for recipient: {}", currentUser.getUsername(), constructedOrderRequestDto.getRecipientName());
                placedOrder = orderService.placeOrderForOtherByRegularUser(currentUser.getId(), constructedOrderRequestDto);
                successMessageRecipientPart = "cho " + placedOrder.getRecipientName();
            } else {
                logger.info("User {} is placing an order for self.", currentUser.getUsername());
                placedOrder = orderService.placeOrderForUser(currentUser.getId(), constructedOrderRequestDto);
                successMessageRecipientPart = "cho bạn";
            }

            UserDto userAfterOrder = new UserDto(userRepository.findById(currentUser.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi đặt hàng")));
            session.setAttribute(LOGGED_IN_USER_SESSION_KEY, userAfterOrder);

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Đặt món thành công %s! Mã đơn hàng: %d. Tổng tiền: %.2f VND.",
                            successMessageRecipientPart,
                            placedOrder.getId(),
                            placedOrder.getTotalAmount()
                    ));
            return "redirect:/order/menu";
        } catch (Exception e) {
            logger.error("Error when user {} places order (self or proxy): {}", currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt món: " + e.getMessage());
            redirectAttributes.addFlashAttribute("orderRequestDto", constructedOrderRequestDto);
            return "redirect:/order/menu";
        }
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng (Mã ĐH: " + orderId + ") thành công.");
        } catch (Exception e) {
            logger.error("Lỗi khi người dùng {} hủy đơn hàng ID {}: {}", currentUser.getUsername(), orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
        return "redirect:/order/menu";
    }
}