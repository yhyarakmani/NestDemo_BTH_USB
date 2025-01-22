package com.cashin.nest.demo.services.NestService.base;

public interface CommunicationService {
    void connect();
    void disconnect();
    void send(String data);
    void setListener(CommunicationListener listener);
}