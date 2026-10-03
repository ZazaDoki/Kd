package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private static final int TOP = 36;   // első sor y pozíciója
    private static final int ROW = 21;   // sorok közti távolság

    private final Screen parent;
    private final List<ModConfig.Entry> entries = new ArrayList<>();
    private final List<TextFieldWidget> secFields = new ArrayList<>();
    private TextFieldWidget yField;
    private TextFieldWidget perRowField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Cooldown Tracker – Beállítások"));
        this.parent = parent;
    }

    private static Text onOff(boolean b) {
        return Text.literal(b ? "Be" : "Ki");
    }

    @Override
    protected void init() {
        secFields.clear();
        entries.clear();
        ModConfig cfg = ModConfig.get();
        for (ModConfig.Entry e : cfg.entries) if (!e.boss) entries.add(e); // a boss kártyák külön kapcsolóval vannak
        int left = this.width / 2 - 150;
        int y = TOP;

        // felső sor: 4 gomb
        addDrawableChild(ButtonWidget.builder(Text.literal("Mod: ").append(onOff(cfg.enabled)), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(Text.literal("Mod: ").append(onOff(cfg.enabled)));
        }).dimensions(left, y, 72, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Boss: ").append(onOff(cfg.bossEnabled)), b -> {
            cfg.bossEnabled = !cfg.bossEnabled;
            b.setMessage(Text.literal("Boss: ").append(onOff(cfg.bossEnabled)));
        }).dimensions(left + 76, y, 72, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Előnézet"), b -> CooldownManager.startAllPreview())
                .dimensions(left + 152, y, 72, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Törlés"), b -> CooldownManager.clear())
                .dimensions(left + 228, y, 72, 20).build());
        y += 26;

        for (ModConfig.Entry e : entries) {
            final ModConfig.Entry entry = e;
            addDrawableChild(ButtonWidget.builder(onOff(entry.enabled), b -> {
                entry.enabled = !entry.enabled;
                b.setMessage(onOff(entry.enabled));
            }).dimensions(left + 150, y, 44, 20).build());

            TextFieldWidget f = new TextFieldWidget(this.textRenderer, left + 200, y, 50, 20, Text.literal(entry.label));
            if (entry.isEffect()) {
                f.setTextPredicate(s -> true);
                f.setText("auto");   // az idő a rajtad lévő hatásból jön
                f.setEditable(false);
            } else {
                f.setTextPredicate(s -> s.matches("[0-9.,]{0,6}"));
                f.setText(trim(entry.seconds));
            }
            addDrawableChild(f);
            secFields.add(f);
            y += ROW;
        }

        y += 6;
        yField = new TextFieldWidget(this.textRenderer, left + 200, y, 50, 20, Text.literal("Y"));
        yField.setTextPredicate(s -> s.matches("-?[0-9]{0,3}"));
        yField.setText(String.valueOf(cfg.yOffset));
        addDrawableChild(yField);
        y += ROW;

        perRowField = new TextFieldWidget(this.textRenderer, left + 200, y, 50, 20, Text.literal("Row"));
        perRowField.setTextPredicate(s -> s.matches("[0-9]{0,2}"));
        perRowField.setText(String.valueOf(cfg.maxPerRow));
        addDrawableChild(perRowField);
        y += 28;

        addDrawableChild(ButtonWidget.builder(Text.literal("Kártyák mozgatása"), b -> {
            applyFields(); // a beírt értékek ne vesszenek el
            this.client.setScreen(new MoveScreen(this));
        }).dimensions(left, y, 147, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Mentés és kilépés"), b -> close())
                .dimensions(left + 153, y, 147, 20).build());
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Készítette: FeherHollo69", this.width / 2, 20, 0xFFFFD166);

        int left = this.width / 2 - 150;
        int y = TOP + 26;
        for (int i = 0; i < entries.size(); i++) {
            ModConfig.Entry e = entries.get(i);
            ctx.drawTextWithShadow(this.textRenderer, e.label, left, y + 6, 0xFF000000 | e.color);
            if (!e.isEffect()) ctx.drawTextWithShadow(this.textRenderer, "mp", left + 254, y + 6, 0xFFAAAAAA);
            y += ROW;
        }
        y += 6;
        ctx.drawTextWithShadow(this.textRenderer, "Y eltolás (fel +)", left, y + 6, 0xFFFFFFFF);
        y += ROW;
        ctx.drawTextWithShadow(this.textRenderer, "Max jelző soronként", left, y + 6, 0xFFFFFFFF);
    }

    /** A beviteli mezők értékeit átírja a configba és elmenti. */
    private void applyFields() {
        ModConfig cfg = ModConfig.get();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isEffect()) continue;
            try {
                double v = Double.parseDouble(secFields.get(i).getText().replace(',', '.'));
                entries.get(i).seconds = Math.max(0.5, Math.min(3600, v));
            } catch (NumberFormatException ignored) {}
        }
        try { cfg.yOffset = Integer.parseInt(yField.getText()); } catch (NumberFormatException ignored) {}
        try { cfg.maxPerRow = Math.max(1, Math.min(10, Integer.parseInt(perRowField.getText()))); } catch (NumberFormatException ignored) {}
        ModConfig.save();
    }

    @Override
    public void close() {
        applyFields();
        this.client.setScreen(parent);
    }
}
