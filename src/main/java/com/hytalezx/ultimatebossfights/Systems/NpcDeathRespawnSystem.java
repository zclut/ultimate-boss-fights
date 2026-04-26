package com.hytalezx.ultimatebossfights.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytalezx.ultimatebossfights.Config.BossNPCTracker;
import com.hytalezx.ultimatebossfights.Config.BossRegistry;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Every tick, checks all NPC entities for HP <= 0.
 * When detected, schedules a spawn of the configured next role.
 * Uses a processed-set to fire only once per death.
 *
 * Register in UltimateBossFight.setup():
 *   EntityStore.REGISTRY.registerSystem(new NpcDeathRespawnSystem());
 *   NpcDeathRespawnSystem.register("MyBoss_Phase1", "MyBoss_Phase2", 5.0f);
 */
public class NpcDeathRespawnSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NpcDeathRespawn-scheduler");
                t.setDaemon(true);
                return t;
            });

    // roleName -> config
    private static final Map<String, RespawnConfig> CONFIGS = new HashMap<>();

    // Refs already scheduled for respawn — prevents firing multiple times
    private final Set<Ref<EntityStore>> processed =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public record RespawnConfig(
            String nextRole,
            float spawnTimeout,
            @NullableDecl String deathParticleId,
            float deathParticleScale,
            float deathParticleDuration
    ) {}

    public static void register(String roleName, String nextRole, float spawnTimeout) {
        CONFIGS.put(roleName, new RespawnConfig(nextRole, spawnTimeout, null, 1.0f, 0.0f));
        LOGGER.atInfo().log("[NpcDeathRespawnSystem] registered: %s -> %s (%.1fs)",
                roleName, nextRole, spawnTimeout);
    }

    public static void register(String roleName, String nextRole, float spawnTimeout,
                                String deathParticleId, float deathParticleScale, float deathParticleDuration) {
        CONFIGS.put(roleName, new RespawnConfig(nextRole, spawnTimeout, deathParticleId, deathParticleScale, deathParticleDuration));
        LOGGER.atInfo().log("[NpcDeathRespawnSystem] registered: %s -> %s (%.1fs), particle=%s scale=%.1f dur=%.1fs",
                roleName, nextRole, spawnTimeout, deathParticleId, deathParticleScale, deathParticleDuration);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        // Match all entities that have both NPCEntity and EntityStatMap
        return Query.any();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void tick(float delta, int idx,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        // Skip if no respawn rules configured
        if (CONFIGS.isEmpty()) return;

        EntityStatMap statMap = chunk.getComponent(idx, EntityStatMap.getComponentType());
        if (statMap == null) return;

        var hp = statMap.get(DefaultEntityStatTypes.getHealth());
        if (hp == null || hp.asPercentage() > 0.0) return; // still alive

        Ref<EntityStore> ref = chunk.getReferenceTo(idx);

        // Already scheduled — skip
        if (!processed.add(ref)) return;

        NPCEntity npcEntity = chunk.getComponent(idx, NPCEntity.getComponentType());
        if (npcEntity == null) return;

        // Push health=0 to cache so HUD drains before entry is removed
        BossNPCTracker.markDead(ref);
        BossNPCTracker.unregister(ref);

        String roleName = NPCPlugin.get().getName(npcEntity.getRoleIndex());
        LOGGER.atInfo().log("[NpcDeathRespawnSystem] NPC dead: roleName='%s'", roleName);

        RespawnConfig config = CONFIGS.get(roleName);
        if (config == null) {
            LOGGER.atInfo().log("[NpcDeathRespawnSystem] no config for '%s', keys=%s",
                    roleName, CONFIGS.keySet());
            return;
        }

        // Capture spawn position
        TransformComponent transform = chunk.getComponent(idx, TransformComponent.getComponentType());
        Vector3d spawnPos = new Vector3d(0, 64, 0);
        Vector3f spawnRot = new Vector3f(0, 0, 0);

        if (transform != null) {
            Vector3d p = transform.getPosition();
            Vector3f r = transform.getRotation();
            spawnPos.x = p.x; spawnPos.y = p.y; spawnPos.z = p.z;
            spawnRot.x = r.x; spawnRot.y = r.y; spawnRot.z = r.z;
        }

        final String roleToSpawn  = config.nextRole();
        final long   delayMs      = (long) (config.spawnTimeout() * 1000L);
        final EntityStore entityStore = (EntityStore) store.getExternalData();

        LOGGER.atInfo().log("[NpcDeathRespawnSystem] scheduling '%s' in %.1fs",
                roleToSpawn, config.spawnTimeout());

        if (config.deathParticleId() != null && config.deathParticleDuration() > 0) {
            DeathParticleTickSystem.addEffect(
                    config.deathParticleId(),
                    config.deathParticleScale(),
                    spawnPos,
                    config.deathParticleDuration()
            );
        }

        SCHEDULER.schedule(() -> {
            try {
                entityStore.getWorld().execute(() -> {
                    Store<EntityStore> worldStore = entityStore.getWorld()
                            .getEntityStore().getStore();
                    var result = NPCPlugin.get().spawnNPC(
                            worldStore, roleToSpawn, null, spawnPos, spawnRot);
                    if (result != null) {
                        LOGGER.atInfo().log("[NpcDeathRespawnSystem] '%s' spawned", roleToSpawn);
                        var newRef = result.first();
                        var bossConfig = BossRegistry.resolve(roleToSpawn);
                        if (newRef != null && bossConfig != null) {
                            BossNPCTracker.register(newRef, bossConfig);
                        }
                    } else {
                        LOGGER.atWarning().log(
                                "[NpcDeathRespawnSystem] spawnNPC null for '%s'", roleToSpawn);
                    }
                    // Clean up processed ref after successful spawn
                    processed.remove(ref);
                });
            } catch (Exception e) {
                LOGGER.atWarning().log("[NpcDeathRespawnSystem] error: %s", e.getMessage());
                processed.remove(ref);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }
}