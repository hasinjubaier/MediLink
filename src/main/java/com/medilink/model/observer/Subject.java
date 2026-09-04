package com.medilink.model.observer;

/**
 * Subject interface in the Observer Design Pattern.
 */
public interface Subject {
    void registerObserver(Notifiable observer);
    void removeObserver(Notifiable observer);
    void notifyObservers(String eventType, Object payload);
}
