package com.bananasandwich.bananaclaims.previewv2;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DisplayPreviewV2Manager {

    private static final int TEST_DURATION_TICKS =
            10 * 20;

    private static final int RECTANGLE_SIZE =
            8;

    private static final float BORDER_HEIGHT =
            0.35F;

    private static final float BORDER_THICKNESS =
            0.35F;

    private static final double TERRAIN_OFFSET =
            0.18D;

    private final Map<UUID, TestDisplaySession> sessions =
            new HashMap<>();

    private boolean registered;

    public void register() {
        if (registered) {
            return;
        }

        registered = true;

        ServerTickEvents.END_SERVER_TICK.register(
                this::tick
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

        removeExistingSession(
                player.getUUID()
        );

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
                                + horizontalLook.x * 7.0D
                );

        int centerZ =
                (int) Math.floor(
                        player.getZ()
                                + horizontalLook.z * 7.0D
                );

        int halfSize =
                RECTANGLE_SIZE / 2;

        int minX =
                centerX - halfSize;

        int maxX =
                minX + RECTANGLE_SIZE;

        int minZ =
                centerZ - halfSize;

        int maxZ =
                minZ + RECTANGLE_SIZE;

        List<Display.BlockDisplay> displays =
                new ArrayList<>();

        appendContouredXEdge(
                level,
                displays,
                minX,
                maxX,
                minZ
        );

        appendContouredXEdge(
                level,
                displays,
                minX,
                maxX,
                maxZ
        );

        appendContouredZEdge(
                level,
                displays,
                minZ,
                maxZ,
                minX
        );

        appendContouredZEdge(
                level,
                displays,
                minZ,
                maxZ,
                maxX
        );

        if (displays.isEmpty()) {
            return false;
        }

        long currentTick =
                server.getTickCount();

        sessions.put(
                player.getUUID(),
                new TestDisplaySession(
                        List.copyOf(displays),
                        currentTick
                                + TEST_DURATION_TICKS
                )
        );

        return true;
    }

    private static void appendContouredXEdge(
            ServerLevel level,
            List<Display.BlockDisplay> displays,
            int minX,
            int maxX,
            int z
    ) {
        int runStartX =
                minX;

        int runHeight =
                terrainHeight(
                        level,
                        minX,
                        z
                );

        for (int x = minX + 1;
             x <= maxX;
             x++) {
            int height =
                    terrainHeight(
                            level,
                            x,
                            z
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

            addBeam(
                    level,
                    displays,
                    runStartX,
                    runHeight
                            + TERRAIN_OFFSET,
                    z
                            - BORDER_THICKNESS / 2.0D,
                    runEndX - runStartX,
                    BORDER_HEIGHT,
                    BORDER_THICKNESS
            );

            runStartX = x;
            runHeight = height;
        }
    }

    private static void appendContouredZEdge(
            ServerLevel level,
            List<Display.BlockDisplay> displays,
            int minZ,
            int maxZ,
            int x
    ) {
        int runStartZ =
                minZ;

        int runHeight =
                terrainHeight(
                        level,
                        x,
                        minZ
                );

        for (int z = minZ + 1;
             z <= maxZ;
             z++) {
            int height =
                    terrainHeight(
                            level,
                            x,
                            z
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

            addBeam(
                    level,
                    displays,
                    x
                            - BORDER_THICKNESS / 2.0D,
                    runHeight
                            + TERRAIN_OFFSET,
                    runStartZ,
                    BORDER_THICKNESS,
                    BORDER_HEIGHT,
                    runEndZ - runStartZ
            );

            runStartZ = z;
            runHeight = height;
        }
    }

    private static int terrainHeight(
            ServerLevel level,
            int x,
            int z
    ) {
        return level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );
    }

    private static void addBeam(
            ServerLevel level,
            List<Display.BlockDisplay> displays,
            double x,
            double y,
            double z,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        if (scaleX <= 0.0F
                || scaleY <= 0.0F
                || scaleZ <= 0.0F) {
            return;
        }

        Display.BlockDisplay display =
                new Display.BlockDisplay(
                        EntityTypes.BLOCK_DISPLAY,
                        level
                );

        display.setPos(
                x,
                y,
                z
        );

        display.setBlockState(
                Blocks.AMETHYST_BLOCK
                        .defaultBlockState()
        );

        display.setTransformation(
                new Transformation(
                        new Vector3f(),
                        new Quaternionf(),
                        new Vector3f(
                                scaleX,
                                scaleY,
                                scaleZ
                        ),
                        new Quaternionf()
                )
        );

        display.setViewRange(4.0F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(
                Math.max(
                        scaleX,
                        scaleZ
                )
        );
        display.setHeight(scaleY);
        display.setGlowingTag(true);
        display.setGlowColorOverride(0xA855F7);
        display.setNoGravity(true);

        boolean added =
                level.addFreshEntity(display);

        if (!added) {
            display.discard();
            return;
        }

        displays.add(display);
    }

    private void tick(
            MinecraftServer server
    ) {
        if (sessions.isEmpty()) {
            return;
        }

        long currentTick =
                server.getTickCount();

        Iterator<Map.Entry<UUID, TestDisplaySession>> iterator =
                sessions.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TestDisplaySession> entry =
                    iterator.next();

            TestDisplaySession session =
                    entry.getValue();

            if (currentTick
                    < session.expiryTick()) {
                continue;
            }

            discardAll(
                    session.displays()
            );

            iterator.remove();
        }
    }

    private void removeExistingSession(
            UUID playerUuid
    ) {
        TestDisplaySession existing =
                sessions.remove(playerUuid);

        if (existing == null) {
            return;
        }

        discardAll(
                existing.displays()
        );
    }

    private static void discardAll(
            List<Display.BlockDisplay> displays
    ) {
        if (displays == null) {
            return;
        }

        for (Display.BlockDisplay display : displays) {
            if (display != null
                    && !display.isRemoved()) {
                display.discard();
            }
        }
    }

    private record TestDisplaySession(
            List<Display.BlockDisplay> displays,
            long expiryTick
    ) {
        private TestDisplaySession {
            displays = List.copyOf(displays);
        }
    }
}
