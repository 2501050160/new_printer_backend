package com.saipraveen.login_registration.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    // Thread-safe list of active client emitters
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Register a new client for Server-Sent Events stream
     */
    public SseEmitter registerClient() {
        // 30 minute timeout for emitter
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError((e) -> {
            emitter.complete();
            emitters.remove(emitter);
        });

        // Send initial connection ACK
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "SSE Stream Connected Successfully", "timestamp", System.currentTimeMillis()), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Broadcast an event and payload to all connected clients
     */
    public void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }

    /**
     * Broadcast an order change event
     */
    public void broadcastOrderEvent(String orderId, String status) {
        broadcast("ORDER_UPDATED", Map.of(
                "orderId", orderId != null ? orderId : "",
                "status", status != null ? status : "",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Broadcast queue modification event
     */
    public void broadcastQueueEvent(String message) {
        broadcastQueueEvent(message, "");
    }

    /**
     * Broadcast queue modification event with target block location
     */
    public void broadcastQueueEvent(String message, String blockLocation) {
        broadcast("QUEUE_UPDATED", Map.of(
                "message", message != null ? message : "Queue Modified",
                "blockLocation", blockLocation != null ? blockLocation : "",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Heartbeat keep-alive every 25 seconds to keep browser / proxy connections open
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PING")
                        .data(Map.of("heartbeat", true), MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
