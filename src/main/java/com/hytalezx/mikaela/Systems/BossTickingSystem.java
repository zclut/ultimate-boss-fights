package com.hytalezx.mikaela.Systems;


import com.hytalezx.mikaela.Config.BossConfig;
import com.hytalezx.mikaela.Config.BossRegistry;
import com.hytalezx.mikaela.UI.BossHealthHud;
import com.hytalezx.mikaela.UI.EmptyHud;
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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BossTickingSystem extends EntityTickingSystem<EntityStore> {


    @NullableDecl
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    public void tick(float delta, int idx,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        // 1. ¿Es un boss registrado?
        NPCEntity npc = (NPCEntity) chunk.getComponent(idx, NPCEntity.getComponentType());
        if (npc == null || npc.getWorld() == null) return;

        BossConfig config = BossRegistry.resolve(npc.getRoleName());
        if (config == null) return;

        // 2. Stats de salud
        EntityStatMap statMap = (EntityStatMap) chunk.getComponent(idx, EntityStatMap.getComponentType());
        if (statMap == null) return;
        EntityStatValue healthValue = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthValue == null) return;

        // 3. Posición del NPC
        TransformComponent npcTransform = (TransformComponent) chunk.getComponent(idx, TransformComponent.getComponentType());
        if (npcTransform == null) return;
        Vector3d npcPos = npcTransform.getPosition();

        double healthPct    = healthValue.asPercentage();
        double entryRadiusSq = config.getProximityBlocks() * config.getProximityBlocks();
        double exitRadiusSq  = config.getExitBlocks()      * config.getExitBlocks();

        // 4. Iterar jugadores del mundo del NPC
        npc.getWorld().getPlayerRefs().forEach(playerRef -> {
            if (playerRef.getReference() == null) return;

            Player player = (Player) store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (player == null) return;

            TransformComponent playerTransform = (TransformComponent) store.getComponent(
                    playerRef.getReference(), TransformComponent.getComponentType());
            if (playerTransform == null) return;

            double distSq     = distanceSq(npcPos, playerTransform.getPosition());
            HudManager hud    = player.getHudManager();
            CustomUIHud current = hud.getCustomHud();

            boolean alreadyShowingThisBoss = current instanceof BossHealthHud
                    && ((BossHealthHud) current).getBossName().equals(config.getDisplayName());

            // ── Hysteresis ───────────────────────────────────────────────────
            // Radio de ENTRADA para detectar al boss por primera vez.
            // Radio de SALIDA (mayor) para perderlo → evita parpadeo en el borde.
            double activeRadiusSq = alreadyShowingThisBoss ? exitRadiusSq : entryRadiusSq;
            boolean inRange = healthPct > 0.0 && distSq <= activeRadiusSq;

            if (inRange) {
                if (alreadyShowingThisBoss) {
                    // Mismo boss → update incremental de salud
                    BossHealthHud currentBossHud = (BossHealthHud) current;
                    currentBossHud.updateHealth(healthPct);
                    currentBossHud.currentDistanceSq = distSq;
                } else if (current instanceof BossHealthHud) {
                    // Hay otro boss en pantalla → solo reemplazar si este está más cerca
                    BossHealthHud currentBossHud = (BossHealthHud) current;
                    if (distSq < currentBossHud.currentDistanceSq) {
                        setNewBossHud(hud, playerRef, config, healthPct, distSq);
                    }
                } else {
                    // ── Entry event: no había ningún boss en pantalla ─────────
                    setNewBossHud(hud, playerRef, config, healthPct, distSq);
                }
            } else {
                // Fuera de rango o boss muerto: limpiar solo si el HUD es de ESTE boss
                if (alreadyShowingThisBoss) {
                    hud.setCustomHud(playerRef, new EmptyHud(playerRef));
                }
            }
        });
    }

    private void setNewBossHud(HudManager hud, Object playerRef,
                               BossConfig config, double healthPct, double distSq) {
        BossHealthHud newHud = new BossHealthHud(
                (com.hypixel.hytale.server.core.universe.PlayerRef) playerRef,
                config.getDisplayName(),
                healthPct,
                config.getHudStyle()
        );
        newHud.currentDistanceSq = distSq;
        hud.setCustomHud((com.hypixel.hytale.server.core.universe.PlayerRef) playerRef, newHud);
    }

    private double distanceSq(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }
}