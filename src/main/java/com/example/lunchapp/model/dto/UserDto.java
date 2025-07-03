package com.example.lunchapp.model.dto;

import com.example.lunchapp.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserDto {
    private Long id;
    private String username;
    private String department;
    private BigDecimal balance;
    private Set<String> roles;
    private boolean admin;

    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.department = user.getDepartment();
        this.balance = user.getBalance();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        this.admin = this.roles.contains("ROLE_ADMIN");
    }
}