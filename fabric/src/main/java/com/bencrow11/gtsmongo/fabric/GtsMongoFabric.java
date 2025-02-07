package com.bencrow11.gtsmongo.fabric;

import com.bencrow11.gtsmongo.GtsMongo;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class GtsMongoFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GtsMongo.init();

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GtsMongo.mongo.closeConnection();
        });
    }


}