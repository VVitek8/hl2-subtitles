package net.mcreator.hlsubtitles.hl2.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class ClearSubtitlesPacket {
    public ClearSubtitlesPacket() {}
    public static void encode(ClearSubtitlesPacket p, PacketBuffer buf) {}
    public static ClearSubtitlesPacket decode(PacketBuffer buf) { return new ClearSubtitlesPacket(); }
    public static void handle(ClearSubtitlesPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.mcreator.hlsubtitles.hl2.client.ClientPacketHandler.handleClear()));
        ctx.get().setPacketHandled(true);
    }
}
