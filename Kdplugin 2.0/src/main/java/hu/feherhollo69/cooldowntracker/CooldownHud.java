package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CooldownHud {
    public static final int W = 100;   // kártya szélesség
    public static final int H = 26;    // kártya magasság
    public static final int GAP = 4;   // kártyák közti távolság

    /** Az el nem mozgatott kártyák automatikus elrendezése (a hotbar fölött, középen). */
    public static Map<ModConfig.Entry, int[]> autoLayout(List<ModConfig.Entry> list, int sw, int sh, ModConfig cfg) {
        Map<ModConfig.Entry, int[]> out = new HashMap<>();
        List<ModConfig.Entry> auto = new ArrayList<>();
        for (ModConfig.Entry e : list) if (!e.moved) auto.add(e);

        int perRow = Math.max(1, cfg.maxPerRow);
        int baseY = sh - 72 - cfg.yOffset; // a hotbar + életerő/éhség/XP sáv fölött

        for (int i = 0; i < auto.size(); i += perRow) {
            int inRow = Math.min(perRow, auto.size() - i);
            int rowIndex = i / perRow;
            int rowW = inRow * W + (inRow - 1) * GAP;
            int startX = (sw - rowW) / 2;
            int y = baseY - (rowIndex + 1) * H - rowIndex * GAP;
            for (int j = 0; j < inRow; j++) {
                out.put(auto.get(i + j), new int[]{startX + j * (W + GAP), y});
            }
        }
        return out;
    }

    /** Egy kártya végleges helye: kézzel mozgatott vagy automatikus. */
    public static int[] position(ModConfig.Entry e, Map<ModConfig.Entry, int[]> auto, int sw, int sh) {
        if (e.moved) {
            int x = Math.max(0, Math.min(sw / 2 + e.posX, Math.max(0, sw - W)));
            int y = Math.max(0, Math.min(sh + e.posY, Math.max(0, sh - H)));
            return new int[]{x, y};
        }
        int[] p = auto.get(e);
        return p != null ? p : new int[]{0, 0};
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || mc.options.hudHidden || mc.player == null) return;
        if (mc.currentScreen instanceof MoveScreen) return; // ott a MoveScreen rajzolja a kártyákat

        List<ModConfig.Entry> active = new ArrayList<>();
        for (ModConfig.Entry e : cfg.entries) {
            if (e.visible(cfg) && CooldownManager.isActive(e.id)) active.add(e);
        }
        if (active.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        Map<ModConfig.Entry, int[]> auto = autoLayout(active, sw, sh, cfg);

        for (ModConfig.Entry e : active) {
            int[] p = position(e, auto, sw, sh);
            drawCard(ctx, tr, e, p[0], p[1],
                    CooldownManager.remainingMs(e.id),
                    Math.max(1, CooldownManager.totalMs(e.id)));
        }
    }

    public static void drawCard(DrawContext ctx, TextRenderer tr, ModConfig.Entry e, int x, int y, long rem, long total) {
        float frac = (e.boss && rem <= 0) ? 1f : Math.min(1f, rem / (float) Math.max(1, total));
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
        boolean spawned = e.boss && rem <= 0;
        boolean urgent = rem < 3000;
        String timeText = spawned ? "Spawnolt!" : formatTime(rem);
        int timeColor = spawned ? 0xFF55FF55 : (urgent ? 0xFFFF5555 : 0xFFFFFFFF);
        ctx.drawText(tr, timeText, x + 25, y + 15, timeColor, true);

        // progress bar alul
        int barX1 = x + 1, barX2 = x + W - 1, barY1 = y + H - 3, barY2 = y + H - 1;
        ctx.fill(barX1, barY1, barX2, barY2, 0x80000000);
        int filled = (int) ((barX2 - barX1) * frac);
        ctx.fill(barX1, barY1, barX1 + filled, barY2, accent);
    }

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private static ItemStack getStack(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            Identifier ident = Identifier.tryParse(k);
            Item item = ident == null ? net.minecraft.item.Items.BARRIER : Registries.ITEM.get(ident);
            return new ItemStack(item);
        });
    }

    private static String formatTime(long ms) {
        if (ms >= 3_600_000) {
            long s = (ms + 999) / 1000;
            return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        }
        if (ms >= 60_000) {
            long s = (ms + 999) / 1000;
            return String.format("%d:%02d", s / 60, s % 60);
        }
        return String.format("%.1fs", ms / 1000.0);
    }
}
