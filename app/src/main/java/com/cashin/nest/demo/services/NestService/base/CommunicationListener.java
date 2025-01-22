package com.cashin.nest.demo.services.NestService.base;

public interface CommunicationListener {
    void onConnected();
    void onDisconnected();
    void onMessageReceived(String message);
}