package com.hytalezx.mikaela.Systems;

import com.hytalezx.mikaela.Config.BossConfig;
import com.hytalezx.mikaela.Config.BossNPCTracker;
import com.hytalezx.mikaela.UI.BossHealthHud;
import com.hytalezx.mikaela.UI.EmptyHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Map;
import java.util.UUID;

/**
 * Player-based system — always ticks regardless of NPC visibility.
 * Reads fresh NPC data from the store by UUID (from BossNPCTracker)
 * so the HUD works even when the player isn't looking at the boss.
 */
public class PlayerBossHudSystem extends EntityTickingSystem<EntityStore> {

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

        if (BossNPCTracker.isEmpty()) return;

        Player player = (Player) chunk.getComponent(idx, Player.getComponentType());
        if (player == null || player.getWorld() == null) return;

        TransformComponent playerTransform = (TransformComponent) chunk.getComponent(
                idx, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        PlayerRef playerRef = (PlayerRef) chunk.getComponent(idx, PlayerRef.getComponentType());
        if (playerRef == null) return;

        HudManager hud   = player.getHudManager();
        Vector3d playerPos = playerTransform.getPosition();

        for (Map.Entry<UUID, BossConfig> entry : BossNPCTracker.entries()) {
            UUID bossUuid   = entry.getKey();
            BossConfig config = entry.getValue();

            Ref<EntityStore> npcRef = player.getWorld().getEntityRef(bossUuid);
            if (npcRef == null) continue;

            TransformComponent npcTransform = (TransformComponent) store.getComponent(
                    npcRef, TransformComponent.getComponentType());
            if (npcTransform == null) continue;

            EntityStatMap statMap = (EntityStatMap) store.getComponent(
                    npcRef, EntityStatMap.getComponentType());
            if (statMap == null) continue;

            EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
            if (healthValue == null) continue;

            double healthPct  = healthValue.asPercentage();
            Vector3d npcPos   = npcTransform.getPosition();
            double distSq     = horizontalDistSq(npcPos, playerPos);

            double entryRadiusSq = config.getProximityBlocks() * config.getProximityBlocks();
            double exitRadiusSq  = config.getExitBlocks()      * config.getExitBlocks();

            CustomUIHud current = hud.getCustomHud();
            boolean alreadyShowingThisBoss = current instanceof BossHealthHud
                    && ((BossHealthHud) current).getBossName().equals(config.getDisplayName());

            double activeRadiusSq = alreadyShowingThisBoss ? exitRadiusSq : entryRadiusSq;
            boolean inRange = healthPct > 0.0 && distSq <= activeRadiusSq;

            if (inRange) {
                if (alreadyShowingThisBoss) {
                    BossHealthHud currentBossHud = (BossHealthHud) current;
                    currentBossHud.updateHealth(playerRef, healthPct, hud);
                    currentBossHud.currentDistanceSq = distSq;
                } else if (current instanceof BossHealthHud) {
                    BossHealthHud currentBossHud = (BossHealthHud) current;
                    if (distSq < currentBossHud.currentDistanceSq) {
                        setNewBossHud(hud, playerRef, config, healthPct, distSq);
                    }
                } else {
                    setNewBossHud(hud, playerRef, config, healthPct, distSq);
                }
            } else {
                if (alreadyShowingThisBoss) {
                    hud.setCustomHud(playerRef, new EmptyHud(playerRef));
                }
            }
        }
    }

    private void setNewBossHud(HudManager hud, PlayerRef playerRef,
                               BossConfig config, double healthPct, double distSq) {
        BossHealthHud newHud = new BossHealthHud(playerRef, config.getDisplayName(),
                healthPct, config.getHudStyle());
        newHud.currentDistanceSq = distSq;
        hud.setCustomHud(playerRef, newHud);
    }

    private double horizontalDistSq(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
