package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Átlátszó képernyő, ahol az egérrel el lehet húzni a cooldown kártyákat. */
public class MoveScreen extends Screen {
    private final Screen parent;
    private ModConfig.Entry dragging;
    private int grabX, grabY;
    // true-ról indul, hogy a "Kártyák mozgatása" gomb kattintása ne fogjon meg kártyát
    private boolean wasDown = true;

    public MoveScreen(Screen parent) {
        super(Text.literal("Kártyák mozgatása"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Alaphelyzet"), b -> ModConfig.resetPositions())
                .dimensions(this.width / 2 - 105, 34, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Kész"), b -> close())
                .dimensions(this.width / 2 + 5, 34, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // szándékosan üres: átlátszó háttér, hogy lásd a játékot és a hotbart
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, Math.max(0, max)));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModConfig cfg = ModConfig.get();
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        List<ModConfig.Entry> list = new ArrayList<>();
        for (ModConfig.Entry e : cfg.entries) if (e.enabled) list.add(e);
        Map<ModConfig.Entry, int[]> auto = CooldownHud.autoLayout(list, sw, sh, cfg);

        // egér állapot: lenyomás -> megfogás, húzás -> mozgatás, felengedés -> elengedés
        boolean down = this.client.mouse.wasLeftButtonClicked();
        if (down && !wasDown) {
            for (int i = list.size() - 1; i >= 0; i--) {
                ModConfig.Entry e = list.get(i);
                int[] p = CooldownHud.position(e, auto, sw, sh);
                if (mouseX >= p[0] && mouseX < p[0] + CooldownHud.W
                        && mouseY >= p[1] && mouseY < p[1] + CooldownHud.H) {
                    dragging = e;
                    grabX = mouseX - p[0];
                    grabY = mouseY - p[1];
                    break;
                }
            }
        } else if (down && dragging != null) {
            int x = clamp(mouseX - grabX, sw - CooldownHud.W);
            int y = clamp(mouseY - grabY, sh - CooldownHud.H);
            dragging.moved = true;
            dragging.posX = x - sw / 2;
            dragging.posY = y - sh;
            auto = CooldownHud.autoLayout(list, sw, sh, cfg); // a többi kártya újrarendeződhet
        } else if (!down) {
            dragging = null;
        }
        wasDown = down;

        // kártyák (mintha 60%-ig lennének kitöltve)
        for (ModConfig.Entry e : list) {
            int[] p = CooldownHud.position(e, auto, sw, sh);
            long total = (long) (e.seconds * 1000);
            CooldownHud.drawCard(ctx, this.textRenderer, e, p[0], p[1], (long) (total * 0.6), Math.max(1, total));

            boolean hover = mouseX >= p[0] && mouseX < p[0] + CooldownHud.W
                    && mouseY >= p[1] && mouseY < p[1] + CooldownHud.H;
            if (e == dragging || (hover && dragging == null)) {
                int c = e == dragging ? 0xFFFFFFFF : 0x99FFFFFF;
                int x2 = p[0] + CooldownHud.W, y2 = p[1] + CooldownHud.H;
                ctx.fill(p[0] - 1, p[1] - 1, x2 + 1, p[1], c);
                ctx.fill(p[0] - 1, y2, x2 + 1, y2 + 1, c);
                ctx.fill(p[0] - 1, p[1], p[0], y2, c);
                ctx.fill(x2, p[1], x2 + 1, y2, c);
            }
        }

        // gombok + szöveg
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Húzd a kártyákat az egérrel a kívánt helyre", this.width / 2, 12, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "ESC vagy Kész = mentés", this.width / 2, 23, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        ModConfig.save();
        this.client.setScreen(parent);
    }
}
