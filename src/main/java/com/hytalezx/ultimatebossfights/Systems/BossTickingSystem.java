package com.hytalezx.ultimatebossfights.Systems;

import com.hytalezx.ultimatebossfights.Config.BossConfig;
import com.hytalezx.ultimatebossfights.Config.BossNPCTracker;
import com.hytalezx.ultimatebossfights.Config.BossRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * NPC-based system — runs when NPC is in view.
 * Registers boss NPCs into BossNPCTracker so PlayerBossHudSystem
 * can update the HUD even when the player looks away.
 */
public class BossTickingSystem extends EntityTickingSystem<EntityStore> {

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float delta, int idx,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        NPCEntity npc = (NPCEntity) chunk.getComponent(idx, NPCEntity.getComponentType());
        if (npc == null || npc.getWorld() == null) return;

        BossConfig config = BossRegistry.resolve(npc.getRoleName());
        if (config == null) return;

        EntityStatMap statMap = (EntityStatMap) chunk.getComponent(idx, EntityStatMap.getComponentType());
        if (statMap == null) return;
        EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthValue == null) return;

        double healthPct = healthValue.asPercentage();

        TransformComponent transform = (TransformComponent) chunk.getComponent(
                idx, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        Ref<EntityStore> ref = chunk.getReferenceTo(idx);
        BossNPCTracker.registerOrUpdate(ref, config, npc.getWorld(), healthPct, pos.x, pos.z);

        if (healthPct <= 0.0) return;
    }
}
