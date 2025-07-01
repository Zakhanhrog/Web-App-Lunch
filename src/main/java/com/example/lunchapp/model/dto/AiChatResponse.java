package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AiChatResponse {
    private String explanation;
    private List<SuggestedItem> suggestedItems;
    private BigDecimal totalPrice;

    @Getter
    @Setter
    public static class SuggestedItem {
        private Long id;
        private String name;
    }
}