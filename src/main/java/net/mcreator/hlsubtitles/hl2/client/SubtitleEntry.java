package net.mcreator.hlsubtitles.hl2.client;

import java.util.List;

public class SubtitleEntry {
    public final String rawText;
    public final List<SubtitleScript.Step> steps;
    public final int argbColor;
    public final float holdDuration;
    public final float delay;
    public final String soundId;
    public final float pitch;
    public final float volume;
    public final String soundMode;

    public final long createdMs = System.currentTimeMillis();
    public long lastSoundMs = 0;
    public long nextStepTimeMs;
    public int stepIndex = 0;
    public String visibleRaw = "";
    public boolean typedDone = false;
    public long typedDoneMs = 0;
    public float fadeAlpha = 1f;
    public boolean startSoundPlayed = false;

    public SubtitleEntry(String rawText, List<SubtitleScript.Step> steps, int argbColor,
                         float holdDuration, float delay,
                         String soundId, float pitch, float volume,
                         String soundMode) {
        this.rawText = rawText;
        this.steps = steps;
        this.argbColor = argbColor;
        this.holdDuration = holdDuration;
        this.delay = delay;
        this.soundId = soundId;
        this.pitch = pitch;
        this.volume = volume;
        this.soundMode = soundMode == null ? "each" : soundMode;
        this.nextStepTimeMs = this.createdMs;
    }
}
