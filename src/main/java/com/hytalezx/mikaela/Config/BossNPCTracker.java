package com.hytalezx.mikaela.Config;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossNPCTracker {

    private static final ConcurrentHashMap<UUID, BossConfig> LIVE = new ConcurrentHashMap<>();

    public static void register(UUID uuid, BossConfig config) {
        LIVE.put(uuid, config);
    }

    public static void unregister(UUID uuid) {
        LIVE.remove(uuid);
    }

    public static Set<Map.Entry<UUID, BossConfig>> entries() {
        return LIVE.entrySet();
    }

    public static boolean isEmpty() {
        return LIVE.isEmpty();
    }
}
