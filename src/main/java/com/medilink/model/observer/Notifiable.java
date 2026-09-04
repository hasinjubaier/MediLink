package com.medilink.model.observer;

/**
 * Observer interface in the Observer Design Pattern.
 */
public interface Notifiable {
    void onNotification(String eventType, Object payload);
    String getObserverId();
}
