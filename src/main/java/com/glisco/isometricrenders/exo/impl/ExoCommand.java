package com.glisco.isometricrenders.exo.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.glisco.isometricrenders.exo.api.Exo;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ExoCommand {

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("exo")
                    .then(literal("list").executes(ExoCommand::executeList))
                    .then(literal("open").executes(ExoCommand::executeOpen)
                            .then(argument("port", IntegerArgumentType.integer(0, 65535)).executes(ExoCommand::executeOpenPort)))
                    .then(literal("close").executes(ExoCommand::executeClose)
                            .then(argument("port", IntegerArgumentType.integer(0, 65535)).executes(ExoCommand::executeClosePort))
                            .then(literal("all").executes(ExoCommand::closeAll))));
        });
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        final var openPorts = Exo.getOpenPorts();
        if (openPorts.size() != 0) {
            context.getSource().sendFeedback(prefix("open ports: §d" +
                    openPorts.stream().map(Object::toString).collect(Collectors.joining(", "))));
        } else {
            context.getSource().sendFeedback(prefix("no open ports"));
        }
        return 0;
    }

    private static int executeOpenPort(CommandContext<FabricClientCommandSource> context) {
        final int port = IntegerArgumentType.getInteger(context, "port");
        return open(context, port);
    }

    private static int executeOpen(CommandContext<FabricClientCommandSource> context) {
        return open(context, Exo.DEFAULT_PORT);
    }

    private static int open(CommandContext<FabricClientCommandSource> context, int port) {
        if (Exo.isOpen(port)) {
            context.getSource().sendFeedback(prefix("port §d" + port + " §7 is already open"));
            return 0;
        }

        Exo.open(port);
        context.getSource().sendFeedback(prefix("opening on port §d" + port));
        return 0;
    }

    private static int executeClosePort(CommandContext<FabricClientCommandSource> context) {
        final int port = IntegerArgumentType.getInteger(context, "port");
        return close(context, port);
    }

    private static int executeClose(CommandContext<FabricClientCommandSource> context) {
        return close(context, Exo.DEFAULT_PORT);
    }

    private static int closeAll(CommandContext<FabricClientCommandSource> context) {
        Exo.getOpenPorts().forEach(port -> close(context, port));
        return 0;
    }

    private static int close(CommandContext<FabricClientCommandSource> context, int port) {
        if (!Exo.isOpen(port)) {
            context.getSource().sendFeedback(prefix("not running on port §d" + port));
            return 0;
        }

        final boolean closed = Exo.close(port);
        if (closed) {
            context.getSource().sendFeedback(prefix("successfully closed on port §d" + port));
        } else {
            context.getSource().sendFeedback(prefix("could not close exo on port §d" + port + "§7. check your log for details"));
        }
        return 0;
    }

    static Text prefix(String message) {
        return Text.literal("§9exo > §7" + message);
    }

}
