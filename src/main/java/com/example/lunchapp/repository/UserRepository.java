package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.Role;
import com.example.lunchapp.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByEnabledTrue();
    Optional<User> findFirstByRolesContains(Role role);

    @Query("SELECT DISTINCT u FROM User u JOIN ChatMessage cm ON u.id = cm.sender.id OR u.id = cm.recipient.id WHERE (cm.sender.id = :adminId OR cm.recipient.id = :adminId) AND u.id != :adminId")
    List<User> findUsersWithChatHistoryWithAdmin(@Param("adminId") Long adminId);
}