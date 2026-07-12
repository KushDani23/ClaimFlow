package com.company.icps.notification.controller;

import com.company.icps.common.constants.AppConstants;
import com.company.icps.common.exception.ResourceNotFoundException;
import com.company.icps.common.response.ApiResponse;
import com.company.icps.notification.dto.NotificationResponse;
import com.company.icps.notification.entity.Notification;
import com.company.icps.notification.repository.NotificationRepository;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Manage user in-app notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Operation(summary = "Get all notifications for the authenticated user")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Authentication authentication) {
        
        User user = getUser(authentication.getName());
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
        
        Page<NotificationResponse> response = notifications.map(n -> NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", response));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        User user = getUser(authentication.getName());
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", Map.of("unreadCount", count)));
    }

    @Operation(summary = "Mark notification as read")
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication.getName());
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot modify this notification");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
