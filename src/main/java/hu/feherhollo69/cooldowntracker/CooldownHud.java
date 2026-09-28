package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CooldownHud {
    private static final int W = 100;   // kártya szélesség
    private static final int H = 26;    // kártya magasság
    private static final int GAP = 4;   // kártyák közti távolság

    public static void render(DrawContext ctx, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || mc.options.hudHidden || mc.player == null) return;

        List<ModConfig.Entry> active = new ArrayList<>();
        for (ModConfig.Entry e : cfg.entries) {
            if (e.enabled && CooldownManager.isActive(e.id)) active.add(e);
        }
        if (active.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int perRow = Math.max(1, cfg.maxPerRow);
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        // A hotbar + életerő/éhség/XP sáv fölött
        int baseY = sh - 72 - cfg.yOffset;

        for (int i = 0; i < active.size(); i += perRow) {
            int inRow = Math.min(perRow, active.size() - i);
            int rowIndex = i / perRow;
            int rowW = inRow * W + (inRow - 1) * GAP;
            int startX = (sw - rowW) / 2;
            int y = baseY - (rowIndex + 1) * H - rowIndex * GAP;

            for (int j = 0; j < inRow; j++) {
                drawCard(ctx, tr, active.get(i + j), startX + j * (W + GAP), y);
            }
        }
    }

    private static void drawCard(DrawContext ctx, TextRenderer tr, ModConfig.Entry e, int x, int y) {
        long rem = CooldownManager.remainingMs(e.id);
        long total = Math.max(1, CooldownManager.totalMs(e.id));
        float frac = Math.min(1f, rem / (float) total);
        int accent = 0xFF000000 | e.color;

        // háttér + keret
        ctx.fill(x, y, x + W, y + H, 0xB0101018);
        ctx.fill(x, y, x + W, y + 1, accent);              // felső vékony akcent csík
        ctx.fill(x, y + 1, x + 1, y + H, 0x60FFFFFF);      // bal
        ctx.fill(x + W - 1, y + 1, x + W, y + H, 0x60FFFFFF); // jobb

        // ikon
        ctx.fill(x + 3, y + 4, x + 21, y + 22, 0x55000000);
        ctx.drawItem(getStack(e.item), x + 4, y + 5);

        // felirat + idő
        String label = tr.trimToWidth(e.label, W - 28);
        ctx.drawText(tr, label, x + 25, y + 5, accent, true);
        boolean urgent = rem < 3000;
        ctx.drawText(tr, formatTime(rem), x + 25, y + 15, urgent ? 0xFFFF5555 : 0xFFFFFFFF, true);

        // progress bar alul
        int barX1 = x + 1, barX2 = x + W - 1, barY1 = y + H - 3, barY2 = y + H - 1;
        ctx.fill(barX1, barY1, barX2, barY2, 0x80000000);
        int filled = (int) ((barX2 - barX1) * frac);
        ctx.fill(barX1, barY1, barX1 + filled, barY2, accent);
    }

    private static final java.util.Map<String, ItemStack> CACHE = new java.util.HashMap<>();

    private static ItemStack getStack(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            Identifier ident = Identifier.tryParse(k);
            Item item = ident == null ? net.minecraft.item.Items.BARRIER : Registries.ITEM.get(ident);
            return new ItemStack(item);
        });
    }

    private static String formatTime(long ms) {
        if (ms >= 60_000) {
            long s = (ms + 999) / 1000;
            return String.format("%d:%02d", s / 60, s % 60);
        }
        return String.format("%.1fs", ms / 1000.0);
    }
}
