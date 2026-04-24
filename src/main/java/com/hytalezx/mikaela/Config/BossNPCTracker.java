package com.hytalezx.mikaela.Config;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BossNPCTracker {

    public record Entry(Ref<EntityStore> ref, BossConfig config) {}

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    public static void register(Ref<EntityStore> ref, BossConfig config) {
        // Remove any entry for this boss (same config instance) OR same ref — one entry per boss
        ENTRIES.removeIf(e -> e.config() == config || e.ref() == ref);
        ENTRIES.add(new Entry(ref, config));
    }

    public static void unregister(Ref<EntityStore> ref) {
        ENTRIES.removeIf(e -> e.ref() == ref);
    }

    /** Remove entries whose Ref is no longer valid (entity removed from store). */
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
