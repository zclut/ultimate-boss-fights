package com.hytalezx.ultimatebossfights.Systems;

import com.hytalezx.ultimatebossfights.Config.BossConfig;
import com.hytalezx.ultimatebossfights.Config.BossNPCTracker;
import com.hytalezx.ultimatebossfights.UI.BossHealthHud;
import com.hytalezx.ultimatebossfights.UI.EmptyHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Player-based system — always ticks regardless of NPC view frustum.
 * Reads cached NPC position/health from BossNPCTracker (written by BossTickingSystem
 * on the NPC's thread) — no cross-thread store access.
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

        HudManager hud     = player.getHudManager();
        Vector3d playerPos = playerTransform.getPosition();

        CustomUIHud current   = hud.getCustomHud();
        String showingName    = (current instanceof BossHealthHud)
                ? ((BossHealthHud) current).getBossName() : null;

        // Prune invalid refs
        for (BossNPCTracker.Entry entry : BossNPCTracker.entries()) {
            if (!entry.ref().isValid()) BossNPCTracker.unregister(entry.ref());
        }

        // Single-pass: find the closest in-range entry across ALL tracked NPCs.
        // Uses exit radius for the currently shown boss (hysteresis), entry radius for others.
        BossNPCTracker.Entry bestEntry  = null;
        double               bestDistSq = Double.MAX_VALUE;

        for (BossNPCTracker.Entry entry : BossNPCTracker.entries()) {
            if (!entry.ref().isValid()) continue;
            if (entry.npcWorld() != null && entry.npcWorld() != player.getWorld()) continue;

            double healthPct = entry.cachedHealthPct();
            if (healthPct <= 0.0) continue;

            double dx     = entry.cachedX() - playerPos.x;
            double dz     = entry.cachedZ() - playerPos.z;
            double distSq = dx * dx + dz * dz;

            boolean isShowing = entry.config().getDisplayName().equals(showingName);
            double  radius    = isShowing
                    ? entry.config().getExitBlocks()
                    : entry.config().getProximityBlocks();
            double  radiusSq  = radius * radius;

            if (distSq <= radiusSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                bestEntry  = entry;
            }
        }

        if (bestEntry == null) {
            if (current instanceof BossHealthHud) {
                hud.setCustomHud(playerRef, new EmptyHud(playerRef));
            }
        } else {
            String bestName = bestEntry.config().getDisplayName();
            if (current instanceof BossHealthHud && bestName.equals(showingName)) {
                BossHealthHud bossHud = (BossHealthHud) current;
                bossHud.updateHealth(playerRef, bestEntry.cachedHealthPct(), hud);
                bossHud.currentDistanceSq = bestDistSq;
            } else {
                setNewBossHud(hud, playerRef, bestEntry.config(),
                        bestEntry.cachedHealthPct(), bestDistSq);
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
}
