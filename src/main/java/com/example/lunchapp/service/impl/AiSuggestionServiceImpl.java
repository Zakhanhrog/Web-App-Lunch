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

    private final String apiKey;

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

        // Đọc API Key của Groq từ biến môi trường
        this.apiKey = System.getenv("GROQ_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            logger.error("!!! CRITICAL: GROQ_API_KEY environment variable is not set. AI features will not work.");
        }
    }

    @Override
    public AiSuggestionResponse getMealSuggestion(String userPrompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("AI service is not configured. Missing Groq API Key.");
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
        String userMessage = "Thực đơn hôm nay là: \n" + menuJson + "\n\nYêu cầu của tôi là: \"" + userPrompt + "\"";

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

            return objectMapper.readValue(cleanJson, AiSuggestionResponse.class);

        } catch (Exception e) {
            logger.error("Error calling Groq API", e);
            throw new IOException("Không thể nhận gợi ý từ AI. " + e.getMessage(), e);
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

        // Thêm yêu cầu trả về JSON
        ObjectNode responseFormatNode = objectMapper.createObjectNode();
        responseFormatNode.put("type", "json_object");
        requestBody.set("response_format", responseFormatNode);

        return requestBody;
    }

    private String buildSystemPrompt() {
        return "Bạn là một chuyên gia dinh dưỡng, am hiểu ẩm thực văn phòng Việt Nam. Nhiệm vụ của bạn là tạo một thực đơn bữa trưa cân bằng, ngon miệng và hợp lý từ danh sách món ăn có sẵn."
                + "\n\n**QUY TẮC BẮT BUỘC:**"
                + "\n- Bạn chỉ được sử dụng các món ăn có trong thực đơn hôm nay. Không được thêm món mới hoặc món không có trong danh sách. Và chỉ được nói tiếng Việt."
                + "\n1. **Bám sát yêu cầu:** Luôn tuân thủ yêu cầu của người dùng về ngân sách, sở thích (ví dụ: món chay, món mặn...), và số lượng món."
                + "\n2. **Ưu tiên đa dạng:** Cố gắng kết hợp đủ các nhóm món như cơm, món mặn (đạm), và món rau/canh. Tránh chọn quá nhiều món trong cùng một nhóm nếu không được yêu cầu cụ thể."
                + "\n3. **Tối ưu ngân sách:** Chọn các món sao cho tổng giá tiền gần với ngân sách yêu cầu nhất có thể nhưng **không được vượt quá**."
                + "\n4. **Giải thích thông minh:** Trong phần 'explanation', hãy giải thích ngắn gọn, thân thiện tại sao bạn chọn thực đơn đó (ví dụ: 'Tôi đã chọn cho bạn một suất ăn 30k đủ chất gồm cơm, thịt kho và canh rau, đảm bảo năng lượng cho buổi chiều.')."
                + "\n5. **Luôn trả về JSON:** Kết quả cuối cùng **chỉ được phép** là một chuỗi JSON hợp lệ theo đúng định dạng mẫu, không có bất kỳ ký tự hay lời dẫn nào bên ngoài cặp dấu `{}`."
                + "\n\n**JSON MẪU ĐỂ TRẢ VỀ:**\n"
                + "{\"suggestedItems\": [{\"id\": 1, \"name\": \"Tên món\"}], \"totalPrice\": 30000, \"explanation\": \"Gợi ý của bạn ở đây.\"}";
    }

    private String convertFoodItemsToJson(List<FoodItem> items) throws JsonProcessingException {
        List<SimpleFoodItem> simpleItems = items.stream()
                .map(item -> new SimpleFoodItem(item.getId(), item.getName(), item.getPrice().intValue(), item.getCategory() != null ? item.getCategory().getName() : "Khác"))
                .collect(Collectors.toList());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleItems);
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