package net.mcreator.hlsubtitles.hl2.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.mcreator.hlsubtitles.hl2.HL2Subtitles;
import net.mcreator.hlsubtitles.hl2.network.NetworkHandler;
import net.mcreator.hlsubtitles.hl2.network.SyncLangPacket;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = HL2Subtitles.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocalizationStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hl2_subtitles.json";

    private static String defaultLang = "en_us";
    private static Map<String, Map<String, String>> langs = new HashMap<>();

    private static File getFile(MinecraftServer server) { return server.getFile(FILE_NAME); }

    private static void load(MinecraftServer server) {
        try {
            File f = getFile(server);
            if (!f.exists()) { langs = new HashMap<>(); return; }
            try (FileReader r = new FileReader(f)) {
                JsonObject root = new JsonParser().parse(r).getAsJsonObject();
                if (root.has("default")) defaultLang = root.get("default").getAsString();
                langs = new HashMap<>();
                if (root.has("entries")) {
                    JsonObject entries = root.getAsJsonObject("entries");
                    for (Map.Entry<String, JsonElement> le : entries.entrySet()) {
                        JsonObject obj = le.getValue().getAsJsonObject();
                        Map<String, String> m = new HashMap<>();
                        for (Map.Entry<String, JsonElement> kv : obj.entrySet()) {
                            m.put(kv.getKey(), kv.getValue().getAsString());
                        }
                        langs.put(le.getKey(), m);
                    }
                }
            }
        } catch (Exception e) {
            HL2Subtitles.LOG.warn("Failed to load hl2_subtitles.json", e);
        }
    }

    private static void save(MinecraftServer server) {
        try {
            File f = getFile(server);
            try (FileWriter w = new FileWriter(f)) { GSON.toJson(buildJson(), w); }
        } catch (Exception e) {
            HL2Subtitles.LOG.warn("Failed to save hl2_subtitles.json", e);
        }
    }

    private static JsonObject buildJson() {
        JsonObject root = new JsonObject();
        root.addProperty("default", defaultLang);
        JsonObject entries = new JsonObject();
        for (Map.Entry<String, Map<String, String>> e : langs.entrySet()) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> kv : e.getValue().entrySet()) {
                obj.addProperty(kv.getKey(), kv.getValue());
            }
            entries.add(e.getKey(), obj);
        }
        root.add("entries", entries);
        return root;
    }

    public static void setEntry(MinecraftServer server, String lang, String key, String text) {
        load(server);
        Map<String, String> m = langs.get(lang);
        if (m == null) { m = new HashMap<>(); langs.put(lang, m); }
        m.put(key, text);
        save(server);
    }

    public static void setDefault(MinecraftServer server, String lang) {
        load(server); defaultLang = lang; save(server);
    }

    public static String toJson(MinecraftServer server) {
        load(server);
        return GSON.toJson(buildJson());
    }

    public static String resolveServerSide(String raw) { return raw; }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent ev) {
        if (!(ev.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity)) return;
        net.minecraft.entity.player.ServerPlayerEntity sp = (net.minecraft.entity.player.ServerPlayerEntity) ev.getPlayer();
        String json = toJson(sp.server);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncLangPacket(json));
    }
}
