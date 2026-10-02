package hu.feherhollo69.cooldowntracker;

import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A /boss parancs válaszát olvassa a chatből, és percenként magától is lekérdezi.
 * Várt formátum soronként, pl.:  "Lich King: 30p 1mp",  "World Boss: 0h 16p múlva"
 */
public class BossTracker {
    private static final Pattern HOURS = Pattern.compile("(\\d+)\\s*(?:h|ó)(?![\\p{L}])");
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*p(?![\\p{L}])");
    private static final Pattern SECONDS = Pattern.compile("(\\d+)\\s*(?:mp|s)(?![\\p{L}])");

    private static long nextPoll = 0;
    private static long suppressUntil = 0;
    private static long pollSentAt = 0;
    private static boolean awaiting = false;
    private static boolean gotResponse = false;
    private static int failures = 0;
    private static boolean autoStopped = false;

    /**
     * Feldolgozza a chat üzenetet. Visszaadja, hogy megjeleníthető-e (false = elrejtjük,
     * mert a saját automatikus /boss lekérdezésünk válasza).
     */
    public static boolean onMessage(String text) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || !cfg.bossEnabled || text == null) return true;

        // a saját /boss lekérdezésünk ideje alatt a díszítő sorokat (----, ▬▬▬, üres sor) is elrejtjük
        if (System.currentTimeMillis() < suppressUntil) {
            boolean hasContent = false;
            for (int i = 0; i < text.length(); i++) {
                if (Character.isLetterOrDigit(text.charAt(i))) { hasContent = true; break; }
            }
            if (!hasContent) return false;
        }

        boolean relevant = false;
        boolean parsed = false;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("boss inform") || lower.contains("boss token")) relevant = true;

        for (String line : text.split("\\R")) {
            String l = line.trim();
            for (ModConfig.Entry e : cfg.entries) {
                if (!e.boss || e.bossName == null || e.bossName.isBlank()) continue;
                if (!l.toLowerCase(Locale.ROOT).startsWith(e.bossName.toLowerCase(Locale.ROOT))) continue;
                String rest = l.substring(e.bossName.length()).trim();
                if (!rest.startsWith(":")) continue;
                relevant = true;
                rest = rest.substring(1);

                Matcher h = HOURS.matcher(rest), m = MINUTES.matcher(rest), s = SECONDS.matcher(rest);
                boolean hasH = h.find(), hasM = m.find(), hasS = s.find();
                if (!hasH && !hasM && !hasS) continue; // pl. nincs aktív időzítő

                long ms = 0;
                if (hasH) ms += Long.parseLong(h.group(1)) * 3_600_000L;
                if (hasM) ms += Long.parseLong(m.group(1)) * 60_000L;
                if (hasS) ms += Long.parseLong(s.group(1)) * 1_000L;
                CooldownManager.startBoss(e, ms, !hasS);
                parsed = true;
            }
        }

        if (parsed) {
            gotResponse = true;
            failures = 0;
            autoStopped = false;
        }
        // csak a saját automatikus lekérdezésünk válaszát rejtjük el, a kézi /boss látszik
        return !(relevant && System.currentTimeMillis() < suppressUntil);
    }

    public static void tick(MinecraftClient mc) {
        ModConfig cfg = ModConfig.get();
        if (mc.player == null || mc.getNetworkHandler() == null) {
            nextPoll = 0; failures = 0; autoStopped = false; awaiting = false;
            return;
        }
        if (!cfg.enabled || !cfg.bossEnabled || mc.isInSingleplayer()) return;

        long now = System.currentTimeMillis();

        // ha nem jött használható válasz, 3 próbálkozás után leáll az automatika
        if (awaiting && now - pollSentAt > 4000) {
            awaiting = false;
            if (!gotResponse && ++failures >= 3) autoStopped = true;
        }

        if (autoStopped) return;
        if (nextPoll == 0) nextPoll = now + 10_000; // csatlakozás után 10 mp-pel az első
        if (now >= nextPoll) {
            nextPoll = now + Math.max(15, cfg.bossIntervalSec) * 1000L;
            suppressUntil = now + 4000;
            pollSentAt = now;
            awaiting = true;
            gotResponse = false;
            mc.getNetworkHandler().sendChatCommand("boss");
        }
    }
}
