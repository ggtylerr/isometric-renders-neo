package com.glisco.isometricrenders.exo.impl;

import com.glisco.isometricrenders.exo.api.Exo;
import com.glisco.isometricrenders.exo.api.ExoChannel;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.BindException;
import java.net.InetSocketAddress;
import net.minecraft.client.MinecraftClient;

public class ExoServer extends WebSocketServer {

    public ExoServer(int port) {
        super(new InetSocketAddress("localhost", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send(exo("channels" + Exo.ARGUMENT_DELIMITER + String.join(",", Exo.getChannelIdentifiers())));
        Exo.LOGGER.info("Exo client connected from '{}'", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Exo.LOGGER.info("Exo client disconnected from '{}'", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        final int delimiterIndex = message.indexOf(Exo.CHANNEL_DELIMITER);
        if (delimiterIndex > -1) {
            var channelId = message.substring(0, delimiterIndex);
            final var channel = Exo.getChannel(channelId);
            if (channel != null) {
                var sanitizedMessage = message.substring(delimiterIndex + 1);

                if (sanitizedMessage.endsWith("\n")) {
                    sanitizedMessage = sanitizedMessage.substring(0, sanitizedMessage.length() - 1);
                }

                final int port = getPort();
                Exo.send(channel, port, channel.receive(sanitizedMessage, port));
            } else {
                conn.send("exo" + Exo.CHANNEL_DELIMITER + "unknown channel");
            }
        } else {
            conn.send("exo" + Exo.CHANNEL_DELIMITER + "missing channel delimiter");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            Exo.LOGGER.error("Exo server encountered an exception on connection '{}'", conn.getLocalSocketAddress(), ex);
        } else if (ex instanceof BindException) {
            MinecraftClient.getInstance().player.sendMessage(ExoCommand.prefix("§ccould not bind to port"), false);
            Exo.close(getPort());
        }
    }

    @Override
    public void onStart() {
        MinecraftClient.getInstance().player.sendMessage(ExoCommand.prefix("successfully opened"), false);
    }

    private String exo(String msg) {
        return "exo" + Exo.CHANNEL_DELIMITER + msg;
    }
}
