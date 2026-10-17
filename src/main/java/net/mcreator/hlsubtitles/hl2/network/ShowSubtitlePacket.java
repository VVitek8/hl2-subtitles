package net.mcreator.hlsubtitles.hl2.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class ShowSubtitlePacket {
    public final String text;
    public final int color;
    public final float typeSpeed;
    public final float duration;
    public final float delay;
    public final String soundId;
    public final float pitch;
    public final float volume;
    public final String soundMode;

    public ShowSubtitlePacket(String text, int color, float typeSpeed,
                              float duration, float delay,
                              String soundId, float pitch, float volume,
                              String soundMode) {
        this.text = text; this.color = color; this.typeSpeed = typeSpeed;
        this.duration = duration; this.delay = delay;
        this.soundId = soundId; this.pitch = pitch; this.volume = volume;
        this.soundMode = soundMode;
    }

    public static void encode(ShowSubtitlePacket p, PacketBuffer buf) {
        buf.writeString(p.text);
        buf.writeInt(p.color);
        buf.writeFloat(p.typeSpeed);
        buf.writeFloat(p.duration);
        buf.writeFloat(p.delay);
        buf.writeString(p.soundId);
        buf.writeFloat(p.pitch);
        buf.writeFloat(p.volume);
        buf.writeString(p.soundMode);
    }

    public static ShowSubtitlePacket decode(PacketBuffer buf) {
        return new ShowSubtitlePacket(
            buf.readString(), buf.readInt(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readString(),
            buf.readFloat(), buf.readFloat(), buf.readString());
    }

    public static void handle(ShowSubtitlePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> net.mcreator.hlsubtitles.hl2.client.ClientPacketHandler.handleShow(p)));
        ctx.get().setPacketHandled(true);
    }
}
