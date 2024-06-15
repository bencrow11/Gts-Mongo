package com.bencrow11.gtsmongo.fabric;

import com.bencrow11.gtsmongo.GtsMongo;
import net.fabricmc.api.ModInitializer;

public class GtsMongoFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GtsMongo.init();
    }
}