package com.hytalezx.ultimatebossfights.Utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class EntityUtils {

    private EntityUtils() {}

    public static UUID getUuid(PlayerRef playerRef) {
        if (playerRef == null) return null;
        return getUuid(playerRef.getReference());
    }

    public static UUID getUuid(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) return null;
        UUIDComponent comp = (UUIDComponent) ref.getStore()
                .getComponent(ref, UUIDComponent.getComponentType());
        return comp != null ? comp.getUuid() : null;
    }
}
