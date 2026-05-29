package com.waimai.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waimai.common.dto.WsMessage;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/{role}/{userId}")
public class WebSocketServer {

    private static final Map<String, Map<String, Session>> ROOM_MAP = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("role") String role, @PathParam("userId") String userId) {
        String roomKey = role + ":" + userId;
        ROOM_MAP.computeIfAbsent(roomKey, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        log.info("WS连接: role={}, userId={}, sessionId={}", role, userId, session.getId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("role") String role, @PathParam("userId") String userId) {
        String roomKey = role + ":" + userId;
        Map<String, Session> sessions = ROOM_MAP.get(roomKey);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) ROOM_MAP.remove(roomKey);
        }
        log.info("WS断开: role={}, userId={}, sessionId={}", role, userId, session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WS异常: sessionId={}", session.getId(), error);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("role") String role) {
        log.debug("WS收到消息: role={}, content={}", role, message);
    }

    public static void sendToUser(Long userId, WsMessage msg) {
        send("user", String.valueOf(userId), msg);
    }

    public static void sendToMerchant(Long merchantId, WsMessage msg) {
        send("merchant", String.valueOf(merchantId), msg);
    }

    public static void sendToRider(Long riderId, WsMessage msg) {
        send("rider", String.valueOf(riderId), msg);
    }

    private static void send(String role, String userId, WsMessage msg) {
        Map<String, Session> sessions = ROOM_MAP.get(role + ":" + userId);
        if (sessions == null || sessions.isEmpty()) return;

        String json;
        try {
            json = MAPPER.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("WS消息序列化失败", e);
            return;
        }

        for (Session session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.error("WS发送失败: userId={}", userId, e);
                }
            }
        }
    }
}
