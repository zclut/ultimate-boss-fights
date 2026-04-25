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

        // If showing a boss that's no longer tracked, clear the HUD immediately
        CustomUIHud showing = hud.getCustomHud();
        if (showing instanceof BossHealthHud) {
            String showingName = ((BossHealthHud) showing).getBossName();
            boolean stillTracked = false;
            for (BossNPCTracker.Entry e : BossNPCTracker.entries()) {
                if (e.config().getDisplayName().equals(showingName)) {
                    stillTracked = true;
                    break;
                }
            }
            if (!stillTracked) {
                hud.setCustomHud(playerRef, new EmptyHud(playerRef));
            }
        }

        for (BossNPCTracker.Entry entry : BossNPCTracker.entries()) {
            Ref<EntityStore> npcRef = entry.ref();
            BossConfig config       = entry.config();

            if (!npcRef.isValid()) {
                BossNPCTracker.unregister(npcRef);
                continue;
            }

            // Player left the instance — hide HUD and skip
            if (entry.npcWorld() != null && entry.npcWorld() != player.getWorld()) {
                CustomUIHud current = hud.getCustomHud();
                if (current instanceof BossHealthHud
                        && ((BossHealthHud) current).getBossName().equals(config.getDisplayName())) {
                    hud.setCustomHud(playerRef, new EmptyHud(playerRef));
                }
                continue;
            }

            double healthPct = entry.cachedHealthPct();
            double dx        = entry.cachedX() - playerPos.x;
            double dz        = entry.cachedZ() - playerPos.z;
            double distSq    = dx * dx + dz * dz;

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
}
