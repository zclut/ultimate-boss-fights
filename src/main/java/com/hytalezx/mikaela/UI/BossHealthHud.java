package com.hytalezx.mikaela.UI;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class BossHealthHud extends CustomUIHud {

    private final String bossName;
    private final String hudStyle;
    private double percentagePv;

    public double currentDistanceSq = Double.MAX_VALUE;

    public BossHealthHud(@Nonnull PlayerRef playerRef, String bossName, double percentagePv, String hudStyle) {
        super(playerRef);
        this.bossName     = bossName;
        this.percentagePv = percentagePv;
        this.hudStyle     = hudStyle;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append(this.hudStyle);
        builder.set("#BossName.Text", this.bossName);
        builder.set("#BossHealthBar.Value", this.percentagePv);
    }

    public void updateHealth(double newPercentage) {
        this.percentagePv = newPercentage;
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#BossHealthBar.Value", this.percentagePv);
        update(false, builder);
    }

    public String getBossName() {
        return bossName;
    }
}