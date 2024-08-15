package com.bencrow11.gtsmongo.forge;

import com.bencrow11.gtsmongo.GtsMongo;
import net.neoforged.fml.common.Mod;

@Mod(GtsMongo.MOD_ID)
public class GtsMongoForge {
    public GtsMongoForge() {
        GtsMongo.init();
    }
}