package net.mcreator.hlsubtitles.hl2.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class SyncLangPacket {
    public final String json;
    public SyncLangPacket(String json) { this.json = json; }

    public static void encode(SyncLangPacket p, PacketBuffer buf) {
        buf.writeString(p.json, 262144);
    }
    public static SyncLangPacket decode(PacketBuffer buf) {
        return new SyncLangPacket(buf.readString(262144));
    }
    public static void handle(SyncLangPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.mcreator.hlsubtitles.hl2.client.ClientPacketHandler.handleSyncLang(p.json)));
        ctx.get().setPacketHandled(true);
    }
}
