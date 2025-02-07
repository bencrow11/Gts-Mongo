package com.bencrow11.gtsmongo.forge;

import com.bencrow11.gtsmongo.GtsMongo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(GtsMongo.MOD_ID)
public class GtsMongoForge {
    public GtsMongoForge() {
        GtsMongo.init();
    }

    @SubscribeEvent
    public void onServerStopped(final ServerStoppedEvent event) {
        GtsMongo.mongo.closeConnection();
    }
}