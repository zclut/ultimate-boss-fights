package com.hytalezx.ultimatebossfights.Systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class DeathParticleTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ms between each re-spawn — must be less than the particle's LifeSpan
    private static final long SPAWN_INTERVAL_MS = 4999;

    private record Effect(
            WorldParticle particle,
            Vector3d pos,
            long endMs,
            AtomicLong nextSpawnMs
    ) {}

    private static final CopyOnWriteArrayList<Effect> EFFECTS = new CopyOnWriteArrayList<>();

    /**
     * @param particleId    particle system ID (filename without extension)
     * @param scale         visual scale
     * @param pos           world position to spawn at
     * @param durationSeconds how long the loop runs
     */
    public static void addEffect(String particleId, float scale, Vector3d pos, float durationSeconds) {
        long now = System.currentTimeMillis();
        WorldParticle wp = new WorldParticle(
                particleId,
                null,
                scale,
                new com.hypixel.hytale.protocol.Vector3f(),
                new com.hypixel.hytale.protocol.Direction()
        );
        EFFECTS.add(new Effect(
                wp,
                new Vector3d(pos.x, pos.y, pos.z),
                now + (long) (durationSeconds * 1000L),
                new AtomicLong(now)
        ));
    }

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

        if (EFFECTS.isEmpty()) return;

        long now = System.currentTimeMillis();
        EFFECTS.removeIf(e -> now >= e.endMs);

        EntityStore entityStore = (EntityStore) store.getExternalData();
        List<Ref<EntityStore>> playerRefs = new ArrayList<>();
        entityStore.getWorld().getPlayerRefs().forEach(pr ->  {
            Ref<EntityStore> ref = ((PlayerRef) pr).getReference();
            if (ref != null) playerRefs.add(ref);
        });

        for (Effect e : EFFECTS) {
            long next = e.nextSpawnMs.get();
            if (now >= next && e.nextSpawnMs.compareAndSet(next, next + SPAWN_INTERVAL_MS)) {
                try {
                    ParticleUtil.spawnParticleEffect(e.particle, e.pos, playerRefs, commandBuffer);
                } catch (Exception ex) {
                    LOGGER.atWarning().log("[DeathParticleTickSystem] spawn failed: %s", ex.getMessage());
                }
            }
        }
    }
}
