package com.hytalezx.mikaela.Systems;

import com.hytalezx.mikaela.Config.BossConfig;
import com.hytalezx.mikaela.Config.BossNPCTracker;
import com.hytalezx.mikaela.Config.BossRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * NPC-based system — runs only when NPC is in view (frustum).
 * Sole job: populate BossNPCTracker so PlayerBossHudSystem can work
 * even when the NPC is out of the player's view.
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

        UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(idx, UUIDComponent.getComponentType());
        if (uuidComp == null) return;

        EntityStatMap statMap = (EntityStatMap) chunk.getComponent(idx, EntityStatMap.getComponentType());
        if (statMap == null) return;
        EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthValue != null && healthValue.asPercentage() <= 0.0) return;

        BossNPCTracker.register(uuidComp.getUuid(), config);
    }
}
