package com.hytalezx.mikaela.Config;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BossNPCTracker {

    public static class Entry {
        private final Ref<EntityStore> ref;
        private final BossConfig config;
        private volatile World npcWorld;
        private volatile double cachedHealthPct = 1.0;
        private volatile double cachedX = 0;
        private volatile double cachedZ = 0;

        Entry(Ref<EntityStore> ref, BossConfig config) {
            this.ref = ref;
            this.config = config;
        }

        public Ref<EntityStore> ref()   { return ref; }
        public BossConfig config()      { return config; }
        public World npcWorld()         { return npcWorld; }
        public double cachedHealthPct() { return cachedHealthPct; }
        public double cachedX()         { return cachedX; }
        public double cachedZ()         { return cachedZ; }

        public void update(World world, double healthPct, double x, double z) {
            this.npcWorld       = world;
            this.cachedHealthPct = healthPct;
            this.cachedX = x;
            this.cachedZ = z;
        }
    }

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    public static void registerOrUpdate(Ref<EntityStore> ref, BossConfig config,
                                        World world, double healthPct, double x, double z) {
        for (Entry e : ENTRIES) {
            if (e.ref() == ref) {
                e.update(world, healthPct, x, z);
                return;
            }
        }
        // new entry — remove stale entry for same config first
        ENTRIES.removeIf(e -> e.config() == config);
        Entry entry = new Entry(ref, config);
        entry.update(world, healthPct, x, z);
        ENTRIES.add(entry);
    }

    public static void register(Ref<EntityStore> ref, BossConfig config) {
        for (Entry e : ENTRIES) {
            if (e.ref() == ref) return;
        }
        ENTRIES.removeIf(e -> e.config() == config);
        ENTRIES.add(new Entry(ref, config));
    }

    public static void markDead(Ref<EntityStore> ref) {
        for (Entry e : ENTRIES) {
            if (e.ref() == ref) {
                e.update(e.npcWorld(), 0.0, e.cachedX(), e.cachedZ());
                return;
            }
        }
    }

    public static void unregister(Ref<EntityStore> ref) {
        ENTRIES.removeIf(e -> e.ref() == ref);
    }

    public static void pruneInvalid() {
        ENTRIES.removeIf(e -> !e.ref().isValid());
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }
}
