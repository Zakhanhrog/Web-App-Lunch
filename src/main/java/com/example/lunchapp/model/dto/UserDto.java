package com.example.lunchapp.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import com.example.lunchapp.model.entity.User; // Cần import User entity

@Getter
@Setter
public class UserDto {
    private Long id;
    private String username;
    private String department;
    private BigDecimal balance;
    private Set<String> roles;

    // Constructor để chuyển từ User entity sang UserDto
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.department = user.getDepartment();
        this.balance = user.getBalance();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
    }
}