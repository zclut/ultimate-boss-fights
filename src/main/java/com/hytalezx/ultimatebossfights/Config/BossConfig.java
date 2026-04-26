package com.hytalezx.ultimatebossfights.Config;


public class BossConfig {
    private final String rolePrefix;
    private final String displayName;
    private final double proximityBlocks;
    private final double exitBlocks;
    private final String hudStyle;        // nombre del fichero .ui a usar

    public BossConfig(String rolePrefix, String displayName, double proximityBlocks, double exitBlocks, String hudStyle) {
        this.rolePrefix      = rolePrefix;
        this.displayName     = displayName;
        this.proximityBlocks = proximityBlocks;
        this.exitBlocks      = Math.max(proximityBlocks, exitBlocks);
        this.hudStyle        = hudStyle;
    }

    public BossConfig(String rolePrefix, String displayName, double proximityBlocks, String hudStyle) {
        this(rolePrefix, displayName, proximityBlocks, proximityBlocks * 1.2, hudStyle);
    }


    public BossConfig(String rolePrefix, String displayName, double proximityBlocks) {
        this(rolePrefix, displayName, proximityBlocks, proximityBlocks * 1.2, "bosshealth.ui");
    }

    public String getRolePrefix()      { return rolePrefix; }
    public String getDisplayName()     { return displayName; }
    public double getProximityBlocks() { return proximityBlocks; }
    public double getExitBlocks()      { return exitBlocks; }
    public String getHudStyle()        { return hudStyle; }
}