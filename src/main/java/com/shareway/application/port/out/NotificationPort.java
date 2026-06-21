package com.shareway.application.port.out;
public interface NotificationPort {
    void notify(String userId, String type, String title, String body);
    void notifyWithLink(String userId, String type, String title, String body, String link);
    void broadcast(String type, String title, String body);
}
