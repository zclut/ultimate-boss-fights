package com.hytalezx.mikaela.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytalezx.mikaela.Components.HitboxOffsetComponent;
import com.hytalezx.mikaela.Interactions.ApplyHitboxInteraction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class HitboxOffsetTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, HitboxOffsetComponent> hitboxOffsetType;

    public HitboxOffsetTickSystem(ComponentType<EntityStore, HitboxOffsetComponent> hitboxOffsetType) {
        this.hitboxOffsetType = hitboxOffsetType;
    }

    @NullableDecl
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float delta, int idx,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        HitboxOffsetComponent offset = chunk.getComponent(idx, hitboxOffsetType);
        if (offset == null) return;

        Ref<EntityStore> entityRef = chunk.getReferenceTo(idx);
        BoundingBox boundingBox = chunk.getComponent(idx, BoundingBox.getComponentType());
        if (boundingBox == null) {
            ApplyHitboxInteraction.PENDING_OFFSETS.remove(entityRef);
            commandBuffer.removeComponent(entityRef, hitboxOffsetType);
            return;
        }

        // Guardar hitbox original en el primer tick
        if (!offset.originalSaved) {
            Box original = boundingBox.getBoundingBox();
            offset.originalMinY = original.min.y;
            offset.originalMaxY = original.max.y;
            offset.originalSaved = true;
        }

        // Interpolar offset actual hacia el target
        float diff = offset.targetOffsetY - offset.currentOffsetY;
        if (Math.abs(diff) > 0.01f) {
            offset.currentOffsetY += diff * offset.speed;
        } else {
            offset.currentOffsetY = offset.targetOffsetY;
        }

        // Aplicar offset a la hitbox
        Box current = boundingBox.getBoundingBox();
        double newMinY = offset.originalMinY + offset.currentOffsetY;
        double newMaxY = offset.originalMaxY + offset.currentOffsetY;

        Box offsetBox = new Box(
                current.min.x, newMinY, current.min.z,
                current.max.x, newMaxY, current.max.z
        );
        boundingBox.setBoundingBox(offsetBox);

        // Countdown
        offset.ticksRemaining--;
        if (offset.ticksRemaining <= 0) {
            // Restaurar hitbox original
            Box restoredBox = new Box(
                    current.min.x, offset.originalMinY, current.min.z,
                    current.max.x, offset.originalMaxY, current.max.z
            );
            boundingBox.setBoundingBox(restoredBox);

            ApplyHitboxInteraction.PENDING_OFFSETS.remove(entityRef);
            commandBuffer.removeComponent(entityRef, hitboxOffsetType);
            LOGGER.atInfo().log("[HitboxOffsetTick] offset finalizado, hitbox restaurada");
        }
    }
}
