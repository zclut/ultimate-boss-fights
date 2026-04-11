package com.hytalezx.mikaela.Systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hytalezx.mikaela.Interactions.FallingProjectileInteraction;
import com.hytalezx.mikaela.Interactions.FallingProjectileInteraction.Task;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class FallingProjectileTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int MAX_POSITION_ATTEMPTS = 10;

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
        if (npc == null) return;

        UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(idx, UUIDComponent.getComponentType());
        if (uuidComp == null) return;
        UUID npcUuid = uuidComp.getUuid();

        List<Task> tasks = FallingProjectileInteraction.ACTIVE_TASKS.get(npcUuid);
        if (tasks == null || tasks.isEmpty()) return;

        TimeResource timeResource = commandBuffer.getResource(TimeResource.getResourceType());

        tasks.removeIf(task -> {
            task.totalTicksElapsed++;
            int t = task.totalTicksElapsed;

            // Fire on tick 1, then every delayBetweenTicks after that
            if (t <= task.durationTicks && (t == 1 || (t - 1) % task.delayBetweenTicks == 0)) {
                double[] landing = pickLanding(task);
                spawnWarningParticle(task, landing[0], landing[1], commandBuffer);
                doSpawnProjectile(task, landing[0], landing[1], timeResource, commandBuffer);
            }

            return t >= task.durationTicks;
        });

        if (tasks.isEmpty()) {
            FallingProjectileInteraction.ACTIVE_TASKS.remove(npcUuid);
        }
    }

    private void spawnWarningParticle(Task task, double landX, double landZ,
                                      CommandBuffer<EntityStore> commandBuffer) {
        if (task.warningParticle == null || task.warningParticle.isEmpty()) return;
        try {
            ParticleUtil.spawnParticleEffect(
                    task.warningParticle,
                    new Vector3d(landX, task.centerY, landZ),
                    commandBuffer);
        } catch (Exception e) {
            LOGGER.atWarning().log("[FallingProjectile] warning particle failed: %s", e.getMessage());
        }
    }

    private double[] pickLanding(Task task) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle  = rng.nextDouble() * 2.0 * Math.PI;
        double radius = rng.nextDouble() * task.range;
        return new double[]{
                task.centerX + Math.cos(angle) * radius,
                task.centerZ + Math.sin(angle) * radius
        };
    }

    private void doSpawnProjectile(Task task, double landX, double landZ,
                                   TimeResource timeResource,
                                   CommandBuffer<EntityStore> commandBuffer) {

        double landY = task.centerY;

        Vector3d spawnPos = new Vector3d(landX, landY + task.height, landZ);
        Vector3f spawnRot = new Vector3f(0, -90, 0);

        Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(
                timeResource, task.projectileId, spawnPos, spawnRot);

        ProjectileComponent projComp = holder.getComponent(ProjectileComponent.getComponentType());
        if (projComp == null) {
            LOGGER.atWarning().log("[FallingProjectile] failed to assemble projectile '%s'", task.projectileId);
            return;
        }

        holder.ensureComponent(Intangible.getComponentType());

        if (projComp.getProjectile() == null) {
            projComp.initialize();
            if (projComp.getProjectile() == null) {
                LOGGER.atWarning().log("[FallingProjectile] projectile config '%s' not found", task.projectileId);
                return;
            }
        }

        projComp.shoot(holder, task.shooterUuid,
                spawnPos.x, spawnPos.y, spawnPos.z,
                0f, -90f);

        // Override position to exact spawn point (undo computeStartOffset)
        TransformComponent projTransform = holder.getComponent(TransformComponent.getComponentType());
        if (projTransform != null) {
            projTransform.setPosition(new Vector3d(landX, landY + task.height, landZ));
        }

        // Straight down at MuzzleVelocity from config
        double speed = projComp.getProjectile().getMuzzleVelocity();
        projComp.getSimplePhysicsProvider().setVelocity(new Vector3d(0, -speed, 0));

        commandBuffer.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("[FallingProjectile] spawned -> land (%.1f, %.1f, %.1f) speed=%.1f",
                landX, landY, landZ, speed);
    }
}
