package com.hytalezx.mikaela.Components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GrabbedComponent implements Component<EntityStore> {

    public Ref<EntityStore> grabberRef;

    // Duración y daño
    public int   ticksRemaining;
    public float damagePerTick;
    public int   damageIntervalTicks;
    public int   damageTickCounter = 0;

    // Rotación original del jugador antes del grab — para restaurarla cada tick
    public float originalRotationX = 0f;
    public float originalRotationY = 0f;
    public float anchorOffsetY;
    public float anchorOffsetZ;
    public float anchorOffsetX;

    public GrabbedComponent(Ref<EntityStore> grabberRef,
                            int durationTicks,
                            float damagePerTick,
                            int damageIntervalTicks,
                            float anchorOffsetX,
                            float anchorOffsetY,
                            float anchorOffsetZ) {
        this.grabberRef          = grabberRef;
        this.ticksRemaining      = durationTicks;
        this.damagePerTick       = damagePerTick;
        this.damageIntervalTicks = damageIntervalTicks;
        this.anchorOffsetX       = anchorOffsetX;
        this.anchorOffsetY       = anchorOffsetY;
        this.anchorOffsetZ       = anchorOffsetZ;
    }

    // Constructor vacío requerido por el ComponentType
    public GrabbedComponent() {}

    @Override
    public Component<EntityStore> clone() {
        GrabbedComponent c = new GrabbedComponent(
                grabberRef, ticksRemaining, damagePerTick, damageIntervalTicks,
                anchorOffsetX, anchorOffsetY, anchorOffsetZ
        );
        c.damageTickCounter = this.damageTickCounter;
        return c;
    }
}