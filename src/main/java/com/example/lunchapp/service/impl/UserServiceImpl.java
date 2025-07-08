package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.dto.UserRegistrationDto;
import com.example.lunchapp.model.entity.Role;
import com.example.lunchapp.model.entity.User;
import com.example.lunchapp.repository.OrderRepository;
import com.example.lunchapp.repository.RoleRepository;
import com.example.lunchapp.repository.UserRepository;
import com.example.lunchapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Lỗi: Tên đăng nhập đã tồn tại!");
        }
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new RuntimeException("Lỗi: Mật khẩu nhập lại không khớp!");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setDepartment(registrationDto.getDepartment());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setBalance(registrationDto.getInitialBalance() != null ? registrationDto.getInitialBalance() : BigDecimal.ZERO);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role newRole = new Role("ROLE_USER");
            return roleRepository.save(newRole);
        });

        user.getRoles().add(userRole);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Không tìm thấy người dùng với ID: " + userId);
        }
        if (orderRepository.existsByUser_Id(userId)) {
            throw new RuntimeException("Không thể xóa người dùng đã có lịch sử đặt hàng. Hãy xem xét việc vô hiệu hóa tài khoản.");
        }
        userRepository.deleteById(userId);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public User save(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User depositMoney(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        user.setBalance(user.getBalance().add(amount));
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUserByAdmin(Long userId, User updatedUserPartialInfo, String newPassword, List<Long> roleIds) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (updatedUserPartialInfo.getUsername() != null && !updatedUserPartialInfo.getUsername().isEmpty() &&
                !updatedUserPartialInfo.getUsername().equals(userToUpdate.getUsername())) {
            if (userRepository.existsByUsername(updatedUserPartialInfo.getUsername())) {
                throw new RuntimeException("Error: Username '" + updatedUserPartialInfo.getUsername() + "' is already taken!");
            }
            userToUpdate.setUsername(updatedUserPartialInfo.getUsername());
        }

        if (updatedUserPartialInfo.getDepartment() != null && !updatedUserPartialInfo.getDepartment().isEmpty()) {
            userToUpdate.setDepartment(updatedUserPartialInfo.getDepartment());
        }

        if (updatedUserPartialInfo.getBalance() != null) {
            userToUpdate.setBalance(updatedUserPartialInfo.getBalance());
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            userToUpdate.setPassword(passwordEncoder.encode(newPassword));
        }

        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> newRoles = roleIds.stream()
                    .map(roleId -> roleRepository.findById(roleId)
                            .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId)))
                    .collect(Collectors.toSet());
            userToUpdate.setRoles(newRoles);
        }

        userToUpdate.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(userToUpdate);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findByEnabledTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public User findAdmin() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found."));
        return userRepository.findFirstByRolesContains(adminRole)
                .orElseThrow(() -> new RuntimeException("Admin user not found."));
    }
}