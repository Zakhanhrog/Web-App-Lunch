package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.AiSuggestionResponse;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.service.AiSuggestionService;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiSuggestionServiceImpl implements AiSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(AiSuggestionServiceImpl.class);

    private final FoodItemService foodItemService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.key:}") // Thêm giá trị mặc định là chuỗi rỗng
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
    public AiSuggestionResponse getMealSuggestion(String userPrompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("!!! CRITICAL: Groq API Key is not configured as an environment variable (GROQ_API_KEY). AI features will not work.");
            throw new IOException("Dịch vụ AI chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        List<FoodItem> availableFoodItems = foodItemService.getAvailableFoodItemsForToday();
        if (availableFoodItems.isEmpty()) {
            AiSuggestionResponse emptyResponse = new AiSuggestionResponse();
            emptyResponse.setExplanation("Rất tiếc, hôm nay không có món nào trong thực đơn để gợi ý.");
            emptyResponse.setSuggestedItems(Collections.emptyList());
            return emptyResponse;
        }

        String menuJson = convertFoodItemsToJson(availableFoodItems);
        String systemMessage = buildSystemPrompt();
        String userMessage = "Thực đơn hôm nay là: \n" + menuJson + "\n\nYêu cầu của người dùng là: \"" + userPrompt + "\"";

        ObjectNode requestBodyJson = buildRequestBody(systemMessage, userMessage);
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

            JsonNode root = objectMapper.readTree(responseBody);
            String aiResponseText = root.path("choices").get(0).path("message").path("content").asText();

            String cleanJson = cleanupAiResponse(aiResponseText);
            logger.info("Received clean JSON from Groq AI: {}", cleanJson);

            // Cập nhật lại các ID món ăn từ tên được trả về
            AiSuggestionResponse aiSuggestionResponse = objectMapper.readValue(cleanJson, AiSuggestionResponse.class);
            updateItemIdsFromName(aiSuggestionResponse, availableFoodItems);
            return aiSuggestionResponse;

        } catch (Exception e) {
            logger.error("Error calling Groq API or parsing response", e);
            throw new IOException("Không thể nhận gợi ý từ AI. Lỗi xử lý dữ liệu. " + e.getMessage(), e);
        }
    }

    private ObjectNode buildRequestBody(String systemMessage, String userMessage) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", this.model);

        ArrayNode messages = requestBody.putArray("messages");

        ObjectNode systemMessageNode = objectMapper.createObjectNode();
        systemMessageNode.put("role", "system");
        systemMessageNode.put("content", systemMessage);
        messages.add(systemMessageNode);

        ObjectNode userMessageNode = objectMapper.createObjectNode();
        userMessageNode.put("role", "user");
        userMessageNode.put("content", userMessage);
        messages.add(userMessageNode);

        ObjectNode responseFormatNode = objectMapper.createObjectNode();
        responseFormatNode.put("type", "json_object");
        requestBody.set("response_format", responseFormatNode);

        return requestBody;
    }

    private String buildSystemPrompt() {
        return "Bạn là một trợ lý ảo tư vấn dinh dưỡng tên là 'CG LUNCH AI', thân thiện, thông minh, và am hiểu sâu sắc về sức khỏe cũng như ẩm thực văn phòng tại Việt Nam. Bạn chỉ được giao tiếp bằng tiếng Việt."
                + "\n\n**NHIỆM VỤ CHÍNH:**"
                + "\nDựa trên yêu cầu của người dùng và danh sách các món ăn có sẵn được cung cấp, hãy tư vấn một bữa trưa hoàn hảo và trả về kết quả dưới dạng một đối tượng JSON duy nhất."
                + "\n\n**QUY TẮC VÀNG (BẮT BUỘC TUÂN THỦ):**"
                + "\n1.  **CHỈ DÙNG MÓN CÓ SẴN:** Tuyệt đối chỉ được chọn các món ăn từ danh sách được cung cấp. Không được bịa ra món mới."
                + "\n2.  **HIỂU YÊU CẦU:** Phân tích kỹ yêu cầu của người dùng về: sở thích (chay, mặn, ít dầu mỡ, nhiều đạm...), vấn đề sức khỏe (mệt mỏi, cần tỉnh táo, ăn kiêng...), và ngân sách (ví dụ: 'suất 30k')."
                + "\n3.  **TƯ VẤN THÔNG MINH:** Trong phần `explanation`, hãy giải thích một cách thuyết phục và thân thiện tại sao bạn lại chọn thực đơn đó. Ví dụ: 'Hôm nay bạn hơi mệt nên mình đã chọn một suất ăn giàu đạm với thịt bò xào và nhiều vitamin từ rau luộc. Thực đơn này sẽ giúp bạn phục hồi năng lượng cho buổi chiều làm việc hiệu quả nhé!'."
                + "\n4.  **TỐI ƯU HÓA:** Luôn cố gắng kết hợp các món ăn một cách hài hòa (ví dụ: có món mặn, món rau/canh). Nếu có ngân sách, hãy chọn các món có tổng giá tiền gần nhất với ngân sách nhưng **KHÔNG ĐƯỢC VƯỢT QUÁ**."
                + "\n5.  **XỬ LÝ TÌNH HUỐNG:** Nếu yêu cầu của người dùng không thể đáp ứng (ví dụ: ngân sách quá thấp, không có món chay), hãy trả về một `explanation` lịch sự giải thích lý do và có thể gợi ý một phương án khác nếu có thể."
                + "\n6.  **LUÔN TRẢ VỀ JSON:** Đầu ra của bạn **CHỈ ĐƯỢC PHÉP** là một chuỗi JSON hợp lệ. Không được thêm bất kỳ văn bản, ghi chú, hay ký tự nào (như '```json') bên ngoài cặp dấu `{}` của đối tượng JSON."
                + "\n\n**ĐỊNH DẠNG JSON BẮT BUỘC TRẢ VỀ:**"
                + "\n```json\n"
                + "{\n"
                + "  \"suggestedItems\": [\n"
                + "    {\"id\": 1, \"name\": \"Cơm trắng\"},\n"
                + "    {\"id\": 5, \"name\": \"Thịt kho tàu\"}\n"
                + "  ],\n"
                + "  \"totalPrice\": 35000,\n"
                + "  \"explanation\": \"Gợi ý và lời giải thích thông minh của bạn ở đây.\"\n"
                + "}\n"
                + "```";
    }

    private String convertFoodItemsToJson(List<FoodItem> items) throws JsonProcessingException {
        List<SimpleFoodItem> simpleItems = items.stream()
                .map(item -> new SimpleFoodItem(item.getId(), item.getName(), item.getPrice().intValue(), item.getCategory() != null ? item.getCategory().getName() : "Khác"))
                .collect(Collectors.toList());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleItems);
    }

    private void updateItemIdsFromName(AiSuggestionResponse response, List<FoodItem> availableItems) {
        if (response.getSuggestedItems() == null) return;

        for (AiSuggestionResponse.SuggestedItem suggestedItem : response.getSuggestedItems()) {
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
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text.trim();
    }

    private static class SimpleFoodItem {
        public Long id;
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
}