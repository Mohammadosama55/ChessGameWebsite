package com.chessgame.service;

import com.chessgame.model.DBUtil;
import com.chessgame.model.Notification;
import com.chessgame.dao.NotificationDAO;
import com.google.gson.Gson;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/notifications")
public class NotificationService {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private final NotificationDAO notificationDAO;
    
    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }
    
    @OnOpen
    public void onOpen(Session session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.put(userId, session);
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle incoming messages if needed
    }
    
    public void sendNotification(String userId, Notification notification) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(gson.toJson(notification));
            } catch (IOException e) {
                // Handle send error
                sessions.remove(userId);
            }
        }
        
        // Store notification in database
        notificationDAO.saveNotification(notification);
    }
    
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationDAO.getUnreadNotifications(userId);
    }
    
    public void markAsRead(String userId, long notificationId) {
        notificationDAO.markAsRead(userId, notificationId);
    }
    
    public void markAllAsRead(String userId) {
        notificationDAO.markAllAsRead(userId);
    }
    
    private String getUserIdFromSession(Session session) {
        try {
            Map<String, String> pathParams = session.getPathParameters();
            return pathParams.get("userId");
        } catch (Exception e) {
            return null;
        }
    }
}
