package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;
import com.example.lunchapp.model.dto.ChatMessage;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.OrderItem;
import com.example.lunchapp.service.AiChatService;
import com.example.lunchapp.service.FoodItemService;
import com.example.lunchapp.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiSuggestionServiceImpl implements AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiSuggestionServiceImpl.class);

    private final FoodItemService foodItemService;
    private final OrderService orderService; // Thêm OrderService để lấy lịch sử
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.key:}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Autowired
    public AiSuggestionServiceImpl(FoodItemService foodItemService, OrderService orderService, ObjectMapper objectMapper) {
        this.foodItemService = foodItemService;
        this.orderService = orderService; // Injecct OrderService
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public AiChatResponse getChatResponse(AiChatRequest chatRequest, Long userId) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("!!! CRITICAL: Groq API Key is not configured (GROQ_API_KEY).");
            throw new IOException("Dịch vụ AI chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        // 1. Lấy ngữ cảnh: Thực đơn hôm nay và món ăn yêu thích của người dùng
        List<FoodItem> availableFoodItems = foodItemService.getAvailableFoodItemsForToday();
        String menuJson = convertFoodItemsToJson(availableFoodItems);
        String favoriteFoods = extractFavoriteFoods(userId);

        // 2. Xây dựng prompt hệ thống mới, thông minh hơn
        String systemMessage = buildSystemPrompt(menuJson, favoriteFoods);

        // 3. Gửi yêu cầu đến AI
        ObjectNode requestBodyJson = buildRequestBody(systemMessage, chatRequest.getHistory(), chatRequest.getNewMessage());
        RequestBody body = RequestBody.create(requestBodyJson.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                logger.error("Groq API request failed with code {}: {}", response.code(), responseBody);
                throw new IOException("Yêu cầu đến AI thất bại. " + responseBody);
            }

            logger.info("Raw response from Groq AI: {}", responseBody);
            String cleanJson = cleanupAiResponse(responseBody);
            logger.info("Cleaned JSON from AI: {}", cleanJson);

            // 4. Xử lý phản hồi JSON
            JsonNode root = objectMapper.readTree(cleanJson);
            String aiResponseContent = root.path("choices").get(0).path("message").path("content").asText();

            String finalCleanJson = cleanupAiResponse(aiResponseContent);
            logger.info("Final clean JSON content: {}", finalCleanJson);

            // 5. Trả về kết quả mà không cần cập nhật ID nữa
            return objectMapper.readValue(finalCleanJson, AiChatResponse.class);

        } catch (Exception e) {
            logger.error("Error calling Groq API or parsing response", e);
            throw new IOException("Không thể nhận phản hồi từ AI. Lỗi xử lý dữ liệu. " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(String systemMessage, List<ChatMessage> history, String newMessage) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", this.model);

        ArrayNode messages = requestBody.putArray("messages");

        ObjectNode systemMessageNode = objectMapper.createObjectNode();
        systemMessageNode.put("role", "system");
        systemMessageNode.put("content", systemMessage);
        messages.add(systemMessageNode);

        history.forEach(chatMessage -> {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", chatMessage.getRole());
            messageNode.put("content", chatMessage.getContent());
            messages.add(messageNode);
        });

        ObjectNode userMessageNode = objectMapper.createObjectNode();
        userMessageNode.put("role", "user");
        userMessageNode.put("content", newMessage);
        messages.add(userMessageNode);

        ObjectNode responseFormatNode = objectMapper.createObjectNode();
        responseFormatNode.put("type", "json_object");
        requestBody.set("response_format", responseFormatNode);

        return requestBody;
    }

    private String buildSystemPrompt(String menuJson, String favoriteFoods) {
        return "Bạn là 'AI ChiyoCare', một trợ lý đặt cơm trưa thông minh và thân thiện, luôn giao tiếp bằng tiếng Việt và LUÔN xưng tên ChiyoCare trong mọi câu trả lời, kể cả khi chỉ trò chuyện phiếm."
                + "\n\n**BỐI CẢNH:**"
                + "\n- Thực đơn hôm nay có các món sau (JSON): " + menuJson
                + "\n- Lịch sử đặt món của người dùng cho thấy họ thường thích các món: " + favoriteFoods
                + "\n\n**NHIỆM VỤ TỐI THƯỢNG:**"
                + "\nPhân tích yêu cầu của người dùng và trả về một đối tượng JSON DUY NHẤT. KHÔNG được phép có bất kỳ ký tự nào khác bên ngoài cặp dấu `{}`."
                + "\n\n**QUY TẮC VÀNG:**"
                + "\n1.  **HIỂU & GỢI Ý:** Nếu người dùng muốn gợi ý món ăn, hãy dựa vào cả thực đơn hôm nay và món họ yêu thích để tạo một combo hợp lý. Giải thích ngắn gọn tại sao bạn chọn chúng."
                + "\n2.  **CHÍNH XÁC TUYỆT ĐỐI:** Khi gợi ý món ăn trong mảng `suggestedItems`, bạn BẮT BUỘC phải sử dụng chính xác `id` và `name` từ JSON thực đơn đã cho."
                + "\n3.  **TRÒ CHUYỆN TỰ NHIÊN:** Nếu người dùng chỉ chào hỏi hoặc nói chuyện phiếm, hãy phản hồi một cách thân thiện trong trường `explanation`, **nhưng luôn phải nhắc tên ChiyoCare trong câu trả lời**. Trong trường hợp này, để `suggestedItems` là một mảng rỗng `[]`."
                + "\n4.  **XỬ LÝ NGÂN SÁCH:** Nếu người dùng đưa ra ngân sách, chọn các món có tổng giá tiền không vượt quá. Nếu không thể, hãy giải thích và không gợi ý món nào."
                + "\n\n**ĐỊNH DẠNG JSON TRẢ VỀ BẮT BUỘC:**"
                + "\n```json\n"
                + "{\n"
                + "  \"explanation\": \"Câu trò chuyện hoặc giải thích của ChiyoCare ở đây. Ví dụ: 'Chào bạn, ChiyoCare thấy bạn hay ăn cay, hôm nay có món Gà xào sả ớt rất ngon đó, bạn thử nhé!'\",\n"
                + "  \"suggestedItems\": [\n"
                + "    {\"id\": 15, \"name\": \"Gà xào sả ớt\"}\n"
                + "  ],\n"
                + "  \"totalPrice\": 35000\n"
                + "}\n"
                + "```";
    }


    private String convertFoodItemsToJson(List<FoodItem> items) throws JsonProcessingException {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        List<SimpleFoodItem> simpleItems = items.stream()
                .map(item -> new SimpleFoodItem(item.getId(), item.getName(), item.getPrice().intValue(), item.getCategory() != null ? item.getCategory().getName() : "Khác"))
                .collect(Collectors.toList());
        return objectMapper.writeValueAsString(simpleItems);
    }

    private String extractFavoriteFoods(Long userId) {
        try {
            List<Order> orderHistory = orderService.getOrderHistory(userId);
            if (orderHistory.isEmpty()) {
                return "Chưa có dữ liệu.";
            }
            // Đếm số lần xuất hiện của mỗi món ăn
            Map<String, Long> foodFrequency = orderHistory.stream()
                    .flatMap(order -> order.getOrderItems().stream())
                    .map(OrderItem::getFoodItem)
                    .collect(Collectors.groupingBy(FoodItem::getName, Collectors.counting()));

            // Sắp xếp và lấy ra 3 món hàng đầu
            return foodFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            logger.warn("Không thể lấy lịch sử đặt món cho người dùng ID {}: {}", userId, e.getMessage());
            return "Không thể truy cập.";
        }
    }

    private String cleanupAiResponse(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim();
        }
        return text.trim();
    }

    // Lớp nội tại để đơn giản hóa đối tượng gửi cho AI
    private static class SimpleFoodItem {
        public Long id; // THÊM ID VÀO ĐÂY
        public String name;
        public int price;
        public String category;
        public SimpleFoodItem(Long id, String name, int price, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.category = category;
        }
    }

    // Ghi đè phương thức từ interface
    @Override
    public AiChatResponse getChatResponse(AiChatRequest chatRequest) throws IOException {
        throw new UnsupportedOperationException("Please provide userId to get a personalized response.");
    }
}