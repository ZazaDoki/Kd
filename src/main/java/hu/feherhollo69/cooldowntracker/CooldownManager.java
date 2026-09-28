package hu.feherhollo69.cooldowntracker;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CooldownManager {
    private static final Map<String, Long> END = new HashMap<>();
    private static final Map<String, Long> TOTAL = new HashMap<>();

    public static void handle(ItemStack stack, String trigger) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || stack == null || stack.isEmpty()) return;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);

        for (ModConfig.Entry e : cfg.entries) {
            if (!e.enabled || !e.trigger.equalsIgnoreCase(trigger) || !e.item.equals(itemId)) continue;
            if (e.nameContains != null && !e.nameContains.isBlank()
                    && !name.contains(e.nameContains.toLowerCase(Locale.ROOT))) continue;
            start(e, false);
        }
    }

    public static void start(ModConfig.Entry e, boolean force) {
        if (!force && isActive(e.id)) return; // ne indítsuk újra, amíg tart
        long ms = (long) (e.seconds * 1000);
        END.put(e.id, System.currentTimeMillis() + ms);
        TOTAL.put(e.id, ms);
    }

    public static void startAllPreview() {
        for (ModConfig.Entry e : ModConfig.get().entries) start(e, true);
    }

    public static void clear() {
        END.clear();
        TOTAL.clear();
    }

    public static boolean isActive(String id) {
        return remainingMs(id) > 0;
    }

    public static long remainingMs(String id) {
        Long end = END.get(id);
        if (end == null) return 0;
        return Math.max(0, end - System.currentTimeMillis());
    }

    public static long totalMs(String id) {
        return TOTAL.getOrDefault(id, 1L);
    }
}
