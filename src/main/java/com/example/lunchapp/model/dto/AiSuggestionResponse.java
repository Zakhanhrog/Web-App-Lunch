package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AiSuggestionResponse {
    private List<SuggestedItem> suggestedItems;
    private BigDecimal totalPrice;
    private String explanation;

    @Getter
    @Setter
    public static class SuggestedItem {
        private Long id;
        private String name;
    }
}