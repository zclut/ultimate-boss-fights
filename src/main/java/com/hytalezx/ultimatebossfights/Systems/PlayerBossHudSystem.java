package com.hytalezx.ultimatebossfights.Systems;

import com.hytalezx.ultimatebossfights.Config.BossConfig;
import com.hytalezx.ultimatebossfights.Config.BossNPCTracker;
import com.hytalezx.ultimatebossfights.UI.BossHealthHud;
import com.hytalezx.ultimatebossfights.Utils.EntityUtils;
import com.hytalezx.ultimatebossfights.Utils.MultipleHudAPI;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-based system — always ticks regardless of NPC view frustum.
 * Reads cached NPC position/health from BossNPCTracker (written by BossTickingSystem
 * on the NPC's thread) — no cross-thread store access.
 */
public class PlayerBossHudSystem extends EntityTickingSystem<EntityStore> {

    private record ActiveEntry(BossHealthHud hud, Player player, PlayerRef playerRef) {}

    // Keyed by UUID so instance changes (new Ref) don't orphan an active HUD.
    private final ConcurrentHashMap<UUID, ActiveEntry> activeHuds = new ConcurrentHashMap<>();

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

        Vector3d playerPos = playerTransform.getPosition();

        UUID uuid = EntityUtils.getUuid(playerRef);
        if (uuid == null) return;

        ActiveEntry      active      = activeHuds.get(uuid);
        BossHealthHud    current     = active != null ? active.hud() : null;
        String           showingName = current != null ? current.getBossName() : null;

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
            if (current != null) {
                hideHud(active);
                activeHuds.remove(uuid);
            }
        } else {
            String bestName = bestEntry.config().getDisplayName();
            if (current != null && bestName.equals(showingName)) {
                current.updateHealth(playerRef, player, bestEntry.cachedHealthPct());
                current.currentDistanceSq = bestDistSq;
            } else {
                if (current != null) hideHud(active);
                setNewBossHud(uuid, player, playerRef,
                        bestEntry.config(), bestEntry.cachedHealthPct(), bestDistSq);
            }
        }
    }

    private void hideHud(ActiveEntry active) {
        if (!MultipleHudAPI.get().hideCustomHud(active.player(), active.playerRef())) {
            active.player().getHudManager().setCustomHud(active.playerRef(), null);
        }
    }

    private void setNewBossHud(UUID uuid, Player player, PlayerRef playerRef,
                               BossConfig config, double healthPct, double distSq) {
        BossHealthHud newHud = new BossHealthHud(playerRef, config.getDisplayName(),
                healthPct, config.getHudStyle());
        newHud.currentDistanceSq = distSq;
        if (!MultipleHudAPI.get().setCustomHud(player, playerRef, newHud)) {
            player.getHudManager().setCustomHud(playerRef, newHud);
        }
        activeHuds.put(uuid, new ActiveEntry(newHud, player, playerRef));
    }
}
