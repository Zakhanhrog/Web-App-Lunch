package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.sender JOIN FETCH cm.recipient WHERE (cm.sender.id = :userId1 AND cm.recipient.id = :userId2) OR (cm.sender.id = :userId2 AND cm.recipient.id = :userId1) ORDER BY cm.timestamp ASC")
    List<ChatMessage> findChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    long countByRecipient_IdAndIsReadFalse(Long recipientId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.recipient.id = :recipientId AND cm.isRead = false ORDER BY cm.timestamp ASC")
    List<ChatMessage> findUnreadMessages(@Param("recipientId") Long recipientId);

    @Query("SELECT cm FROM ChatMessage cm WHERE (cm.sender.id = :userId1 AND cm.recipient.id = :userId2) OR (cm.sender.id = :userId2 AND cm.recipient.id = :userId1) ORDER BY cm.timestamp DESC")
    List<ChatMessage> findLastMessageBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}