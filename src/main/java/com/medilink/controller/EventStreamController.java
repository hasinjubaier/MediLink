package com.medilink.controller;

import com.medilink.service.StockObserverService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventStreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        StockObserverService.getInstance().addEventListener(this::broadcast);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        // Emitter timeout: 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Initial welcome event
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("[SYSTEM_CONNECTED] Real-time MediLink Observer Event Stream Online."));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    private void broadcast(String message) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(message));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
