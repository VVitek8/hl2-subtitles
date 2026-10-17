package net.mcreator.hlsubtitles.hl2.client;

import java.util.ArrayList;
import java.util.List;

public class SubtitleScript {
    public static class Step {
        public final String append;
        public final float delayAfter;
        public final boolean visible;
        public Step(String append, float delayAfter, boolean visible) {
            this.append = append; this.delayAfter = delayAfter; this.visible = visible;
        }
    }

    public static class Segment {
        public List<Step> steps = new ArrayList<>();
        public String rawText = "";
        public float typingDuration = 0f;
        public float breakAfter = -1f; // -1 = последний

        public Integer color = null;
        public String sound = null;
        public Float pitch = null;
        public Float volume = null;
        public Float typeSpeed = null;
        public Float duration = null;
        public Float extraDelay = null;
        public String soundMode = null;
    }

    private static class RawTok {
        final String append;
        final boolean visible;
        final boolean instant;
        final float explicitPause;
        RawTok(String append, boolean visible, boolean instant, float explicitPause) {
            this.append = append; this.visible = visible; this.instant = instant; this.explicitPause = explicitPause;
        }
    }

    public static List<Segment> split(String raw, float baseTypeSpeed) {
        List<Segment> result = new ArrayList<>();
        List<RawTok> toks = new ArrayList<>();
        StringBuilder rawText = new StringBuilder();
        Segment cur = new Segment();
        boolean instant = false;

        int i = 0, n = raw.length();
        while (i < n) {
            if (raw.startsWith("{fast}", i)) { instant = true; i += 6; continue; }
            if (raw.startsWith("{/fast}", i)) { instant = false; i += 7; continue; }

            if (raw.startsWith("{cfg:", i)) {
                int close = raw.indexOf('}', i + 5);
                if (close > 0) {
                    parseCfg(raw.substring(i + 5, close), cur);
                    i = close + 1;
                    continue;
                }
            }

            if (raw.startsWith("{break", i)) {
                int close = raw.indexOf('}', i + 6);
                if (close > 0) {
                    String arg = raw.substring(i + 6, close).trim();
                    float bd = 0f;
                    if (arg.startsWith(":")) {
                        try { bd = Float.parseFloat(arg.substring(1).trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    result.add(finalize(cur, toks, rawText.toString(), bd, baseTypeSpeed));
                    toks = new ArrayList<>();
                    rawText = new StringBuilder();
                    cur = new Segment();
                    instant = false;
                    i = close + 1;
                    continue;
                }
            }

            if (raw.startsWith("{p:", i)) {
                int close = raw.indexOf('}', i + 3);
                if (close > 0) {
                    try {
                        float v = Float.parseFloat(raw.substring(i + 3, close).trim());
                        toks.add(new RawTok("", false, false, v));
                        i = close + 1;
                        continue;
                    } catch (NumberFormatException ignored) {}
                }
            }

            char c = raw.charAt(i);
            if (c == '\u00a7' && i + 1 < n) {
                String pair = raw.substring(i, i + 2);
                toks.add(new RawTok(pair, false, false, -1f));
                rawText.append(pair);
                i += 2;
                continue;
            }
            String ch = String.valueOf(c);
            toks.add(new RawTok(ch, true, instant, -1f));
            rawText.append(ch);
            i++;
        }

        result.add(finalize(cur, toks, rawText.toString(), -1f, baseTypeSpeed));
        return result;
    }

    private static Segment finalize(Segment s, List<RawTok> toks, String rawText,
                                    float breakAfter, float baseTypeSpeed) {
        float typeSpeed = s.typeSpeed != null ? s.typeSpeed : baseTypeSpeed;
        Segment out = new Segment();
        out.rawText = rawText;
        out.breakAfter = breakAfter;
        out.color = s.color;
        out.sound = s.sound;
        out.pitch = s.pitch;
        out.volume = s.volume;
        out.typeSpeed = s.typeSpeed;
        out.duration = s.duration;
        out.extraDelay = s.extraDelay;
        out.soundMode = s.soundMode;

        float total = 0f;
        for (RawTok t : toks) {
            float d;
            if (t.explicitPause >= 0f) d = t.explicitPause;
            else if (t.visible) d = t.instant ? 0f : typeSpeed;
            else d = 0f;
            out.steps.add(new Step(t.append, d, t.visible));
            total += d;
        }
        out.typingDuration = total;
        return out;
    }

    private static void parseCfg(String body, Segment seg) {
        for (String pair : body.split(",")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = pair.substring(0, eq).trim().toLowerCase();
            String val = pair.substring(eq + 1).trim();
            try {
                switch (key) {
                    case "color":  seg.color = parseColor(val); break;
                    case "sound":  seg.sound = val; break;
                    case "pitch":  seg.pitch = Float.parseFloat(val); break;
                    case "volume": seg.volume = Float.parseFloat(val); break;
                    case "speed":  seg.typeSpeed = Float.parseFloat(val); break;
                    case "dur": case "duration": seg.duration = Float.parseFloat(val); break;
                    case "delay":  seg.extraDelay = Float.parseFloat(val); break;
                    case "mode":   seg.soundMode = val.toLowerCase(); break;
                }
            } catch (Exception ignored) {}
        }
    }

    private static int parseColor(String s) {
        s = s.trim();
        if (s.startsWith("#")) return 0xFF000000 | Integer.parseInt(s.substring(1), 16);
        if (s.matches("[0-9a-fA-F]{6}")) return 0xFF000000 | Integer.parseInt(s, 16);
        if (s.startsWith("0x") || s.startsWith("0X")) return (int) Long.parseLong(s.substring(2), 16);
        switch (s.toLowerCase()) {
            case "black": return 0x000000;
            case "white": return 0xFFFFFF;
            case "red": return 0xFF5555;
            case "green": return 0x55FF55;
            case "blue": return 0x5555FF;
            case "yellow": return 0xFFFF55;
            case "gold": return 0xFFAA00;
            case "aqua": return 0x55FFFF;
            case "gray": case "grey": return 0xAAAAAA;
            case "dark_gray": case "dark_grey": return 0x555555;
            case "dark_red": return 0xAA0000;
            case "dark_green": return 0x00AA00;
            case "dark_blue": return 0x0000AA;
            case "dark_aqua": return 0x00AAAA;
            case "dark_purple": return 0xAA00AA;
            case "light_purple": return 0xFF55FF;
            default:
                try { return Integer.parseInt(s, 16); } catch (Exception e) { return 0xFFFFFF; }
        }
    }
}
