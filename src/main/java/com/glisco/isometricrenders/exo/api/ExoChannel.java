package com.glisco.isometricrenders.exo.api;

public interface ExoChannel extends ExoEntrypoint {

    String getId();

    default void send(String message, int port) {
        Exo.send(this, port, message);
    }

    default String receive(String message, int port) {
        handleMessage(message, port);
        return Exo.OK_RESPONSE;
    }

    default void init() {
        Exo.registerChannel(this);
    }

    default void handleMessage(String message, int port) {}
}
