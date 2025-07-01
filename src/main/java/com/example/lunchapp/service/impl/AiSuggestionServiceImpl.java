package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.AiChatRequest;
import com.example.lunchapp.model.dto.AiChatResponse;
import com.example.lunchapp.model.dto.ChatMessage;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.service.AiChatService;
import com.example.lunchapp.service.FoodItemService;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiSuggestionServiceImpl implements AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiSuggestionServiceImpl.class);

    private final FoodItemService foodItemService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.key:}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Autowired
    public AiSuggestionServiceImpl(FoodItemService foodItemService, ObjectMapper objectMapper) {
        this.foodItemService = foodItemService;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public AiChatResponse getChatResponse(AiChatRequest chatRequest) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("!!! CRITICAL: Groq API Key is not configured (GROQ_API_KEY).");
            throw new IOException("Dịch vụ AI chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        List<FoodItem> availableFoodItems = foodItemService.getAvailableFoodItemsForToday();
        String menuJson = convertFoodItemsToJson(availableFoodItems);
        String systemMessage = buildSystemPrompt(menuJson);

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

            JsonNode root = objectMapper.readTree(cleanJson);
            String aiResponseText = root.path("choices").get(0).path("message").path("content").asText();

            String finalCleanJson = cleanupAiResponse(aiResponseText);
            logger.info("Final clean JSON content: {}", finalCleanJson);

            AiChatResponse aiChatResponse = objectMapper.readValue(finalCleanJson, AiChatResponse.class);
            updateItemIdsFromName(aiChatResponse, availableFoodItems);
            return aiChatResponse;

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

    private String buildSystemPrompt(String menuJson) {
        return "Bạn là 'CG LUNCH AI', một chuyên gia tư vấn ẩm thực và sức khỏe thông minh, thân thiện. Bạn chỉ giao tiếp bằng tiếng Việt."
                + "\n\n**BỐI CẢNH:**"
                + "\nBạn đang trò chuyện với nhân viên trong ứng dụng đặt cơm trưa. Thực đơn hôm nay có các món sau (định dạng JSON): \n" + menuJson
                + "\n\n**NHIỆM VỤ TỐI THƯỢNG:**"
                + "\nBạn phải vừa trò chuyện tự nhiên, vừa đưa ra gợi ý món ăn khi cần. ĐẦU RA CỦA BẠN **BẮT BUỘC** PHẢI LÀ MỘT ĐỐI TƯỢNG JSON DUY NHẤT. Không được phép có bất kỳ ký tự nào khác bên ngoài cặp dấu `{}`."
                + "\n\n**QUY TẮC VÀNG:**"
                + "\n1.  **HIỂU Ý ĐỒ:** Phân tích kỹ yêu cầu của người dùng. Nếu họ chỉ chào hỏi hoặc trò chuyện phiếm, hãy trả lời một cách tự nhiên. Nếu họ thể hiện ý muốn ăn uống (vd: 'ăn gì bây giờ', 'gợi ý suất 50k', 'hôm nay mệt quá'), hãy đưa ra một thực đơn cụ thể."
                + "\n2.  **ĐƯA RA GỢI Ý:** Khi đưa ra gợi ý, hãy chọn các món có trong thực đơn, kết hợp chúng một cách hợp lý và giải thích tại sao bạn chọn chúng trong trường `explanation`."
                + "\n3.  **QUẢN LÝ NGÂN SÁCH:** Nếu người dùng đưa ra ngân sách, hãy chọn các món có tổng giá tiền gần nhất nhưng không được vượt quá."
                + "\n4.  **XỬ LÝ TÌNH HUỐNG:**"
                + "\n    - Nếu chỉ trò chuyện, hãy trả về một `explanation` tự nhiên và để `suggestedItems` là một mảng rỗng `[]`."
                + "\n    - Nếu không thể đáp ứng yêu cầu (vd: ngân sách quá thấp), hãy giải thích trong `explanation` và để `suggestedItems` rỗng `[]`."
                + "\n\n**ĐỊNH DẠNG JSON BẮT BUỘC TRẢ VỀ (CỰC KỲ QUAN TRỌNG):**"
                + "\n```json\n"
                + "{\n"
                + "  \"explanation\": \"Phần trò chuyện và giải thích của bạn ở đây. Ví dụ: 'Chào bạn, hôm nay bạn muốn ăn gì nào?' hoặc 'Dựa trên yêu cầu một bữa ăn giàu năng lượng, mình đã chọn cho bạn cơm sườn và canh cải. Combo này sẽ giúp bạn tỉnh táo suốt buổi chiều đấy!'\",\n"
                + "  \"suggestedItems\": [\n"
                + "    {\"id\": 0, \"name\": \"Cơm sườn\"},\n"
                + "    {\"id\": 0, \"name\": \"Canh cải thịt bằm\"}\n"
                + "  ],\n"
                + "  \"totalPrice\": 50000\n"
                + "}\n"
                + "```\n"
                + "**LƯU Ý:** `id` trong `suggestedItems` bạn có thể để là 0, hệ thống sẽ tự cập nhật. Chỉ cần `name` chính xác.";
    }

    private String convertFoodItemsToJson(List<FoodItem> items) throws JsonProcessingException {
        if (items.isEmpty()) {
            return "[]";
        }
        List<SimpleFoodItem> simpleItems = items.stream()
                .map(item -> new SimpleFoodItem(item.getName(), item.getPrice().intValue(), item.getCategory() != null ? item.getCategory().getName() : "Khác"))
                .collect(Collectors.toList());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleItems);
    }

    private void updateItemIdsFromName(AiChatResponse response, List<FoodItem> availableItems) {
        if (response.getSuggestedItems() == null) return;

        for (AiChatResponse.SuggestedItem suggestedItem : response.getSuggestedItems()) {
            availableItems.stream()
                    .filter(foodItem -> foodItem.getName().equalsIgnoreCase(suggestedItem.getName()))
                    .findFirst()
                    .ifPresent(foodItem -> suggestedItem.setId(foodItem.getId()));
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

    private static class SimpleFoodItem {
        public String name;
        public int price;
        public String category;
        public SimpleFoodItem(String name, int price, String category) {
            this.name = name;
            this.price = price;
            this.category = category;
        }
    }
}