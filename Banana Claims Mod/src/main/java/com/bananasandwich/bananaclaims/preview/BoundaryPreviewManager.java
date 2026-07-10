package com.bananasandwich.bananaclaims.preview;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BoundaryPreviewManager {

    private final Map<UUID, PreviewSession> sessions = new HashMap<>();
    private boolean registered;

    public void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public void show(
            ServerPlayer player,
            BoundaryPreview preview
    ) {
        if (player == null
                || preview == null
                || preview.lines().isEmpty()) {
            return;
        }

        MinecraftServer server =
                player.level().getServer();

        if (server == null) {
            return;
        }

        long currentTick = server.getTickCount();

        PreviewSession session =
                new PreviewSession(
                        preview,
                        currentTick,
                        currentTick
                                + PreviewSettings.DURATION_TICKS,
                        currentTick + 1
                );

        sessions.put(
                player.getUUID(),
                session
        );

        BoundaryParticleRenderer.render(
                player,
                preview,
                0
        );
    }

    public boolean stop(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }

        return sessions.remove(playerUuid) != null;
    }

    public boolean hasActivePreview(UUID playerUuid) {
        return playerUuid != null
                && sessions.containsKey(playerUuid);
    }

    private void tick(MinecraftServer server) {
        if (sessions.isEmpty()) {
            return;
        }

        long currentTick = server.getTickCount();

        Iterator<Map.Entry<UUID, PreviewSession>> iterator =
                sessions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, PreviewSession> entry =
                    iterator.next();

            PreviewSession session =
                    entry.getValue();

            if (currentTick >= session.expiryTick()) {
                iterator.remove();
                continue;
            }

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!player.level()
                    .dimension()
                    .toString()
                    .equals(session.preview().dimension())) {
                iterator.remove();
                continue;
            }

            if (currentTick < session.nextRenderTick()) {
                continue;
            }

            long elapsedTicks =
                    currentTick - session.startTick();

            BoundaryParticleRenderer.render(
                    player,
                    session.preview(),
                    elapsedTicks
            );

            int nextInterval =
                    elapsedTicks
                            < PreviewSettings.MATERIALIZATION_TICKS
                            ? 2
                            : PreviewSettings.REFRESH_INTERVAL_TICKS;

            entry.setValue(
                    new PreviewSession(
                            session.preview(),
                            session.startTick(),
                            session.expiryTick(),
                            currentTick + nextInterval
                    )
            );
        }
    }

    private record PreviewSession(
            BoundaryPreview preview,
            long startTick,
            long expiryTick,
            long nextRenderTick
    ) {
    }
}

