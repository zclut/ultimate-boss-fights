package com.hytalezx.mikaela.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public class BossRegistry {

    private static final Map<String, BossConfig> REGISTRY = new LinkedHashMap<>();

    public static void register(BossConfig config) {
        REGISTRY.put(config.getRolePrefix(), config);
    }

    /** Devuelve la config si el roleName coincide con algún prefijo, o null si no es boss. */
    public static BossConfig resolve(String npcRoleName) {
        for (Map.Entry<String, BossConfig> entry : REGISTRY.entrySet()) {
            if (npcRoleName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}