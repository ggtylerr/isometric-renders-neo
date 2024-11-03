package com.glisco.isometricrenders.exo.impl;

import com.glisco.isometricrenders.exo.api.Exo;
import com.glisco.isometricrenders.exo.api.ExoChannel;

import java.util.stream.Collectors;

public class ExoDefaultChannel implements ExoChannel {

    @Override
    public String receive(String message, int port) {
        if (message.equals("list-channels")) {
            return String.join(",", Exo.getChannelIdentifiers());
        } else if (message.equals("list-ports")) {
            return Exo.getOpenPorts().stream().map(Object::toString).collect(Collectors.joining(","));
        } else {
            return "ok";
        }
    }

    @Override
    public String getId() {
        return "exo";
    }
}
