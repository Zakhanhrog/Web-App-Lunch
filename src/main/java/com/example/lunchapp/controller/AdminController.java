package com.example.lunchapp.controller;

import com.example.lunchapp.model.dto.*;
import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.OrderItem;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.repository.RoleRepository;
import com.example.lunchapp.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final String LOGGED_IN_USER_SESSION_KEY = "loggedInUser";

    private final UserService userService;
    private final FoodItemService foodItemService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final ServletContext servletContext;
    private final AppSettingService appSettingService;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;

    public record DailyMenuItemUpdateRequest(boolean available, int quantity) {}
    public record DailyMenuItemBatchUpdateRequest(Long id, boolean available, int quantity) {}

    @Autowired
    public AdminController(UserService userService, FoodItemService foodItemService, OrderService orderService,
                           CategoryService categoryService, ServletContext servletContext,
                           AppSettingService appSettingService, RoleRepository roleRepository,
                           FileStorageService fileStorageService) {
        this.userService = userService;
        this.foodItemService = foodItemService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.servletContext = servletContext;
        this.appSettingService = appSettingService;
        this.roleRepository = roleRepository;
        this.fileStorageService = fileStorageService;
    }

    private User getCurrentlyLoggedInAdmin(HttpSession session) {
        logger.debug("AdminController: Attempting to get currently logged-in admin from HttpSession.");
        UserDto userDtoFromSession = (UserDto) session.getAttribute(LOGGED_IN_USER_SESSION_KEY);

        if (userDtoFromSession == null) {
            logger.warn("AdminController: No UserDto found in HttpSession with key '{}'.", LOGGED_IN_USER_SESSION_KEY);
            return null;
        }

        if (userDtoFromSession.getRoles() == null || !userDtoFromSession.getRoles().contains("ROLE_ADMIN")) {
            logger.warn("AdminController: User '{}' from session does not have ROLE_ADMIN. Actual roles: {}",
                    userDtoFromSession.getUsername(), userDtoFromSession.getRoles());
            return null;
        }
        Optional<User> userOpt = userService.findById(userDtoFromSession.getId());
        if (userOpt.isEmpty()) {
            logger.error("AdminController: Admin UserDto found in session (ID: {}), but no corresponding User entity in database!", userDtoFromSession.getId());
            session.removeAttribute(LOGGED_IN_USER_SESSION_KEY);
            return null;
        }
        User adminUser = userOpt.get();
        logger.debug("AdminController: Logged-in admin (from session & DB): ID={}, Username={}", adminUser.getId(), adminUser.getUsername());
        return adminUser;
    }

    @GetMapping("/api/statistics/revenue-last-7-days")
    @ResponseBody
    public ResponseEntity<List<RevenueByDate>> getRevenueLast7Days(HttpSession session) {
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<RevenueByDate> revenueData = orderService.getRevenueLast7Days();
        return ResponseEntity.ok(revenueData);
    }

    @GetMapping("/api/statistics/top-5-foods")
    @ResponseBody
    public ResponseEntity<List<TopFoodItem>> getTop5Foods(HttpSession session) {
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<TopFoodItem> topFoodData = orderService.getTop5SellingFoodItemsLast30Days();
        return ResponseEntity.ok(topFoodData);
    }


    @GetMapping("/chat")
    public String showAdminChatPage(HttpSession session, Model model) {
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("adminUser", new UserDto(adminUser));
        return "admin/chat";
    }

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/dashboard");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/dashboard. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        logger.debug("AdminController: Admin user for dashboard: {}", adminUser.getUsername());
        model.addAttribute("adminUser", new UserDto(adminUser));
        return "admin/dashboard";
    }


    @GetMapping("/food/all")
    public String showAllFoodItems(@RequestParam(value = "keyword", required = false) String keyword, Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/food/all with keyword: {}", keyword);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/food/all. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }

        List<FoodItem> foodItems;
        if (keyword != null && !keyword.trim().isEmpty()) {
            foodItems = foodItemService.findByNameContaining(keyword);
            logger.info("Found {} food items for keyword '{}'", foodItems.size(), keyword);
        } else {
            foodItems = foodItemService.getAllFoodItems();
        }

        model.addAttribute("foodItems", foodItems);
        model.addAttribute("keyword", keyword);
        return "admin/food-list-all";
    }

    @GetMapping("/food/add")
    public String showAddFoodItemForm(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/food/add");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/food/add. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        model.addAttribute("foodItem", new FoodItem());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/food-form";
    }

    @GetMapping("/food/edit/{id}")
    public String showEditFoodItemForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/food/edit/{}", id);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/food/edit. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        Optional<FoodItem> foodItemOpt = foodItemService.findById(id);
        if (foodItemOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy món ăn với ID: " + id);
            return "redirect:/admin/food/all";
        }
        model.addAttribute("foodItem", foodItemOpt.get());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/food-form";
    }

    @PostMapping("/food/save")
    public String saveFoodItem(@Valid @ModelAttribute("foodItem") FoodItem foodItem,
                               BindingResult bindingResult,
                               @RequestParam("imageFile") MultipartFile imageFile,
                               Model model,
                               RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Processing POST /admin/food/save for food item ID: {}", foodItem.getId());
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for POST /admin/food/save. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            logger.warn("Lỗi validation form món ăn khi lưu: {}", bindingResult.getAllErrors());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/food-form";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".png") || originalFilename.toLowerCase().endsWith(".jpg") || originalFilename.toLowerCase().endsWith(".jpeg"))) {
                try {
                    Path uploadPath = fileStorageService.getFoodImageUploadPath();
                    String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String newFileName = "food_" + System.currentTimeMillis() + fileExtension;

                    File dest = new File(uploadPath.toFile(), newFileName);
                    imageFile.transferTo(dest);

                    String webPath = fileStorageService.getWebUploadPath() + "/food/" + newFileName;
                    foodItem.setImageUrl(webPath);
                    logger.debug("AdminController: Saved image and set web path to: {}", webPath);

                } catch (IOException e) {
                    logger.error("Không thể lưu file ảnh: " + originalFilename, e);
                    bindingResult.rejectValue("imageUrl", "error.foodItem", "Không thể tải lên ảnh: " + e.getMessage());
                    model.addAttribute("categories", categoryService.getAllCategories());
                    return "admin/food-form";
                }
            } else if (originalFilename != null && !originalFilename.isEmpty()){
                bindingResult.rejectValue("imageUrl", "error.foodItem", "Loại file ảnh không hợp lệ. Chỉ chấp nhận PNG, JPG, JPEG.");
                model.addAttribute("categories", categoryService.getAllCategories());
                return "admin/food-form";
            }
        } else if (foodItem.getId() != null) {
            foodItemService.findById(foodItem.getId()).ifPresent(existingFood -> {
                if (foodItem.getImageUrl() == null || foodItem.getImageUrl().isEmpty()) {
                    foodItem.setImageUrl(existingFood.getImageUrl());
                }
            });
        }

        try {
            foodItemService.saveFoodItem(foodItem);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu món ăn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu món ăn: " + e.getMessage());
        }
        return "redirect:/admin/food/all";
    }

    @GetMapping("/food/delete/{id}")
    public String deleteFoodItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/food/delete/{}", id);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/food/delete. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        try {
            foodItemService.deleteFoodItem(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa món ăn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa món ăn: " + e.getMessage() + ". Món ăn có thể đã nằm trong đơn hàng.");
        }
        return "redirect:/admin/food/all";
    }

    @GetMapping("/food/daily-menu")
    public String showDailyMenuSetupForm(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/food/daily-menu");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/food/daily-menu. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        List<FoodItem> allFoodItems = foodItemService.getAllFoodItems();
        model.addAttribute("allFoodItems", allFoodItems);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/daily-menu-setup";
    }

    @PostMapping("/food/daily-menu/save")
    public String saveDailyMenuSetup(@RequestParam(value = "selectedFoodItemIds", required = false) List<Long> selectedFoodItemIds,
                                     @RequestParam Map<String, String> allRequestParams,
                                     RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Processing POST /admin/food/daily-menu/save");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for POST /admin/food/daily-menu/save. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        Map<Long, Integer> dailyQuantities = new HashMap<>();
        if (selectedFoodItemIds != null) {
            for (Long itemId : selectedFoodItemIds) {
                String quantityParam = "dailyQuantity_" + itemId;
                if (allRequestParams.containsKey(quantityParam)) {
                    try {
                        int quantity = Integer.parseInt(allRequestParams.get(quantityParam));
                        dailyQuantities.put(itemId, Math.max(0, quantity));
                    } catch (NumberFormatException e) {
                        dailyQuantities.put(itemId, 0);
                    }
                } else {
                    dailyQuantities.put(itemId, 0);
                }
            }
        }
        try {
            foodItemService.setFoodItemsForToday(selectedFoodItemIds, dailyQuantities);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thực đơn hàng ngày thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật thực đơn hàng ngày: " + e.getMessage());
        }
        return "redirect:/admin/food/daily-menu";
    }

    @PostMapping("/api/food/daily-menu/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateSingleDailyMenuItem(@PathVariable Long id,
                                                       @RequestBody DailyMenuItemUpdateRequest request,
                                                       HttpSession session) {
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        try {
            foodItemService.updateDailyMenuItemStatus(id, request.available(), request.quantity());
            return ResponseEntity.ok(Map.of("message", "Đã lưu món ăn ID: " + id));
        } catch (Exception e) {
            logger.error("Lỗi khi tự động lưu món ăn ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/food/daily-menu/update-batch")
    @ResponseBody
    public ResponseEntity<?> updateBatchDailyMenuItems(@RequestBody List<DailyMenuItemBatchUpdateRequest> updates,
                                                       HttpSession session) {
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        try {
            foodItemService.updateDailyMenuItemsInBatch(updates);
            return ResponseEntity.ok(Map.of("message", "Đã lưu hàng loạt " + updates.size() + " món ăn."));
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật hàng loạt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config/order-time")
    public String showOrderTimeConfigForm(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/config/order-time");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/config/order-time. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        model.addAttribute("currentStartTime", appSettingService.getOrderStartTime());
        model.addAttribute("currentCutoffTime", appSettingService.getOrderCutoffTime());
        return "admin/order-time-config";
    }

    @PostMapping("/config/order-time/save")
    public String saveOrderTimeConfig(@RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                      @RequestParam("cutoffTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime cutoffTime,
                                      RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Processing POST /admin/config/order-time/save with startTime: {}, cutoffTime: {}", startTime, cutoffTime);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for POST /admin/config/order-time/save. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        if (startTime == null || cutoffTime == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Định dạng thời gian không hợp lệ.");
            return "redirect:/admin/config/order-time";
        }
        if (startTime.isAfter(cutoffTime) || startTime.equals(cutoffTime)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thời gian bắt đầu phải trước thời gian kết thúc.");
            return "redirect:/admin/config/order-time";
        }

        try {
            appSettingService.setOrderStartTime(startTime);
            appSettingService.setOrderCutoffTime(cutoffTime);
            String successMsg = String.format("Cập nhật thành công! Giờ mở cửa: %s, Giờ chốt đơn: %s.",
                    startTime.toString(), cutoffTime.toString());
            redirectAttributes.addFlashAttribute("successMessage", successMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật thời gian: " + e.getMessage());
        }
        return "redirect:/admin/config/order-time";
    }


    @GetMapping("/users/list")
    public String showUserList(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/users/list");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/users/list. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/user-list";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/users/edit/{}", id);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/users/edit. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng với ID: " + id);
            return "redirect:/admin/users/list";
        }
        model.addAttribute("userToEdit", userOpt.get());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user-form-edit";
    }

    @PostMapping("/users/update")
    public String updateUserByAdmin(@Valid @ModelAttribute("userToEdit") User userToEdit,
                                    BindingResult bindingResult,
                                    @RequestParam(value = "newPassword", required = false) String newPassword,
                                    @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                                    Model model,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Processing POST /admin/users/update for user ID: {}", userToEdit.getId());
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for POST /admin/users/update. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        if (bindingResult.hasFieldErrors("username") || bindingResult.hasFieldErrors("department") || bindingResult.hasFieldErrors("balance")) {
            logger.warn("AdminController: Validation errors when updating user {}: {}", userToEdit.getId(), bindingResult.getAllErrors());
            model.addAttribute("allRoles", roleRepository.findAll());
            return "admin/user-form-edit";
        }
        try {
            userService.updateUserByAdmin(userToEdit.getId(), userToEdit, newPassword, roleIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công!");
        } catch (RuntimeException e) {
            logger.error("AdminController: Error updating user {}: {}", userToEdit.getId(), e.getMessage());
            model.addAttribute("userToEdit", userToEdit);
            model.addAttribute("allRoles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Lỗi cập nhật người dùng: " + e.getMessage());
            return "admin/user-form-edit";
        }
        return "redirect:/admin/users/list";
    }

    @GetMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/users/toggle-status/{}", id);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/users/toggle-status. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công!");
        } catch (RuntimeException e) {
            logger.error("AdminController: Error toggling status for user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật trạng thái người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users/list";
    }

    @GetMapping("/orders/create-order-for-anyone")
    public String showOrderFormForAnyone(Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/orders/create-order-for-anyone");
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/orders/create-order-for-anyone. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        Map<Category, List<FoodItem>> groupedFoodItems = foodItemService.getGroupedAvailableFoodItemsForToday();
        model.addAttribute("groupedFoodItems", groupedFoodItems);
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        model.addAttribute("orderRequestDto", orderRequestDto);
        logger.debug("AdminController: Fetching all active users for order form.");
        model.addAttribute("allUsers", userService.getAllActiveUsers());
        return "admin/order-form-for-user";
    }

    @PostMapping("/orders/place-as-admin")
    public String placeOrderAsAdmin(@Valid @ModelAttribute("orderRequestDto") OrderRequestDto orderRequestDto,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Processing POST /admin/orders/place-as-admin. RecipientName: {}, TargetUserId: {}",
                orderRequestDto.getRecipientName(), orderRequestDto.getTargetUserId());
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for POST /admin/orders/place-as-admin. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            logger.warn("AdminController: Validation errors when admin places order: {}", bindingResult.getAllErrors());
            model.addAttribute("groupedFoodItems", foodItemService.getGroupedAvailableFoodItemsForToday());
            model.addAttribute("allUsers", userService.getAllActiveUsers());
            return "admin/order-form-for-user";
        }

        if (orderRequestDto.getSelectedItems().isEmpty()) {
            logger.warn("AdminController: No items selected by admin for order.");
            model.addAttribute("errorMessage", "Vui lòng chọn ít nhất một món ăn.");
            model.addAttribute("groupedFoodItems", foodItemService.getGroupedAvailableFoodItemsForToday());
            model.addAttribute("allUsers", userService.getAllActiveUsers());
            return "admin/order-form-for-user";
        }

        if ( (orderRequestDto.getRecipientName() == null || orderRequestDto.getRecipientName().trim().isEmpty()) && orderRequestDto.getTargetUserId() == null) {
            logger.warn("AdminController: Neither recipientName nor targetUserId provided by admin for order.");
            model.addAttribute("errorMessage", "Vui lòng nhập tên người nhận hoặc chọn một người dùng có sẵn.");
            model.addAttribute("groupedFoodItems", foodItemService.getGroupedAvailableFoodItemsForToday());
            model.addAttribute("allUsers", userService.getAllActiveUsers());
            return "admin/order-form-for-user";
        }

        try {
            logger.debug("AdminController: Calling orderService.placeOrderAsAdmin for admin ID: {}", adminUser.getId());
            Order placedOrder = orderService.placeOrderAsAdmin(adminUser.getId(), orderRequestDto);
            String recipientInfo;
            if (placedOrder.getRecipientName() != null && !placedOrder.getRecipientName().isEmpty()) {
                recipientInfo = placedOrder.getRecipientName();
            } else if (placedOrder.getUser() != null) {
                recipientInfo = placedOrder.getUser().getUsername() + " (ID: " + placedOrder.getUser().getId() + ")";
            } else {
                recipientInfo = "Không xác định";
            }
            logger.info("AdminController: Order placed successfully by admin {} for {}. Order ID: {}", adminUser.getUsername(), recipientInfo, placedOrder.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Đặt món thành công cho %s! Mã đơn hàng: %d. Tổng tiền: %.2f VND.",
                            recipientInfo,
                            placedOrder.getId(),
                            placedOrder.getTotalAmount()
                    ));
        } catch (Exception e) {
            logger.error("AdminController: Error when Admin {} places order: {}", adminUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đặt món: " + e.getMessage());
            model.addAttribute("orderRequestDto", orderRequestDto);
            model.addAttribute("groupedFoodItems", foodItemService.getGroupedAvailableFoodItemsForToday());
            model.addAttribute("allUsers", userService.getAllActiveUsers());
            if(orderRequestDto.getTargetUserId() != null) {
                userService.findById(orderRequestDto.getTargetUserId()).ifPresent(u -> model.addAttribute("targetUser", u));
            }
            model.addAttribute("errorMessage", "Lỗi khi đặt món: " + e.getMessage());
            return "admin/order-form-for-user";
        }
        return "redirect:/admin/orders/list";
    }


    @GetMapping("/orders/list")
    public String showOrderListByDate(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model, HttpSession session) {
        logger.info("AdminController: Accessing /admin/orders/list for date: {}", date);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/orders/list. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }

        if (date == null) {
            date = LocalDate.now();
            logger.debug("AdminController: Date not provided for /admin/orders/list, defaulting to today: {}", date);
        }
        List<Order> ordersForDate = orderService.getOrdersByDate(date);
        logger.debug("AdminController: Found {} orders for date {}", (ordersForDate != null ? ordersForDate.size() : 0), date);

        BigDecimal totalRevenueForDate = BigDecimal.ZERO;
        if (ordersForDate != null) {
            totalRevenueForDate = ordersForDate.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        logger.debug("AdminController: Total revenue for date {}: {}", date, totalRevenueForDate);


        Map<String, Integer> foodItemSummary = new HashMap<>();
        if (ordersForDate != null) {
            for (Order order : ordersForDate) {
                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getFoodItem() != null) {
                            String foodName = item.getFoodItem().getName();
                            int quantity = item.getQuantity();
                            foodItemSummary.merge(foodName, quantity, Integer::sum);
                        }
                    }
                }
            }
        }
        logger.debug("AdminController: Food item summary for date {}: {}", date, foodItemSummary);


        model.addAttribute("ordersForDate", ordersForDate);
        model.addAttribute("selectedDate", date);
        model.addAttribute("totalRevenueForDate", totalRevenueForDate);
        model.addAttribute("foodItemSummary", foodItemSummary);

        return "admin/order-list-by-date";
    }

    @PostMapping("/orders/mark-as-paid/{id}")
    public String markOrderAsPaid(@PathVariable("id") Long orderId,
                                  @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Marking order {} as paid", orderId);
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.markOrderAsPaid(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận thanh toán cho đơn hàng #" + orderId);
        } catch (Exception e) {
            logger.error("Error marking order {} as paid: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xác nhận thanh toán: " + e.getMessage());
            return buildErrorRedirectUrl(date);
        }
        return buildRedirectUrl(date, orderId);
    }


    @GetMapping("/orders/delete/{orderId}")
    public String deleteSingleOrder(@PathVariable("orderId") Long orderId,
                                    @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/orders/delete/{} for date: {}", orderId, date);
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.deleteOrderById(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành công đơn hàng (Mã ĐH gốc: " + orderId + ").");
        } catch (Exception e) {
            logger.error("AdminController: Error deleting order ID {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa đơn hàng (Mã ĐH gốc: " + orderId + "): " + e.getMessage());
        }
        return buildRedirectUrl(date);
    }

    @GetMapping("/orders/delete-by-date")
    public String deleteOrdersByDate(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Accessing /admin/orders/delete-by-date for date: {}", date);
        User adminUser = getCurrentlyLoggedInAdmin(session);
        if (adminUser == null) {
            logger.warn("AdminController: Admin user is null for /admin/orders/delete-by-date. Redirecting to /auth/login.");
            return "redirect:/auth/login";
        }
        if (date == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cần chọn ngày.");
            return "redirect:/admin/orders/list";
        }
        try {
            List<Order> ordersToDelete = orderService.getOrdersByDate(date);
            if (ordersToDelete.isEmpty()) {
                redirectAttributes.addFlashAttribute("infoMessage", "Không có đơn hàng nào cho ngày " + date.toString() + " để xóa.");
            } else {
                for (Order order : ordersToDelete) {
                    orderService.deleteOrderById(order.getId());
                }
                foodItemService.resetDailyFoodItemStatus();
                logger.info("AdminController: Đã reset trạng thái món ăn hàng ngày sau khi xóa tất cả đơn hàng ngày {}.", date);
                redirectAttributes.addFlashAttribute("successMessage", "Tất cả đơn hàng cho ngày " + date.toString() + " đã được xóa và trạng thái món ăn đã được reset.");
            }
        } catch (Exception e) {
            logger.error("AdminController: Error deleting orders for date {} or resetting food status: {}", date, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa đơn hàng hoặc reset trạng thái món ăn: " + e.getMessage());
        }
        return "redirect:/admin/orders/list?date=" + date.toString();
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        logger.warn("Admin is attempting to DELETE user with ID: {}", id);
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return "redirect:/auth/login";
        }

        try {
            userService.deleteUserById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành công người dùng ID: " + id);
        } catch (Exception e) {
            logger.error("Error deleting user with ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users/list";
    }

    @PostMapping("/orders/mark-as-unpaid/{id}")
    public String markOrderAsUnpaid(@PathVariable("id") Long orderId,
                                    @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        logger.info("AdminController: Marking order {} as UNPAID", orderId);
        if (getCurrentlyLoggedInAdmin(session) == null) {
            return "redirect:/auth/login";
        }
        try {
            orderService.markOrderAsUnpaid(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy xác nhận thanh toán cho đơn hàng #" + orderId);
        } catch (Exception e) {
            logger.error("Error marking order {} as unpaid: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy xác nhận thanh toán: " + e.getMessage());
            return buildErrorRedirectUrl(date);
        }
        return buildRedirectUrl(date, orderId);
    }

    private String buildRedirectUrl(LocalDate date) {
        return buildRedirectUrl(date, null);
    }

    private String buildRedirectUrl(LocalDate date, Long orderId) {
        String baseUrl = "/admin/orders/list?date=";
        String dateString = (date != null) ? date.toString() : LocalDate.now().toString();
        String fragment = (orderId != null) ? "#order-" + orderId : "";
        return "redirect:" + baseUrl + dateString + fragment;
    }

    private String buildErrorRedirectUrl(LocalDate date) {
        String baseUrl = "/admin/orders/list";
        if (date != null) {
            baseUrl += "?date=" + date.toString();
        }
        return "redirect:" + baseUrl;
    }
}