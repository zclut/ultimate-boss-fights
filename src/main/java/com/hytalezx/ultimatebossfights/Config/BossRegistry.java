package com.hytalezx.ultimatebossfights.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public class BossRegistry {

    private static final Map<String, BossConfig> REGISTRY = new LinkedHashMap<>();

    public static void register(BossConfig config) {
        REGISTRY.put(config.getRolePrefix(), config);
    }

    public static BossConfig resolve(String npcRoleName) {
        for (Map.Entry<String, BossConfig> entry : REGISTRY.entrySet()) {
            if (npcRoleName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}