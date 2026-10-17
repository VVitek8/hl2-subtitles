package net.mcreator.hlsubtitles.hl2.client;

import net.minecraft.client.Minecraft;
import net.mcreator.hlsubtitles.hl2.network.ShowSubtitlePacket;

import java.util.List;

public class ClientPacketHandler {
    public static void handleShow(ShowSubtitlePacket p) {
        Minecraft.getInstance().execute(() -> {
            String resolved = ClientLangStorage.resolve(p.text);
            resolved = applyFormatCodes(resolved);
            List<SubtitleScript.Segment> segments = SubtitleScript.split(resolved, p.typeSpeed);

            float delay = p.delay;
            for (SubtitleScript.Segment s : segments) {
                int    color    = s.color    != null ? s.color    : p.color;
                String sound    = s.sound    != null ? s.sound    : p.soundId;
                float  pitch    = s.pitch    != null ? s.pitch    : p.pitch;
                float  volume   = s.volume   != null ? s.volume   : p.volume;
                float  duration = s.duration != null ? s.duration : p.duration;
                String mode     = s.soundMode != null ? s.soundMode : p.soundMode;

                if (s.extraDelay != null) delay += s.extraDelay;

                SubtitleManager.add(new SubtitleEntry(
                    s.rawText, s.steps, color, duration, delay,
                    sound, pitch, volume, mode));

                if (s.breakAfter >= 0f) {
                    delay += s.typingDuration + s.breakAfter;
                }
            }
        });
    }

    public static void handleClear() {
        Minecraft.getInstance().execute(SubtitleManager::clear);
    }

    public static void handleSyncLang(String json) {
        Minecraft.getInstance().execute(() -> ClientLangStorage.load(json));
    }

    private static String applyFormatCodes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                char n = Character.toLowerCase(s.charAt(i + 1));
                if (n == '&') { sb.append('&'); i++; continue; }
                if ("0123456789abcdefklmnor".indexOf(n) >= 0) {
                    sb.append('\u00a7').append(n); i++; continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
