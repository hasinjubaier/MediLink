package com.medilink.service;

import com.medilink.model.observer.Notifiable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service managing stock update notifications using the Observer Pattern.
 * Bridges backend stock changes with active client listeners.
 */
public class StockObserverService implements Notifiable {
    private static StockObserverService instance;
    private final String observerId = "SYS_STOCK_OBSERVER_SERVICE";
    private final List<Consumer<String>> eventListeners = new CopyOnWriteArrayList<>();
    private final List<String> recentStockEventLogs = Collections.synchronizedList(new ArrayList<>());

    private StockObserverService() {}

    public static synchronized StockObserverService getInstance() {
        if (instance == null) {
            instance = new StockObserverService();
        }
        return instance;
    }

    public void addEventListener(Consumer<String> listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(Consumer<String> listener) {
        eventListeners.remove(listener);
    }

    @Override
    public void onNotification(String eventType, Object payload) {
        String message = "[" + eventType + "] " + (payload != null ? payload.toString() : "");
        recentStockEventLogs.add(0, message);
        if (recentStockEventLogs.size() > 50) {
            recentStockEventLogs.remove(recentStockEventLogs.size() - 1);
        }

        // Dispatch to all connected client stream consumers
        for (Consumer<String> listener : eventListeners) {
            try {
                listener.accept(message);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public List<String> getRecentEvents() {
        return new ArrayList<>(recentStockEventLogs);
    }
}
