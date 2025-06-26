package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    @NotEmpty(message = "Please select at least one item to order.")
    @Valid
    private List<SelectedFoodItemDto> selectedItems = new ArrayList<>();

    private String note;
    private String recipientName;
    private Long targetUserId;
}