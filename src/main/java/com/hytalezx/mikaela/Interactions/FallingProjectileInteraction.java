package com.hytalezx.mikaela.Interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rains projectiles from the sky at random positions within a radius around the target.
 *
 * Every DelayBetweenProjectile seconds a warning particle is shown and a projectile
 * is spawned at a random position within Range. This repeats for Duration seconds.
 *
 * JSON usage:
 * {
 *   "Type": "HytaleZX:FallingProjectile",
 *   "ProjectileId": "Mikaela_Sky_Projectile",
 *   "Height": 40.0,
 *   "Range": 20.0,
 *   "Duration": 3.0,
 *   "DelayBetweenProjectile": 0.3,
 *   "WarningParticle": "Fire_AoE_Spawn",
 *   "WarningParticleScale": 5.0
 * }
 */
public class FallingProjectileInteraction extends SimpleInstantInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Active rain tasks keyed by NPC UUID, consumed by FallingProjectileTickSystem. */
    public static final Map<UUID, List<Task>> ACTIVE_TASKS = new ConcurrentHashMap<>();

    // ── Config ──────────────────────────────────────────────────────────────
    private String projectileId           = "Mikaela_Sky_Projectile";
    private float  height                 = 20.0f;
    private float  range                  = 8.0f;
    private float  duration               = 3.0f;
    private float  delayBetweenProjectile = 0.3f;
    private String warningParticle        = "";
    private float  warningParticleScale   = 1.0f;

    public static final BuilderCodec<FallingProjectileInteraction> CODEC =
            BuilderCodec.builder(FallingProjectileInteraction.class, FallingProjectileInteraction::new, SimpleInstantInteraction.CODEC)
                    .append(new KeyedCodec<>("ProjectileId", Codec.STRING),
                            (i, v) -> i.projectileId = v, i -> i.projectileId).add()
                    .append(new KeyedCodec<>("Height", Codec.FLOAT),
                            (i, v) -> i.height = v, i -> i.height).add()
                    .append(new KeyedCodec<>("Range", Codec.FLOAT),
                            (i, v) -> i.range = v, i -> i.range).add()
                    .append(new KeyedCodec<>("Duration", Codec.FLOAT),
                            (i, v) -> i.duration = v, i -> i.duration).add()
                    .append(new KeyedCodec<>("DelayBetweenProjectile", Codec.FLOAT),
                            (i, v) -> i.delayBetweenProjectile = v, i -> i.delayBetweenProjectile).add()
                    .append(new KeyedCodec<>("WarningParticle", Codec.STRING),
                            (i, v) -> i.warningParticle = v, i -> i.warningParticle).add()
                    .append(new KeyedCodec<>("WarningParticleScale", Codec.FLOAT),
                            (i, v) -> i.warningParticleScale = v, i -> i.warningParticleScale).add()
                    .build();

    public FallingProjectileInteraction() {}

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();

        Ref<EntityStore> targetRef = context.getTargetEntity();
        Ref<EntityStore> npcRef    = context.getOwningEntity();

        if (targetRef == null || !targetRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Skip non-player targets (Selector can hit the NPC itself)
        Player player = buffer.getComponent(targetRef, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        TransformComponent targetTransform = buffer.getComponent(
                targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUIDComponent uuidComp = buffer.getComponent(npcRef, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID npcUuid = uuidComp.getUuid();

        Vector3d targetPos = targetTransform.getPosition();

        int durationTicks = Math.max(1, Math.round(duration * 20));
        int delayTicks    = Math.max(1, Math.round(delayBetweenProjectile * 20));

        Task task = new Task(
                targetPos.x, targetPos.y, targetPos.z,
                range, height,
                durationTicks, delayTicks,
                projectileId, warningParticle, warningParticleScale,
                npcUuid
        );

        ACTIVE_TASKS.computeIfAbsent(npcUuid, k -> new ArrayList<>()).add(task);

        LOGGER.atInfo().log(
                "[FallingProjectile] queued at (%.1f,%.1f,%.1f) dur=%d delay=%d",
                targetPos.x, targetPos.y, targetPos.z, durationTicks, delayTicks);

        context.getState().state = InteractionState.Finished;
    }

    // ── Task ────────────────────────────────────────────────────────────────

    public static class Task {
        public final double centerX;
        public final double centerY;
        public final double centerZ;
        public final float  range;
        public final float  height;
        public final int    durationTicks;
        public final int    delayBetweenTicks;
        public final String projectileId;
        public final String warningParticle;
        public final float  warningParticleScale;
        public final UUID   shooterUuid;

        /** Absolute tick counter — incremented exactly once per game tick. */
        public int totalTicksElapsed = 0;

        public Task(double cx, double cy, double cz,
                    float range, float height,
                    int durationTicks, int delayBetweenTicks,
                    String projectileId, String warningParticle, float warningParticleScale,
                    UUID shooterUuid) {
            this.centerX           = cx;
            this.centerY           = cy;
            this.centerZ           = cz;
            this.range             = range;
            this.height            = height;
            this.durationTicks     = durationTicks;
            this.delayBetweenTicks = delayBetweenTicks;
            this.projectileId      = projectileId;
            this.warningParticle   = warningParticle;
            this.warningParticleScale = warningParticleScale;
            this.shooterUuid       = shooterUuid;
        }
    }
}
