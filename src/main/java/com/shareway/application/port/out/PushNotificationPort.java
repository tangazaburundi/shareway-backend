package com.shareway.application.port.out;

import java.util.Map;

/**
 * Port for push notifications (Web Push / FCM).
 * SMS can be disabled from admin panel; push notifications are ALWAYS active.
 */
public interface PushNotificationPort {

    /**
     * Send a push notification to a specific user.
     * If the user has no device token, the notification is silently ignored.
     */
    void sendPushNotification(String userId, String title, String body, Map<String, Object> data);

    /**
     * Send a push notification to all admin users.
     */
    void sendPushNotificationToAdmins(String title, String body, Map<String, Object> data);

    /**
     * Broadcast a push notification to all active users.
     */
    void broadcastPushNotification(String title, String body, Map<String, Object> data);

    /**
     * Register a device token (FCM or Web Push subscription) for a user.
     */
    void registerDeviceToken(String userId, String token, String platform);
}
