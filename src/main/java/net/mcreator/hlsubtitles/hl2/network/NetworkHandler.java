package net.mcreator.hlsubtitles.hl2.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.mcreator.hlsubtitles.hl2.HL2Subtitles;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HL2Subtitles.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
        );
        int id = 0;
        CHANNEL.registerMessage(id++, ShowSubtitlePacket.class,
            ShowSubtitlePacket::encode, ShowSubtitlePacket::decode, ShowSubtitlePacket::handle);
        CHANNEL.registerMessage(id++, ClearSubtitlesPacket.class,
            ClearSubtitlesPacket::encode, ClearSubtitlesPacket::decode, ClearSubtitlesPacket::handle);
        CHANNEL.registerMessage(id++, SyncLangPacket.class,
            SyncLangPacket::encode, SyncLangPacket::decode, SyncLangPacket::handle);
    }
}
