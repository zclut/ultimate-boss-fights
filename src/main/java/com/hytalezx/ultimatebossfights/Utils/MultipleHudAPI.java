package com.hytalezx.ultimatebossfights.Utils;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;

public final class MultipleHudAPI {

    public static final String HUD_KEY = "UltimateBossFights_BossBar";
    private static final String MHUD_CLASS = "com.buuz135.mhud.MultipleHUD";
    private static final MultipleHudAPI INSTANCE = new MultipleHudAPI();

    private volatile boolean classChecked;
    private volatile boolean classPresent;
    private volatile Object  instance;
    private volatile Method  setCustomHudMethod;
    private volatile Method  hideCustomHudMethod;

    public static MultipleHudAPI get() { return INSTANCE; }

    public synchronized void init() {
        if (instance != null) return;
        try {
            Class<?> cls;
            try {
                cls = Class.forName(MHUD_CLASS);
            } catch (ClassNotFoundException e) {
                cls = Class.forName(MHUD_CLASS, false, ClassLoader.getSystemClassLoader());
            }
            Method getInstance = cls.getMethod("getInstance");
            Object inst        = getInstance.invoke(null);
            setCustomHudMethod  = cls.getMethod("setCustomHud",
                    Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            hideCustomHudMethod = cls.getMethod("hideCustomHud",
                    Player.class, PlayerRef.class, String.class);
            instance     = inst;
            classPresent = true;
        } catch (Exception e) {
            classPresent = false;
        } finally {
            classChecked = true;
        }
    }

    public boolean isPresent() {
        if (!classChecked) init();
        return classPresent;
    }

    public boolean isAvailable() {
        return instance != null;
    }

    /** Returns true if MultipleHUD handled it, false → caller should fall back to HudManager. */
    public boolean setCustomHud(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (!isAvailable()) {
            if (!classChecked) init();
            if (!isAvailable()) return false;
        }
        try {
            setCustomHudMethod.invoke(instance, player, playerRef, HUD_KEY, hud);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if MultipleHUD handled it, false → caller should fall back to HudManager. */
    public boolean hideCustomHud(Player player, PlayerRef playerRef) {
        if (!isAvailable()) {
            if (!classChecked) init();
            if (!isAvailable()) return false;
        }
        try {
            hideCustomHudMethod.invoke(instance, player, playerRef, HUD_KEY);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
