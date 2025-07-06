package com.example.lunchapp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long recipientId;
    private Long senderId;
    private String senderUsername;
    private String messageContent;
}