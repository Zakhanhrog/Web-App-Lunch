package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class SelectedFoodItemDto {
    @NotNull(message = "Mã món ăn không được để trống.")
    private Long foodItemId;

    @NotNull(message = "Số lượng không được để trống.")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1.")
    private Integer quantity = 0;
}