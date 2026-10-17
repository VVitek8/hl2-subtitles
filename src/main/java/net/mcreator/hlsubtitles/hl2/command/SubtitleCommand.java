package net.mcreator.hlsubtitles.hl2.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.mcreator.hlsubtitles.hl2.HL2Subtitles;
import net.mcreator.hlsubtitles.hl2.network.NetworkHandler;
import net.mcreator.hlsubtitles.hl2.network.ShowSubtitlePacket;
import net.mcreator.hlsubtitles.hl2.network.ClearSubtitlesPacket;
import net.mcreator.hlsubtitles.hl2.network.SyncLangPacket;
import net.mcreator.hlsubtitles.hl2.world.LocalizationStorage;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = HL2Subtitles.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SubtitleCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent ev) {
        CommandDispatcher<CommandSource> d = ev.getDispatcher();

        // /send targets text [typeSpeed duration delay color sound pitch volume [mode]]
        ArgumentBuilder<CommandSource, ?> soundModeArg = Commands.argument("soundMode", StringArgumentType.word())
            .executes(SubtitleCommand::sendFullMode);

        ArgumentBuilder<CommandSource, ?> volumeArg = Commands.argument("volume", FloatArgumentType.floatArg(0f, 2f))
            .executes(SubtitleCommand::sendFull)
            .then(soundModeArg);
        ArgumentBuilder<CommandSource, ?> pitchArg = Commands.argument("pitch", FloatArgumentType.floatArg(0.1f, 4f)).then(volumeArg);
        ArgumentBuilder<CommandSource, ?> soundArg = Commands.argument("sound", ResourceLocationArgument.resourceLocation()).then(pitchArg);
        ArgumentBuilder<CommandSource, ?> colorArg = Commands.argument("color", StringArgumentType.string()).then(soundArg);
        ArgumentBuilder<CommandSource, ?> delayArg = Commands.argument("delay", FloatArgumentType.floatArg(0f, 60f)).then(colorArg);
        ArgumentBuilder<CommandSource, ?> durationArg = Commands.argument("duration", FloatArgumentType.floatArg(0.1f, 60f)).then(delayArg);
        ArgumentBuilder<CommandSource, ?> typeSpeedArg = Commands.argument("typeSpeed", FloatArgumentType.floatArg(0f, 2f)).then(durationArg);
        ArgumentBuilder<CommandSource, ?> textArg = Commands.argument("text", StringArgumentType.string())
            .executes(ctx -> send(ctx, 0.05f, 3f, 0f, 0xFFFFFF, "minecraft:ui.button.click", 1.8f, 0.15f, "each"))
            .then(typeSpeedArg);

        LiteralArgumentBuilder<CommandSource> sendCmd = Commands.literal("send")
            .then(Commands.argument("targets", EntityArgument.players()).then(textArg));

        LiteralArgumentBuilder<CommandSource> clearCmd = Commands.literal("clear")
            .then(Commands.argument("targets", EntityArgument.players()).executes(SubtitleCommand::clear));

        LiteralArgumentBuilder<CommandSource> langSet = Commands.literal("set")
            .then(Commands.argument("key", StringArgumentType.string())
                .then(Commands.argument("lang", StringArgumentType.string())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(SubtitleCommand::langSet))));
        LiteralArgumentBuilder<CommandSource> langDefault = Commands.literal("default")
            .then(Commands.argument("lang", StringArgumentType.string())
                .executes(SubtitleCommand::langDefault));
        LiteralArgumentBuilder<CommandSource> langCmd = Commands.literal("lang").then(langSet).then(langDefault);

        d.register(Commands.literal("hl2_subtitles")
            .requires(s -> s.hasPermissionLevel(2))
            .then(sendCmd)
            .then(clearCmd)
            .then(langCmd));
    }

    private static int sendFull(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        return send(ctx,
            FloatArgumentType.getFloat(ctx, "typeSpeed"),
            FloatArgumentType.getFloat(ctx, "duration"),
            FloatArgumentType.getFloat(ctx, "delay"),
            parseColor(StringArgumentType.getString(ctx, "color")),
            ResourceLocationArgument.getResourceLocation(ctx, "sound").toString(),
            FloatArgumentType.getFloat(ctx, "pitch"),
            FloatArgumentType.getFloat(ctx, "volume"),
            "each");
    }

    private static int sendFullMode(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        String mode = StringArgumentType.getString(ctx, "soundMode").toLowerCase();
        if (!mode.equals("each") && !mode.equals("once") && !mode.equals("off")) mode = "each";
        return send(ctx,
            FloatArgumentType.getFloat(ctx, "typeSpeed"),
            FloatArgumentType.getFloat(ctx, "duration"),
            FloatArgumentType.getFloat(ctx, "delay"),
            parseColor(StringArgumentType.getString(ctx, "color")),
            ResourceLocationArgument.getResourceLocation(ctx, "sound").toString(),
            FloatArgumentType.getFloat(ctx, "pitch"),
            FloatArgumentType.getFloat(ctx, "volume"),
            mode);
    }

    private static int send(CommandContext<CommandSource> ctx, float typeSpeed, float duration,
                            float delay, int color, String sound, float pitch, float volume,
                            String soundMode)
            throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
        String raw = StringArgumentType.getString(ctx, "text");
        String resolved = applyFormatCodes(LocalizationStorage.resolveServerSide(raw));

        HL2Subtitles.LOG.info("[HL2] {} -> {} (color=#{}, speed={}, dur={}, delay={}, sound={}, mode={})",
            ctx.getSource().getName(), resolved, Integer.toHexString(color & 0xFFFFFF),
            typeSpeed, duration, delay, sound, soundMode);

        ShowSubtitlePacket p = new ShowSubtitlePacket(resolved, color, typeSpeed, duration, delay, sound, pitch, volume, soundMode);
        for (ServerPlayerEntity pl : targets) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> pl), p);
        }
        return targets.size();
    }

    private static int clear(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        for (ServerPlayerEntity pl : EntityArgument.getPlayers(ctx, "targets")) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> pl), new ClearSubtitlesPacket());
        }
        return 1;
    }

    private static int langSet(CommandContext<CommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String lang = StringArgumentType.getString(ctx, "lang");
        String text = StringArgumentType.getString(ctx, "text");
        LocalizationStorage.setEntry(ctx.getSource().getServer(), lang, key, text);
        ctx.getSource().sendFeedback(new StringTextComponent("\u00a7aOK: [" + lang + "] " + key), true);
        broadcastLang(ctx);
        return 1;
    }

    private static int langDefault(CommandContext<CommandSource> ctx) {
        String lang = StringArgumentType.getString(ctx, "lang");
        LocalizationStorage.setDefault(ctx.getSource().getServer(), lang);
        ctx.getSource().sendFeedback(new StringTextComponent("\u00a7aDefault lang: " + lang), true);
        broadcastLang(ctx);
        return 1;
    }

    private static void broadcastLang(CommandContext<CommandSource> ctx) {
        String json = LocalizationStorage.toJson(ctx.getSource().getServer());
        SyncLangPacket p = new SyncLangPacket(json);
        for (ServerPlayerEntity pl : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> pl), p);
        }
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
