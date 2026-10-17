package net.mcreator.hlsubtitles.hl2.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class ClientLangStorage {
    private static String defaultLang = "en_us";
    private static Map<String, Map<String, String>> langs = new HashMap<>();

    public static void load(String json) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (root.has("default")) defaultLang = root.get("default").getAsString();
            langs = new HashMap<>();
            if (root.has("entries")) {
                JsonObject entries = root.getAsJsonObject("entries");
                for (Map.Entry<String, JsonElement> le : entries.entrySet()) {
                    String langCode = le.getKey();
                    JsonObject obj = le.getValue().getAsJsonObject();
                    Map<String, String> m = new HashMap<>();
                    for (Map.Entry<String, JsonElement> kv : obj.entrySet()) {
                        m.put(kv.getKey(), kv.getValue().getAsString());
                    }
                    langs.put(langCode, m);
                }
            }
        } catch (Exception e) {
            net.mcreator.hlsubtitles.hl2.HL2Subtitles.LOG.warn("Failed to parse lang json", e);
        }
    }

    public static String resolve(String raw) {
        String key = null;
        if (raw.startsWith("{key:") && raw.endsWith("}")) key = raw.substring(5, raw.length() - 1);
        else if (raw.startsWith("@loc:")) key = raw.substring(5);
        if (key == null) return raw;

        String playerLang = "en_us";
        try { playerLang = Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode(); } catch (Exception ignored) {}

        String result = lookup(playerLang, key);
        if (result == null) result = lookup(defaultLang, key);
        return result != null ? result : raw;
    }

    private static String lookup(String lang, String key) {
        Map<String, String> m = langs.get(lang);
        return m != null ? m.get(key) : null;
    }
}
