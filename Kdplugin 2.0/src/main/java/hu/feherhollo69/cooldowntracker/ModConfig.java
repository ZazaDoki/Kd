package hu.feherhollo69.cooldowntracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker.json");
    private static ModConfig instance;

    public boolean enabled = true;
    /** Pozitív = feljebb, negatív = lejjebb a alapértelmezett helytől. */
    public int yOffset = 0;
    /** Egy sorban legfeljebb ennyi jelző van, utána új sor kezdődik felfelé. */
    public int maxPerRow = 4;
    /** Boss időzítők (a /boss parancs válaszából) be/ki. */
    public boolean bossEnabled = true;
    /** Ennyi másodpercenként küldi el magától a /boss parancsot. */
    public int bossIntervalSec = 60;
    public List<Entry> entries = defaults();

    public static class Entry {
        public String id;
        public String label;
        public String item;          // pl. minecraft:ender_pearl
        public String trigger;       // "USE" (jobb klikk) vagy "HIT" (ütés)
        public double seconds;
        public int color;            // RGB
        public String nameContains;  // üres = bármilyen nevű item; egyébként a név tartalmazza (kis/nagybetű mindegy)
        public boolean enabled = true;
        /** true = boss kártya, az idejét a /boss parancs válaszából olvassa. */
        public boolean boss = false;
        /** A boss neve úgy, ahogy a /boss válaszában szerepel. */
        public String bossName = "";
        /** true = a felhasználó kézzel elmozgatta, ilyenkor a posX/posY számít. */
        public boolean moved = false;
        /** Vízszintes helyzet a képernyő közepéhez képest. */
        public int posX = 0;
        /** Függőleges helyzet a képernyő aljához képest (negatív = feljebb). */
        public int posY = 0;

        public Entry() {}

        /** Látható-e a kártya a HUD-on / mozgatáskor. */
        public boolean visible(ModConfig cfg) {
            return enabled && (!boss || cfg.bossEnabled);
        }

        public Entry(String id, String label, String item, String trigger, double seconds, int color, String nameContains) {
            this.id = id; this.label = label; this.item = item; this.trigger = trigger;
            this.seconds = seconds; this.color = color; this.nameContains = nameContains;
        }
    }

    public static List<Entry> defaults() {
        List<Entry> l = new ArrayList<>();
        l.add(new Entry("pearl",     "Ender Pearl",    "minecraft:ender_pearl",  "USE", 9,   0x3DDC97, ""));
        l.add(new Entry("ice_axe",   "Jég Balta",      "minecraft:diamond_axe",  "HIT", 10,  0x55FFFF, "Balta"));
        l.add(new Entry("speed",     "Gyorsaság II",   "minecraft:sugar",        "USE", 30,  0x7DD3FC, "Gyorsaság"));
        l.add(new Entry("strength",  "Erő II",         "minecraft:blaze_powder", "USE", 30,  0xFF5555, "Erő"));
        l.add(new Entry("bamboozle", "Bamboozle",      "minecraft:blaze_rod",    "HIT", 90,  0xFFAA00, "Bamboozle"));
        l.add(new Entry("pearl_hate","Gyöngy Gyűlölő", "minecraft:purple_dye",   "HIT", 120, 0xC77DFF, "Gyöngy"));
        l.add(new Entry("freeze",    "Fagyasztás",     "minecraft:ice",          "HIT", 120, 0x8BE9FD, "Fagyaszt"));
        l.add(new Entry("push",      "Lökés",          "minecraft:rabbit_foot",  "USE", 15,  0x9AFF6B, "Lökés"));
        l.add(boss("boss_world", "World Boss", "minecraft:wither_skeleton_skull", 0xFF5555, "World Boss"));
        l.add(boss("boss_lich",  "Lich King",  "minecraft:skeleton_skull",        0x9D7BFF, "Lich King"));
        l.add(boss("boss_oog",   "Oog",        "minecraft:zombie_head",           0xFFAA00, "Oog"));
        return l;
    }

    private static Entry boss(String id, String label, String item, int color, String bossName) {
        Entry e = new Entry(id, label, item, "BOSS", 600, color, "");
        e.boss = true;
        e.bossName = bossName;
        return e;
    }

    public static void resetPositions() {
        for (Entry e : get().entries) {
            e.moved = false;
            e.posX = 0;
            e.posY = 0;
        }
    }

    public static ModConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    instance = GSON.fromJson(r, ModConfig.class);
                }
            }
        } catch (Exception e) {
            System.err.println("[CooldownTracker] Config betöltési hiba: " + e);
        }
        if (instance == null || instance.entries == null) instance = new ModConfig();
        // régi configból hiányzó (új) kártyák pótlása
        for (Entry d : defaults()) {
            boolean found = false;
            for (Entry e : instance.entries) if (d.id.equals(e.id)) { found = true; break; }
            if (!found) instance.entries.add(d);
        }
        if (instance.bossIntervalSec < 15) instance.bossIntervalSec = 15;
        save();
    }

    public static void save() {
        try {
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            System.err.println("[CooldownTracker] Config mentési hiba: " + e);
        }
    }
}
