package com.bencrow11.gtsmongo.forge;

import com.bencrow11.gtsmongo.GtsMongo;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GtsMongo.MOD_ID)
public class GtsMongoForge {
    public GtsMongoForge() {
        GtsMongo.init();
    }
}