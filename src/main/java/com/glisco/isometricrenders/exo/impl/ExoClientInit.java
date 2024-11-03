package com.glisco.isometricrenders.exo.impl;

import com.glisco.isometricrenders.exo.api.Exo;
import com.glisco.isometricrenders.exo.api.ExoEntrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ExoClientInit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Exo.registerChannel(new ExoDefaultChannel());
        FabricLoader.getInstance().getEntrypoints("exo", ExoEntrypoint.class).forEach(ExoEntrypoint::init);

        ExoCommand.register();
    }
}
