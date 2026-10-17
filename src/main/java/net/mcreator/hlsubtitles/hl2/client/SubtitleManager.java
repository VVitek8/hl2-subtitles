package net.mcreator.hlsubtitles.hl2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

import java.util.ArrayList;
import java.util.List;

public class SubtitleManager {
    public static final int MAX_VISIBLE = 4;
    public static final float FADE_OUT_SEC = 1.0f;
    public static final long MIN_SOUND_INTERVAL_MS = 30;

    public static final List<SubtitleEntry> pending = new ArrayList<>();
    public static final List<SubtitleEntry> active = new ArrayList<>();

    public static void add(SubtitleEntry e) { pending.add(e); }

    public static void clear() { pending.clear(); active.clear(); }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { pending.clear(); active.clear(); return; }
        long now = System.currentTimeMillis();

        // pending -> active
        for (int i = pending.size() - 1; i >= 0; i--) {
            SubtitleEntry e = pending.get(i);
            if (now >= e.createdMs + (long)(e.delay * 1000L)) {
                pending.remove(i);
                e.nextStepTimeMs = now;
                active.add(e);
                if (active.size() > MAX_VISIBLE) active.remove(0);

                if ("once".equals(e.soundMode) && !e.startSoundPlayed) {
                    playSound(e);
                    e.startSoundPlayed = true;
                }
            }
        }

        for (int i = active.size() - 1; i >= 0; i--) {
            SubtitleEntry e = active.get(i);
            e.fadeAlpha = 1f;

            boolean playEach = "each".equals(e.soundMode);

            if (!e.typedDone) {
                long scheduled = e.nextStepTimeMs;
                while (e.stepIndex < e.steps.size()) {
                    if (now < scheduled) break;
                    SubtitleScript.Step s = e.steps.get(e.stepIndex);
                    e.visibleRaw += s.append;
                    e.stepIndex++;
                    scheduled += (long)(s.delayAfter * 1000);
                    if (playEach && s.visible && now - e.lastSoundMs >= MIN_SOUND_INTERVAL_MS) {
                        playSound(e);
                        e.lastSoundMs = now;
                    }
                }
                e.nextStepTimeMs = scheduled;
                if (e.stepIndex >= e.steps.size()) {
                    e.typedDone = true;
                    e.typedDoneMs = now;
                }
            }

            if (e.typedDone) {
                long holdEnd = e.typedDoneMs + (long)(e.holdDuration * 1000L);
                if (now > holdEnd) {
                    float fade = 1f - ((now - holdEnd) / (FADE_OUT_SEC * 1000f));
                    if (fade <= 0f) { active.remove(i); continue; }
                    e.fadeAlpha = fade;
                }
            }
        }
    }

    private static void playSound(SubtitleEntry e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSoundHandler() == null) return;
        if (e.soundId == null || e.soundId.isEmpty()) return;
        try {
            ResourceLocation rl = new ResourceLocation(e.soundId);
            ISound sound = new SimpleSound(
                rl, SoundCategory.VOICE, e.volume, e.pitch,
                false, 0, ISound.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
            mc.getSoundHandler().play(sound);
        } catch (Exception ignored) {}
    }
}
