package com.hytalezx.mikaela.Components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HitboxOffsetComponent implements Component<EntityStore> {

    public float targetOffsetY;
    public float speed;
    public int ticksRemaining;
    public float currentOffsetY = 0f;

    // Hitbox original para restaurar al finalizar
    public double originalMinY;
    public double originalMaxY;
    public boolean originalSaved = false;

    public HitboxOffsetComponent() {}

    public HitboxOffsetComponent(float targetOffsetY, float speed, int durationTicks) {
        this.targetOffsetY = targetOffsetY;
        this.speed = speed;
        this.ticksRemaining = durationTicks;
    }

    @Override
    public Component<EntityStore> clone() {
        HitboxOffsetComponent c = new HitboxOffsetComponent(targetOffsetY, speed, ticksRemaining);
        c.currentOffsetY = this.currentOffsetY;
        c.originalMinY = this.originalMinY;
        c.originalMaxY = this.originalMaxY;
        c.originalSaved = this.originalSaved;
        return c;
    }
}
