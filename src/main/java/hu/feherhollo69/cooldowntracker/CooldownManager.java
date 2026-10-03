package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CooldownManager {
    private static final Map<String, Long> END = new HashMap<>();
    private static final Map<String, Long> TOTAL = new HashMap<>();
    /** Boss id -> mikor spawnolt (a "Spawnolt!" felirathoz). */
    private static final Map<String, Long> SPAWNED = new HashMap<>();
    /** Boss id-k, amelyek visszaszámlálása fut, és a lejáratkor jelezni kell. */
    private static final Set<String> PENDING = new HashSet<>();

    /** Effekt kártya id -> eddig tart az előnézet (addig nem írja felül a valódi hatás). */
    private static final Map<String, Long> PREVIEW = new HashMap<>();

    public static final long SPAWNED_SHOW_MS = 30_000;

    public static void handle(ItemStack stack, String trigger) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || stack == null || stack.isEmpty()) return;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);

        // REEL: jobb klikk, amikor már ki van dobva a horgászbot (azaz visszahúzod)
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean reeling = "USE".equals(trigger) && mc.player != null && mc.player.fishHook != null;

        for (ModConfig.Entry e : cfg.entries) {
            if (!e.enabled || e.isEffect() || !e.item.equals(itemId)) continue;
            boolean match = e.trigger.equalsIgnoreCase(trigger)
                    || (reeling && e.trigger.equalsIgnoreCase("REEL"));
            if (!match) continue;
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

    /** Boss visszaszámlálás indítása / frissítése a /boss válaszából. */
    public static void startBoss(ModConfig.Entry e, long ms, boolean minuteOnly) {
        long now = System.currentTimeMillis();
        if (ms <= 0 && SPAWNED.containsKey(e.id)) return; // már jeleztük a spawnt

        if (minuteOnly && ms > 0) {
            // csak percben megadott idő: a valós érték ~egy perces sávban van, a közepével számolunk
            long est = ms + 30_000;
            long cur = remainingMs(e.id);
            if (cur > 0 && Math.abs(cur - est) <= 45_000 && PENDING.contains(e.id)) return; // folyamatos marad
            ms = est;
        }
        END.put(e.id, now + ms);
        TOTAL.put(e.id, Math.max(1, ms));
        SPAWNED.remove(e.id);
        PENDING.add(e.id);
    }

    /** Minden kliens tick-ben hívódik: észleli, ha egy boss ideje lejárt. */
    public static void tick(MinecraftClient mc) {
        tickEffects(mc);
        if (PENDING.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (String id : new ArrayList<>(PENDING)) {
            Long end = END.get(id);
            if (end != null && now >= end) {
                PENDING.remove(id);
                SPAWNED.put(id, now);
                playSpawnSound(mc);
            }
        }
    }

    /** A rajtad lévő hatásokból frissíti az EFFECT kártyákat (pl. Erő II / III, Gyorsaság II). */
    private static void tickEffects(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        for (ModConfig.Entry e : ModConfig.get().entries) {
            if (!e.isEffect() || e.effect == null || e.effect.isBlank()) continue;

            Long pv = PREVIEW.get(e.id);
            if (pv != null) {
                if (now < pv) continue;
                PREVIEW.remove(e.id);
                END.remove(e.id);
            }

            long ticks = -1;
            Identifier id = Identifier.tryParse(e.effect);
            if (mc.player != null && id != null) {
                for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
                    if (inst.getEffectType().matchesId(id)
                            && inst.getAmplifier() == e.amplifier
                            && !inst.isInfinite()) {
                        ticks = inst.getDuration();
                        break;
                    }
                }
            }

            if (ticks <= 0) { END.remove(e.id); continue; }
            long ms = ticks * 50L;
            long prev = remainingMs(e.id);
            END.put(e.id, now + ms);
            if (prev <= 0 || ms > prev + 1000) TOTAL.put(e.id, ms); // új vagy meghosszabbított hatás
        }
    }

    private static void playSpawnSound(MinecraftClient mc) {
        if (mc.player == null) return;
        SoundEvent s = SoundEvent.of(Identifier.of("minecraft", "entity.player.levelup"));
        mc.player.playSound(s, 1.0f, 1.0f);
    }

    public static void startAllPreview() {
        for (ModConfig.Entry e : ModConfig.get().entries) {
            if (e.boss) {
                END.put(e.id, System.currentTimeMillis() + 15_000);
                TOTAL.put(e.id, 15_000L);
                SPAWNED.remove(e.id);
                PENDING.add(e.id);
            } else if (e.isEffect()) {
                long now = System.currentTimeMillis();
                PREVIEW.put(e.id, now + 15_000);
                END.put(e.id, now + 15_000);
                TOTAL.put(e.id, 15_000L);
            } else {
                start(e, true);
            }
        }
    }

    public static void clear() {
        END.clear();
        TOTAL.clear();
        SPAWNED.clear();
        PENDING.clear();
        PREVIEW.clear();
    }

    public static boolean isActive(String id) {
        return remainingMs(id) > 0 || inSpawnWindow(id);
    }

    public static boolean inSpawnWindow(String id) {
        Long t = SPAWNED.get(id);
        return t != null && System.currentTimeMillis() - t < SPAWNED_SHOW_MS;
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
