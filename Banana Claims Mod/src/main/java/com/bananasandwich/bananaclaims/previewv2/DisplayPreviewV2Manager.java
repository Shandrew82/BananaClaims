package com.bananasandwich.bananaclaims.previewv2;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimChunk;
import com.bananasandwich.bananaclaims.selection.ClaimSelection;
import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class DisplayPreviewV2Manager {

    /**
     * Packet-only entity IDs are deliberately kept far above normal server
     * entity IDs. They only exist in the receiving player's client world.
     */
    private static final int PACKET_ENTITY_ID_FLOOR =
            1_500_000_000;

    private final PreviewV2ConfigManager configManager;

    private final Map<UUID, DisplaySession> sessions =
            new HashMap<>();

    private int nextPacketEntityId = Integer.MAX_VALUE;

    private boolean registered;

    public DisplayPreviewV2Manager(
            PreviewV2ConfigManager configManager
    ) {
        this.configManager =
                Objects.requireNonNull(
                        configManager,
                        "configManager"
                );
    }

    public void register() {
        if (registered) {
            return;
        }

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(
                this::tick
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                this::discardAllSessions
        );
    }

    public boolean stop(
            UUID playerUuid
    ) {
        if (playerUuid == null) {
            return false;
        }

        DisplaySession existing =
                sessions.remove(playerUuid);

        if (existing == null) {
            return false;
        }

        ServerPlayer player =
                existing.server()
                        .getPlayerList()
                        .getPlayer(playerUuid);

        removeClientDisplays(
                player,
                existing.entityIds()
        );

        return true;
    }

    public boolean showClaimDisplay(
            ServerPlayer player,
            Claim claim
    ) {
        if (player == null
                || claim == null
                || claim.getChunks().isEmpty()) {
            return false;
        }

        ServerLevel level =
                player.level();

        if (!level.dimension()
                .toString()
                .equals(claim.getDimension())) {
            return false;
        }

        MinecraftServer server =
                level.getServer();

        if (server == null) {
            return false;
        }

        List<PerimeterEdge> perimeterEdges =
                createClaimPerimeterEdges(claim);

        if (perimeterEdges.isEmpty()) {
            return false;
        }

        PreviewV2ConfigManager.Snapshot snapshot =
                configManager.snapshot();

        RenderContext context =
                new RenderContext(
                        snapshot.config(),
                        snapshot.materials()
                );

        removeExistingSession(
                player.getUUID()
        );

        List<PreviewDisplaySpec> displays =
                new ArrayList<>();

        for (PerimeterEdge edge : perimeterEdges) {
            if (edge.startZ() == edge.endZ()) {
                appendContouredXEdge(
                        level,
                        displays,
                        edge.startX(),
                        edge.endX(),
                        edge.startZ(),
                        context
                );
            } else {
                appendContouredZEdge(
                        level,
                        displays,
                        edge.startZ(),
                        edge.endZ(),
                        edge.startX(),
                        context
                );
            }
        }

        Set<PerimeterPoint> trueCorners =
                findTrueCorners(perimeterEdges);

        appendCornerAnchors(
                level,
                displays,
                trueCorners,
                context
        );

        appendGuideColumns(
                level,
                displays,
                perimeterEdges,
                trueCorners,
                context
        );

        return startSession(
                player,
                server,
                displays,
                context.config()
        );
    }

    public boolean showSelectionDisplay(
            ServerPlayer player,
            ClaimSelection selection
    ) {
        if (player == null
                || selection == null
                || !selection.hasBothPositions()
                || !selection.isSameDimension()) {
            return false;
        }

        ServerLevel level =
                player.level();

        String currentDimension =
                level.dimension()
                        .toString();

        if (!currentDimension.equals(
                selection.getPos1Dimension()
        )) {
            return false;
        }

        MinecraftServer server =
                level.getServer();

        if (server == null) {
            return false;
        }

        int minX =
                Math.min(
                        selection.getPos1().getX(),
                        selection.getPos2().getX()
                );

        int maxX =
                Math.max(
                        selection.getPos1().getX(),
                        selection.getPos2().getX()
                ) + 1;

        int minZ =
                Math.min(
                        selection.getPos1().getZ(),
                        selection.getPos2().getZ()
                );

        int maxZ =
                Math.max(
                        selection.getPos1().getZ(),
                        selection.getPos2().getZ()
                ) + 1;

        return showRectangleDisplay(
                player,
                level,
                server,
                minX,
                maxX,
                minZ,
                maxZ
        );
    }

    public boolean showTestDisplay(
            ServerPlayer player
    ) {
        if (player == null) {
            return false;
        }

        ServerLevel level =
                player.level();

        MinecraftServer server =
                level.getServer();

        if (server == null) {
            return false;
        }

        PreviewV2Config config =
                configManager.snapshot()
                        .config();

        Vec3 horizontalLook =
                new Vec3(
                        player.getLookAngle().x,
                        0.0D,
                        player.getLookAngle().z
                );

        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        } else {
            horizontalLook =
                    horizontalLook.normalize();
        }

        int centerX =
                (int) Math.floor(
                        player.getX()
                                + horizontalLook.x
                                * config.getTestPreviewDistance()
                );

        int centerZ =
                (int) Math.floor(
                        player.getZ()
                                + horizontalLook.z
                                * config.getTestPreviewDistance()
                );

        int halfSize =
                config.getTestPreviewSize() / 2;

        int minX =
                centerX - halfSize;

        int maxX =
                minX + config.getTestPreviewSize();

        int minZ =
                centerZ - halfSize;

        int maxZ =
                minZ + config.getTestPreviewSize();

        return showRectangleDisplay(
                player,
                level,
                server,
                minX,
                maxX,
                minZ,
                maxZ
        );
    }

    private boolean showRectangleDisplay(
            ServerPlayer player,
            ServerLevel level,
            MinecraftServer server,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        PreviewV2ConfigManager.Snapshot snapshot =
                configManager.snapshot();

        RenderContext context =
                new RenderContext(
                        snapshot.config(),
                        snapshot.materials()
                );

        removeExistingSession(
                player.getUUID()
        );

        List<PreviewDisplaySpec> displays =
                new ArrayList<>();

        appendContouredXEdge(
                level,
                displays,
                minX,
                maxX,
                minZ,
                context
        );

        appendContouredXEdge(
                level,
                displays,
                minX,
                maxX,
                maxZ,
                context
        );

        appendContouredZEdge(
                level,
                displays,
                minZ,
                maxZ,
                minX,
                context
        );

        appendContouredZEdge(
                level,
                displays,
                minZ,
                maxZ,
                maxX,
                context
        );

        Set<PerimeterPoint> rectangleCorners =
                Set.of(
                        new PerimeterPoint(minX, minZ),
                        new PerimeterPoint(maxX, minZ),
                        new PerimeterPoint(maxX, maxZ),
                        new PerimeterPoint(minX, maxZ)
                );

        List<PerimeterEdge> rectangleEdges =
                List.of(
                        new PerimeterEdge(
                                minX,
                                minZ,
                                maxX,
                                minZ
                        ),
                        new PerimeterEdge(
                                maxX,
                                minZ,
                                maxX,
                                maxZ
                        ),
                        new PerimeterEdge(
                                minX,
                                maxZ,
                                maxX,
                                maxZ
                        ),
                        new PerimeterEdge(
                                minX,
                                minZ,
                                minX,
                                maxZ
                        )
                );

        appendCornerAnchors(
                level,
                displays,
                rectangleCorners,
                context
        );

        appendGuideColumns(
                level,
                displays,
                rectangleEdges,
                rectangleCorners,
                context
        );

        return startSession(
                player,
                server,
                displays,
                context.config()
        );
    }

    private static void appendContouredXEdge(
            ServerLevel level,
            List<PreviewDisplaySpec> displays,
            int minX,
            int maxX,
            int z,
            RenderContext context
    ) {
        PreviewV2Config.BorderSettings border =
                context.config()
                        .getBorder();

        int runStartX =
                minX;

        int runHeight =
                terrainHeight(
                        level,
                        minX,
                        z,
                        context.config()
                                .getTerrain()
                );

        for (int x = minX + 1;
             x <= maxX;
             x++) {
            int height =
                    terrainHeight(
                            level,
                            x,
                            z,
                            context.config()
                                    .getTerrain()
                    );

            boolean reachedEnd =
                    x == maxX;

            if (height == runHeight
                    && !reachedEnd) {
                continue;
            }

            int runEndX =
                    reachedEnd
                            && height == runHeight
                            ? maxX
                            : x;

            addDisplay(
                    displays,
                    runStartX,
                    runHeight
                            + border.getTerrainOffset(),
                    z
                            - border.getThickness() / 2.0D,
                    runEndX - runStartX,
                    border.getHeight(),
                    border.getThickness(),
                    context.materials()
                            .getBorderState(),
                    border.isGlowEnabled(),
                    context.config()
            );

            runStartX = x;
            runHeight = height;
        }
    }

    private static void appendContouredZEdge(
            ServerLevel level,
            List<PreviewDisplaySpec> displays,
            int minZ,
            int maxZ,
            int x,
            RenderContext context
    ) {
        PreviewV2Config.BorderSettings border =
                context.config()
                        .getBorder();

        int runStartZ =
                minZ;

        int runHeight =
                terrainHeight(
                        level,
                        x,
                        minZ,
                        context.config()
                                .getTerrain()
                );

        for (int z = minZ + 1;
             z <= maxZ;
             z++) {
            int height =
                    terrainHeight(
                            level,
                            x,
                            z,
                            context.config()
                                    .getTerrain()
                    );

            boolean reachedEnd =
                    z == maxZ;

            if (height == runHeight
                    && !reachedEnd) {
                continue;
            }

            int runEndZ =
                    reachedEnd
                            && height == runHeight
                            ? maxZ
                            : z;

            addDisplay(
                    displays,
                    x
                            - border.getThickness() / 2.0D,
                    runHeight
                            + border.getTerrainOffset(),
                    runStartZ,
                    border.getThickness(),
                    border.getHeight(),
                    runEndZ - runStartZ,
                    context.materials()
                            .getBorderState(),
                    border.isGlowEnabled(),
                    context.config()
            );

            runStartZ = z;
            runHeight = height;
        }
    }

    private static int terrainHeight(
            ServerLevel level,
            int x,
            int z,
            PreviewV2Config.TerrainSettings terrain
    ) {
        int topY =
                level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING,
                        x,
                        z
                ) - 1;

        int minimumY =
                level.getMinY();

        BlockPos.MutableBlockPos position =
                new BlockPos.MutableBlockPos(
                        x,
                        topY,
                        z
                );

        while (topY >= minimumY) {
            position.set(
                    x,
                    topY,
                    z
            );

            BlockState blockState =
                    level.getBlockState(position);

            boolean ignoredLog =
                    terrain.isIgnoreLogs()
                            && blockState.is(BlockTags.LOGS);

            boolean ignoredLeaves =
                    terrain.isIgnoreLeaves()
                            && blockState.is(BlockTags.LEAVES);

            if (!ignoredLog
                    && !ignoredLeaves) {
                return topY + 1;
            }

            topY--;
        }

        return minimumY;
    }

    private static void addDisplay(
            List<PreviewDisplaySpec> displays,
            double x,
            double y,
            double z,
            float scaleX,
            float scaleY,
            float scaleZ,
            BlockState blockState,
            boolean glowEnabled,
            PreviewV2Config config
    ) {
        if (scaleX <= 0.0F
                || scaleY <= 0.0F
                || scaleZ <= 0.0F) {
            return;
        }

        displays.add(
                new PreviewDisplaySpec(
                        x,
                        y,
                        z,
                        scaleX,
                        scaleY,
                        scaleZ,
                        blockState,
                        config.isGlowEnabled()
                                && glowEnabled
                )
        );
    }

    private static void appendGuideColumns(
            ServerLevel level,
            List<PreviewDisplaySpec> displays,
            List<PerimeterEdge> edges,
            Set<PerimeterPoint> cornerPoints,
            RenderContext context
    ) {
        PreviewV2Config.GuideSettings guides =
                context.config()
                        .getGuides();

        if (!guides.isEnabled()) {
            return;
        }

        Set<PerimeterPoint> guidePoints =
                new LinkedHashSet<>();

        for (PerimeterEdge edge : edges) {
            int deltaX =
                    Integer.signum(
                            edge.endX() - edge.startX()
                    );

            int deltaZ =
                    Integer.signum(
                            edge.endZ() - edge.startZ()
                    );

            int length =
                    Math.abs(
                            edge.endX() - edge.startX()
                    )
                            + Math.abs(
                            edge.endZ() - edge.startZ()
                    );

            for (int offset = guides.getSpacing();
                 offset < length;
                 offset += guides.getSpacing()) {
                guidePoints.add(
                        new PerimeterPoint(
                                edge.startX()
                                        + deltaX * offset,
                                edge.startZ()
                                        + deltaZ * offset
                        )
                );
            }
        }

        guidePoints.removeAll(cornerPoints);

        for (PerimeterPoint guidePoint : guidePoints) {
            appendFullHeightGuideColumn(
                    level,
                    displays,
                    guidePoint,
                    context
            );
        }
    }

    private static void appendFullHeightGuideColumn(
            ServerLevel level,
            List<PreviewDisplaySpec> displays,
            PerimeterPoint point,
            RenderContext context
    ) {
        PreviewV2Config config =
                context.config();

        PreviewV2Config.BorderSettings border =
                config.getBorder();

        PreviewV2Config.GuideSettings guides =
                config.getGuides();

        int surfaceY =
                terrainHeight(
                        level,
                        point.x(),
                        point.z(),
                        config.getTerrain()
                );

        double upperY =
                surfaceY
                        + border.getTerrainOffset()
                        + border.getHeight();

        float upperHeight =
                (float) Math.max(
                        config.getMinimumColumnHeight(),
                        level.getMaxY() - upperY
                );

        addDisplay(
                displays,
                point.x()
                        - guides.getWidth() / 2.0D,
                upperY,
                point.z()
                        - guides.getWidth() / 2.0D,
                guides.getWidth(),
                upperHeight,
                guides.getWidth(),
                context.materials()
                        .getGuideState(),
                guides.isGlowEnabled(),
                config
        );

        double lowerY =
                level.getMinY();

        float lowerHeight =
                (float) Math.max(
                        config.getMinimumColumnHeight(),
                        surfaceY
                                + border.getTerrainOffset()
                                - lowerY
                );

        addDisplay(
                displays,
                point.x()
                        - guides.getWidth() / 2.0D,
                lowerY,
                point.z()
                        - guides.getWidth() / 2.0D,
                guides.getWidth(),
                lowerHeight,
                guides.getWidth(),
                context.materials()
                        .getGuideState(),
                guides.isGlowEnabled(),
                config
        );
    }

    private static void appendCornerAnchors(
            ServerLevel level,
            List<PreviewDisplaySpec> displays,
            Set<PerimeterPoint> corners,
            RenderContext context
    ) {
        PreviewV2Config config =
                context.config();

        PreviewV2Config.BorderSettings border =
                config.getBorder();

        PreviewV2Config.CornerSettings cornerSettings =
                config.getCorners();

        for (PerimeterPoint corner : corners) {
            int surfaceY =
                    terrainHeight(
                            level,
                            corner.x(),
                            corner.z(),
                            config.getTerrain()
                    );

            addDisplay(
                    displays,
                    corner.x()
                            - cornerSettings.getSize() / 2.0D,
                    surfaceY
                            + border.getTerrainOffset(),
                    corner.z()
                            - cornerSettings.getSize() / 2.0D,
                    cornerSettings.getSize(),
                    cornerSettings.getHeight(),
                    cornerSettings.getSize(),
                    context.materials()
                            .getCornerState(),
                    cornerSettings.isAnchorGlowEnabled(),
                    config
            );

            if (!cornerSettings.isColumnsEnabled()) {
                continue;
            }

            double upperColumnY =
                    surfaceY
                            + border.getTerrainOffset()
                            + cornerSettings.getHeight()
                            + cornerSettings.getColumnGap();

            float upperColumnHeight =
                    (float) Math.max(
                            config.getMinimumColumnHeight(),
                            level.getMaxY() - upperColumnY
                    );

            addDisplay(
                    displays,
                    corner.x()
                            - cornerSettings.getColumnThickness()
                            / 2.0D,
                    upperColumnY,
                    corner.z()
                            - cornerSettings.getColumnThickness()
                            / 2.0D,
                    cornerSettings.getColumnThickness(),
                    upperColumnHeight,
                    cornerSettings.getColumnThickness(),
                    context.materials()
                            .getCornerState(),
                    cornerSettings.isColumnGlowEnabled(),
                    config
            );

            double lowerColumnY =
                    level.getMinY();

            float lowerColumnHeight =
                    (float) Math.max(
                            config.getMinimumColumnHeight(),
                            surfaceY
                                    + border.getTerrainOffset()
                                    - lowerColumnY
                    );

            addDisplay(
                    displays,
                    corner.x()
                            - cornerSettings.getColumnThickness()
                            / 2.0D,
                    lowerColumnY,
                    corner.z()
                            - cornerSettings.getColumnThickness()
                            / 2.0D,
                    cornerSettings.getColumnThickness(),
                    lowerColumnHeight,
                    cornerSettings.getColumnThickness(),
                    context.materials()
                            .getCornerState(),
                    cornerSettings.isColumnGlowEnabled(),
                    config
            );
        }
    }

    private static Set<PerimeterPoint> findTrueCorners(
            List<PerimeterEdge> edges
    ) {
        Map<PerimeterPoint, Integer> directionMasks =
                new HashMap<>();

        for (PerimeterEdge edge : edges) {
            int directionMask =
                    edge.startZ() == edge.endZ()
                            ? 1
                            : 2;

            directionMasks.merge(
                    new PerimeterPoint(
                            edge.startX(),
                            edge.startZ()
                    ),
                    directionMask,
                    (left, right) -> left | right
            );

            directionMasks.merge(
                    new PerimeterPoint(
                            edge.endX(),
                            edge.endZ()
                    ),
                    directionMask,
                    (left, right) -> left | right
            );
        }

        Set<PerimeterPoint> corners =
                new LinkedHashSet<>();

        for (Map.Entry<PerimeterPoint, Integer> entry
                : directionMasks.entrySet()) {
            if (entry.getValue() == 3) {
                corners.add(entry.getKey());
            }
        }

        return corners;
    }

    private static List<PerimeterEdge> createClaimPerimeterEdges(
            Claim claim
    ) {
        Set<PerimeterEdge> edges =
                new HashSet<>();

        for (ClaimChunk chunk : claim.getChunks()) {
            int minX =
                    chunk.getChunkX() * 16;

            int minZ =
                    chunk.getChunkZ() * 16;

            int maxX =
                    minX + 16;

            int maxZ =
                    minZ + 16;

            toggleEdge(
                    edges,
                    new PerimeterEdge(
                            minX,
                            minZ,
                            maxX,
                            minZ
                    )
            );

            toggleEdge(
                    edges,
                    new PerimeterEdge(
                            maxX,
                            minZ,
                            maxX,
                            maxZ
                    )
            );

            toggleEdge(
                    edges,
                    new PerimeterEdge(
                            minX,
                            maxZ,
                            maxX,
                            maxZ
                    )
            );

            toggleEdge(
                    edges,
                    new PerimeterEdge(
                            minX,
                            minZ,
                            minX,
                            maxZ
                    )
            );
        }

        return new ArrayList<>(
                new LinkedHashSet<>(edges)
        );
    }

    private static void toggleEdge(
            Set<PerimeterEdge> edges,
            PerimeterEdge edge
    ) {
        PerimeterEdge normalized =
                edge.normalized();

        if (!edges.remove(normalized)) {
            edges.add(normalized);
        }
    }

    private void discardAllSessions(
            MinecraftServer server
    ) {
        // Packet-only displays are owned by each connected client. Closing the
        // connection clears them automatically, so shutdown only drops the
        // server-side bookkeeping.
        sessions.clear();
    }

    private boolean startSession(
            ServerPlayer player,
            MinecraftServer server,
            List<PreviewDisplaySpec> displays,
            PreviewV2Config config
    ) {
        if (displays.isEmpty()) {
            return false;
        }

        List<Integer> entityIds =
                new ArrayList<>(displays.size());

        try {
            for (PreviewDisplaySpec display : displays) {
                int entityId =
                        allocatePacketEntityId();

                entityIds.add(entityId);

                sendClientDisplay(
                        player,
                        player.level(),
                        entityId,
                        display,
                        config
                );
            }
        } catch (RuntimeException exception) {
            removeClientDisplays(
                    player,
                    entityIds
            );

            Bananaclaims.LOGGER.error(
                    "Failed to send Renderer V2 packet display to '{}'.",
                    player.getScoreboardName(),
                    exception
            );

            return false;
        }

        long currentTick =
                server.getTickCount();

        sessions.put(
                player.getUUID(),
                new DisplaySession(
                        server,
                        player.level()
                                .dimension()
                                .toString(),
                        List.copyOf(entityIds),
                        currentTick
                                + config.getDurationTicks()
                )
        );

        return true;
    }

    private void sendClientDisplay(
            ServerPlayer player,
            ServerLevel level,
            int entityId,
            PreviewDisplaySpec spec,
            PreviewV2Config config
    ) {
        Display.BlockDisplay display =
                new Display.BlockDisplay(
                        EntityTypes.BLOCK_DISPLAY,
                        level
                );

        display.setId(entityId);
        display.setUUID(UUID.randomUUID());
        display.setPos(
                spec.x(),
                spec.y(),
                spec.z()
        );
        display.setBlockState(
                spec.blockState()
        );
        display.setTransformation(
                new Transformation(
                        new Vector3f(),
                        new Quaternionf(),
                        new Vector3f(
                                spec.scaleX(),
                                spec.scaleY(),
                                spec.scaleZ()
                        ),
                        new Quaternionf()
                )
        );
        display.setViewRange(
                config.getViewRange()
        );
        display.setShadowRadius(
                config.getShadowRadius()
        );
        display.setShadowStrength(
                config.getShadowStrength()
        );
        display.setWidth(
                Math.max(
                        spec.scaleX(),
                        spec.scaleZ()
                )
        );
        display.setHeight(
                spec.scaleY()
        );
        display.setGlowingTag(
                spec.glowEnabled()
        );
        display.setGlowColorOverride(
                config.getGlowColorRgb()
        );
        display.setNoGravity(true);

        player.connection.send(
                new ClientboundAddEntityPacket(
                        display.getId(),
                        display.getUUID(),
                        spec.x(),
                        spec.y(),
                        spec.z(),
                        0.0F,
                        0.0F,
                        EntityTypes.BLOCK_DISPLAY,
                        0,
                        Vec3.ZERO,
                        0.0D
                )
        );

        List<SynchedEntityData.DataValue<?>> packedData =
                display.getEntityData()
                        .getNonDefaultValues();

        if (packedData == null
                || packedData.isEmpty()) {
            return;
        }

        player.connection.send(
                new ClientboundSetEntityDataPacket(
                        entityId,
                        packedData
                )
        );
    }

    private int allocatePacketEntityId() {
        if (nextPacketEntityId
                <= PACKET_ENTITY_ID_FLOOR) {
            nextPacketEntityId =
                    Integer.MAX_VALUE;
        }

        return nextPacketEntityId--;
    }

    private void tick(
            MinecraftServer server
    ) {
        if (sessions.isEmpty()) {
            return;
        }

        long currentTick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, DisplaySession>> iterator =
                sessions.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DisplaySession> entry =
                    iterator.next();

            DisplaySession session =
                    entry.getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            String currentDimension =
                    player.level()
                            .dimension()
                            .toString();

            if (!currentDimension.equals(
                    session.dimension()
            )) {
                // A client level change clears the packet-only entities.
                iterator.remove();
                continue;
            }

            if (currentTick
                    < session.expiryTick()) {
                continue;
            }

            removeClientDisplays(
                    player,
                    session.entityIds()
            );

            iterator.remove();
        }
    }

    private void removeExistingSession(
            UUID playerUuid
    ) {
        DisplaySession existing =
                sessions.remove(playerUuid);

        if (existing == null) {
            return;
        }

        ServerPlayer player =
                existing.server()
                        .getPlayerList()
                        .getPlayer(playerUuid);

        removeClientDisplays(
                player,
                existing.entityIds()
        );
    }

    private static void removeClientDisplays(
            ServerPlayer player,
            List<Integer> entityIds
    ) {
        if (player == null
                || entityIds == null
                || entityIds.isEmpty()) {
            return;
        }

        int[] packedIds =
                new int[entityIds.size()];

        for (int index = 0;
             index < entityIds.size();
             index++) {
            packedIds[index] =
                    entityIds.get(index);
        }

        player.connection.send(
                new ClientboundRemoveEntitiesPacket(
                        packedIds
                )
        );
    }

    private record RenderContext(
            PreviewV2Config config,
            PreviewV2Materials materials
    ) {
    }

    private record PerimeterPoint(
            int x,
            int z
    ) {
    }

    private record PerimeterEdge(
            int startX,
            int startZ,
            int endX,
            int endZ
    ) {
        private PerimeterEdge normalized() {
            if (startX < endX
                    || (
                    startX == endX
                            && startZ <= endZ
            )) {
                return this;
            }

            return new PerimeterEdge(
                    endX,
                    endZ,
                    startX,
                    startZ
            );
        }
    }

    private record PreviewDisplaySpec(
            double x,
            double y,
            double z,
            float scaleX,
            float scaleY,
            float scaleZ,
            BlockState blockState,
            boolean glowEnabled
    ) {
        private PreviewDisplaySpec {
            Objects.requireNonNull(
                    blockState,
                    "blockState"
            );
        }
    }

    private record DisplaySession(
            MinecraftServer server,
            String dimension,
            List<Integer> entityIds,
            long expiryTick
    ) {
        private DisplaySession {
            Objects.requireNonNull(
                    server,
                    "server"
            );
            Objects.requireNonNull(
                    dimension,
                    "dimension"
            );
            entityIds =
                    List.copyOf(entityIds);
        }
    }
}
