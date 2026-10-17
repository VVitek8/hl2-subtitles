package net.mcreator.hlsubtitles.hl2;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.hlsubtitles.hl2.network.NetworkHandler;

@Mod.EventBusSubscriber(modid = HL2Subtitles.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HL2Setup {
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(NetworkHandler::register);
    }
}
