package net.mcreator.hlsubtitles.hl2.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.hlsubtitles.hl2.HL2Subtitles;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = HL2Subtitles.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SubtitleRenderer {
    private static final int BG_BASE_ALPHA = 0xB0;
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 3;
    private static final int LINE_SPACING = 2;
    private static final int HOTBAR_OFFSET = 55;

    @SubscribeEvent
    public static void onRender(RenderGameOverlayEvent.Post ev) {
        if (ev.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameSettings.hideGUI) return;

        SubtitleManager.tick();
        if (SubtitleManager.active.isEmpty()) return;

        MatrixStack ms = ev.getMatrixStack();
        FontRenderer fr = mc.fontRenderer;
        int screenW = mc.getMainWindow().getScaledWidth();
        int screenH = mc.getMainWindow().getScaledHeight();

        int maxWidth = Math.max(40, screenW / 3);
        int baseY = screenH - HOTBAR_OFFSET;
        int minY = screenH / 2;

        // Идём с конца (самый новый) — он рисуется внизу,
        // предыдущие уезжают выше. Именно поэтому новые всегда снизу.
        int y = baseY;
        for (int i = SubtitleManager.active.size() - 1; i >= 0; i--) {
            SubtitleEntry e = SubtitleManager.active.get(i);
            String visible = e.visibleRaw;
            if (visible.isEmpty()) continue;

            List<String> lines = wrapText(fr, visible, maxWidth);

            int blockH = lines.size() * (fr.FONT_HEIGHT + LINE_SPACING) - LINE_SPACING;
            int blockW = 0;
            for (String l : lines) blockW = Math.max(blockW, fr.getStringWidth(l));

            int boxX = screenW / 2 - blockW / 2 - PADDING_X;
            int boxY = y - blockH - PADDING_Y;
            int boxW = blockW + PADDING_X * 2;
            int boxH = blockH + PADDING_Y * 2;

            if (boxY < minY) break;

            // Фон
            int bgAlpha = (int)(BG_BASE_ALPHA * e.fadeAlpha);
            AbstractGui.fill(ms, boxX, boxY, boxX + boxW, boxY + boxH, bgAlpha << 24);

            // Текст — с той же альфой, что и фон, чтобы fade-out был равномерным
            int textAlpha = (int)(0xFF * e.fadeAlpha);
            int textColor = (textAlpha << 24) | (e.argbColor & 0xFFFFFF);

            int textY = y - blockH;
            for (String l : lines) {
                int w = fr.getStringWidth(l);
                fr.drawStringWithShadow(ms, l, screenW / 2f - w / 2f, textY, textColor);
                textY += fr.FONT_HEIGHT + LINE_SPACING;
            }

            y = boxY - 4;
        }
    }

    private static List<String> wrapText(FontRenderer fr, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text.isEmpty()) { out.add(""); return out; }
        StringBuilder line = new StringBuilder();
        String[] words = text.split(" ", -1);
        for (int wi = 0; wi < words.length; wi++) {
            String word = words[wi];
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fr.getStringWidth(candidate) <= maxWidth || line.length() == 0) {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            } else {
                out.add(line.toString());
                line = new StringBuilder();
                line.append(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
