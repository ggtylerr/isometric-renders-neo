package com.glisco.isometricrenders.exo.api;

import com.google.common.collect.ImmutableSet;
import com.glisco.isometricrenders.exo.impl.ExoServer;
import com.glisco.isometricrenders.exo.impl.ExoStateException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class Exo {

    public static final int DEFAULT_PORT = 6969;
    public static final String CHANNEL_DELIMITER = "@";
    public static final String ARGUMENT_DELIMITER = "#";
    public static final String OK_RESPONSE = "ok";
    public static final Logger LOGGER = LogManager.getLogger("exo");

    private static final Map<String, ExoChannel> CHANNELS = new HashMap<>();
    private static final Int2ObjectMap<ExoServer> SERVERS = new Int2ObjectOpenHashMap<>();

    public static void open(int port) {
        if (SERVERS.containsKey(port)) return;

        var server = new ExoServer(port);
        server.start();
        SERVERS.put(port, server);

        Exo.LOGGER.info("Exo channel opened at port {}", port);
    }

    public static boolean close(int port) {
        if (!SERVERS.containsKey(port)) return false;

        try {
            SERVERS.get(port).stop();
            SERVERS.remove(port);
            return true;
        } catch (InterruptedException e) {
            Exo.LOGGER.error("Could not close server on port {}", port, e);
            return false;
        }
    }

    public static void send(ExoChannel channel, int port, String message) {
        if (!SERVERS.containsKey(port)) throw new ExoStateException("Port '" + port + "' is not open");
        SERVERS.get(port).broadcast(channel.getId() + CHANNEL_DELIMITER + message);
    }

    public static void registerChannel(ExoChannel channel) {
        if (CHANNELS.containsKey(channel.getId())) {
            throw new ExoStateException("Attempted to double register channel '" + channel.getId() + "' for class '" + channel.getClass().getCanonicalName()
                    + "', already registered for class '" + CHANNELS.get(channel.getId()) + "'");
        }

        CHANNELS.put(channel.getId(), channel);
        Exo.LOGGER.info("Exo channel '{}' registered via class '{}'", channel.getId(), channel.getClass().getCanonicalName());
    }

    public static String join(String... args) {
        return String.join(Exo.ARGUMENT_DELIMITER, args);
    }

    public static ExoChannel getChannel(String id) {
        return CHANNELS.get(id);
    }

    public static boolean isOpen(int port) {
        return SERVERS.containsKey(port);
    }

    public static Collection<Integer> getOpenPorts() {
        return ImmutableSet.copyOf(SERVERS.keySet());
    }

    public static Collection<String> getChannelIdentifiers() {
        return ImmutableSet.copyOf(CHANNELS.keySet());
    }
}
