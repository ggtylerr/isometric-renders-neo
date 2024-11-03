package com.glisco.isometricrenders.exo.api;

import com.glisco.isometricrenders.exo.impl.ExoStateException;

import java.util.HashMap;
import java.util.Map;

public abstract class ExoCommandChannel implements ExoChannel {

    private final Map<String, Command> COMMANDS = new HashMap<>();

    protected void addCommand(String name, Command handler) {
        if (COMMANDS.containsKey(name)) throw new ExoStateException("Attempted to register command '" + name + "' twice for channel '" + getId() + "'");
        this.COMMANDS.put(name, handler);
    }

    @Override
    public String receive(String message, int port) {
        return message.contains(Exo.ARGUMENT_DELIMITER) ?
                handleCommand(port, message.substring(0, message.indexOf(Exo.ARGUMENT_DELIMITER)),
                        message.substring(message.indexOf(Exo.ARGUMENT_DELIMITER) + 1).split(Exo.ARGUMENT_DELIMITER))
                : handleCommand(port, message);
    }

    private String handleCommand(int port, String name, String... args) {
        if (COMMANDS.containsKey(name)) {
            return COMMANDS.get(name).execute(port, args);
        } else {
            return "unknown command";
        }
    }

    @FunctionalInterface
    public interface Command {
        String execute(int port, String[] arguments);
    }
}
