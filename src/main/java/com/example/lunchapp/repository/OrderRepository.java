package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.Order;
import com.example.lunchapp.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user u LEFT JOIN FETCH o.orderedByAdmin oba LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.foodItem fi WHERE o.user = :userParam AND o.orderedByAdmin IS NULL ORDER BY o.orderDate DESC")
    List<Order> findByUserAndOrderedByAdminIsNullOrderByOrderDateDesc(@Param("userParam") User user);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user u LEFT JOIN FETCH o.orderedByAdmin oba LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.foodItem fi WHERE o.orderDate >= :startDate AND o.orderDate < :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetweenFetchingAll(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    boolean existsByUser_IdAndOrderDateBetweenAndRecipientNameIsNullAndOrderedByAdminIsNull(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    Optional<Order> findFirstByUser_IdAndOrderDateBetweenAndRecipientNameIsNullAndOrderedByAdminIsNullOrderByOrderDateDesc(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    boolean existsByUser_IdAndOrderDateBetweenAndOrderedByAdminIsNotNull(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    boolean existsByUser_IdAndOrderDateBetweenAndOrderedByAdminIsNull(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    boolean existsByRecipientNameAndOrderDateBetweenAndOrderedByAdminIsNotNull(String recipientName, LocalDateTime startOfDay, LocalDateTime endOfDay);
    boolean existsByUser_IdAndOrderDateBetweenAndRecipientNameIsNotNullAndOrderedByAdminIsNull(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    boolean existsByRecipientNameAndOrderDateBetweenAndUserIsNotNullAndOrderedByAdminIsNull(String recipientName, LocalDateTime startOfDay, LocalDateTime endOfDay);
    Optional<Order> findFirstByUser_IdAndOrderDateBetweenAndRecipientNameIsNotNullAndOrderedByAdminIsNullOrderByOrderDateDesc(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.user u LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.foodItem fi WHERE u.id = :placingUserId AND o.orderedByAdmin IS NULL AND o.orderDate >= :startOfDay AND o.orderDate < :endOfDay ORDER BY o.orderDate DESC")
    List<Order> findAllByUser_IdAndOrderDateBetweenAndOrderedByAdminIsNullOrderByOrderDateDesc(@Param("placingUserId") Long placingUserId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.user u LEFT JOIN FETCH o.orderedByAdmin oba LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.foodItem fi WHERE (o.user = :userParam AND o.orderedByAdmin IS NULL) ORDER BY o.orderDate DESC")
    List<Order> findAllOrdersPlacedByUser(@Param("userParam") User user);

    boolean existsByUser_Id(Long userId);
}