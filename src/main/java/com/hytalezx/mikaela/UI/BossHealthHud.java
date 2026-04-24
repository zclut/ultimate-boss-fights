package com.hytalezx.mikaela.UI;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class BossHealthHud extends CustomUIHud {

    private final String bossName;
    private final String hudStyle;
    private double       percentagePv;
    private String       currentTier;

    public double  currentDistanceSq = Double.MAX_VALUE;
    boolean        entryEventFired   = false;

    public BossHealthHud(@Nonnull PlayerRef playerRef, String bossName,
                         double percentagePv, String hudStyle) {
        super(playerRef);
        this.bossName     = bossName;
        this.percentagePv = percentagePv;
        this.hudStyle     = hudStyle;
        this.currentTier  = getTier(percentagePv);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append(this.hudStyle);
        applyTier(builder, this.currentTier, this.percentagePv);
    }

    public void updateHealth(@Nonnull PlayerRef playerRef, double newPercentage,
                             HudManager hudManager) {
        String newTier = getTier(newPercentage);

        if (!newTier.equals(this.currentTier)) {
            hudManager.setCustomHud(playerRef,
                    new BossHealthHud(playerRef, bossName, newPercentage, hudStyle));
        } else {
            this.percentagePv = newPercentage;
            UICommandBuilder builder = new UICommandBuilder();
            builder.set("#BossHealthBar" + capitalize(currentTier) + ".Value", newPercentage);
            update(false, builder);
        }
    }

    private void applyTier(UICommandBuilder builder, String tier, double value) {
        for (String t : new String[]{"Yellow", "Orange", "Red"}) {
            boolean active = t.equalsIgnoreCase(tier);
            builder.set("#BossHealthBar" + t + ".Visible", active);
            if (active) builder.set("#BossHealthBar" + t + ".Value", value);
        }
    }

    private String getTier(double pct) {
        if (pct > 0.66) return "yellow";
        if (pct > 0.33) return "orange";
        return "red";
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public String getBossName() { return bossName; }
}