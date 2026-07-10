package com.bananasandwich.bananaclaims.integration.bluemap;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimChunk;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeEvent;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeListener;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class BlueMapBridge {

    public static final String MARKER_SET_ID = "bananaclaims";

    private static final float MARKER_HEIGHT = 64.0F;

    private static final Color BORDER_COLOR =
            new Color(35, 120, 255, 1.0F);

    private static final Color FILL_COLOR =
            new Color(35, 120, 255, 0.22F);

    private static final Map<UUID, Integer> CLAIM_FINGERPRINTS =
            new HashMap<>();

    private static BlueMapAPI activeApi;
    private static boolean registered = false;

    private static final Consumer<BlueMapAPI> ENABLE_LISTENER =
            BlueMapBridge::enable;

    private static final Consumer<BlueMapAPI> DISABLE_LISTENER =
            BlueMapBridge::disable;

    private static final ClaimChangeListener CLAIM_CHANGE_LISTENER =
            BlueMapBridge::handleClaimChange;

    private BlueMapBridge() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        BlueMapAPI.onEnable(ENABLE_LISTENER);
        BlueMapAPI.onDisable(DISABLE_LISTENER);

        Bananaclaims.CLAIM_MANAGER.addChangeListener(
                CLAIM_CHANGE_LISTENER
        );

        Bananaclaims.LOGGER.info(
                "BlueMap detected. Waiting for the BlueMap API."
        );
    }

    private static void enable(BlueMapAPI api) {
        activeApi = api;
        CLAIM_FINGERPRINTS.clear();

        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().put(
                    MARKER_SET_ID,
                    createMarkerSet()
            );
        }

        synchronizeAllClaims();

        Bananaclaims.LOGGER.info(
                "Banana Claims BlueMap integration enabled for {} map(s).",
                api.getMaps().size()
        );
    }

    private static void disable(BlueMapAPI api) {
        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().remove(MARKER_SET_ID);
        }

        CLAIM_FINGERPRINTS.clear();
        activeApi = null;

        Bananaclaims.LOGGER.info(
                "Banana Claims BlueMap integration disabled."
        );
    }

    private static void handleClaimChange(ClaimChangeEvent event) {
        if (activeApi == null || event == null) {
            return;
        }

        synchronizeAllClaims();
    }

    private static void synchronizeAllClaims() {
        BlueMapAPI api = activeApi;

        if (api == null) {
            return;
        }

        List<Claim> claims =
                Bananaclaims.CLAIM_MANAGER.getAllClaims();

        Set<UUID> activeClaimIds = new HashSet<>();

        for (Claim claim : claims) {
            UUID claimId = claim.getClaimId();
            activeClaimIds.add(claimId);

            int fingerprint = createFingerprint(claim);
            Integer previousFingerprint =
                    CLAIM_FINGERPRINTS.get(claimId);

            if (previousFingerprint != null
                    && previousFingerprint == fingerprint) {
                continue;
            }

            removeClaimMarker(api, claimId);
            addClaimMarker(api, claim);

            CLAIM_FINGERPRINTS.put(
                    claimId,
                    fingerprint
            );
        }

        List<UUID> removedClaimIds =
                CLAIM_FINGERPRINTS.keySet()
                        .stream()
                        .filter(claimId ->
                                !activeClaimIds.contains(claimId)
                        )
                        .toList();

        for (UUID removedClaimId : removedClaimIds) {
            removeClaimMarker(
                    api,
                    removedClaimId
            );

            CLAIM_FINGERPRINTS.remove(
                    removedClaimId
            );
        }
    }

    private static void addClaimMarker(
            BlueMapAPI api,
            Claim claim
    ) {
        List<Vector2d> outline = createOutline(claim);

        if (outline.size() < 3) {
            Bananaclaims.LOGGER.warn(
                    "Could not create a BlueMap outline for claim \"{}\".",
                    claim.getName()
            );
            return;
        }

        Shape shape = new Shape(outline);

        ShapeMarker marker = new ShapeMarker(
                claim.getName(),
                shape,
                MARKER_HEIGHT
        );

        marker.setLineWidth(2);
        marker.setColors(
                BORDER_COLOR,
                FILL_COLOR
        );
        marker.setDepthTestEnabled(false);
        marker.setListed(true);
        marker.setDetail(createDetail(claim));

        String markerId =
                createMarkerId(claim.getClaimId());

        for (BlueMapMap map : api.getMaps()) {
            if (!matchesDimension(
                    claim.getDimension(),
                    map.getWorld().getId()
            )) {
                continue;
            }

            MarkerSet markerSet =
                    getOrCreateMarkerSet(map);

            markerSet.put(
                    markerId,
                    marker
            );
        }
    }

    private static void removeClaimMarker(
            BlueMapAPI api,
            UUID claimId
    ) {
        String markerId = createMarkerId(claimId);

        for (BlueMapMap map : api.getMaps()) {
            MarkerSet markerSet =
                    map.getMarkerSets().get(MARKER_SET_ID);

            if (markerSet != null) {
                markerSet.remove(markerId);
            }
        }
    }

    private static MarkerSet getOrCreateMarkerSet(
            BlueMapMap map
    ) {
        MarkerSet existing =
                map.getMarkerSets().get(MARKER_SET_ID);

        if (existing != null) {
            return existing;
        }

        MarkerSet created = createMarkerSet();

        map.getMarkerSets().put(
                MARKER_SET_ID,
                created
        );

        return created;
    }

    private static MarkerSet createMarkerSet() {
        MarkerSet markerSet = new MarkerSet(
                "Banana Claims",
                true,
                false
        );

        markerSet.setSorting(0);

        return markerSet;
    }

    private static List<Vector2d> createOutline(
            Claim claim
    ) {
        Map<Edge, Edge> boundaryEdges =
                new LinkedHashMap<>();

        for (ClaimChunk chunk : claim.getChunks()) {
            int minX = chunk.getChunkX() * 16;
            int minZ = chunk.getChunkZ() * 16;
            int maxX = minX + 16;
            int maxZ = minZ + 16;

            addOrRemoveEdge(
                    boundaryEdges,
                    new Point(minX, minZ),
                    new Point(maxX, minZ)
            );

            addOrRemoveEdge(
                    boundaryEdges,
                    new Point(maxX, minZ),
                    new Point(maxX, maxZ)
            );

            addOrRemoveEdge(
                    boundaryEdges,
                    new Point(maxX, maxZ),
                    new Point(minX, maxZ)
            );

            addOrRemoveEdge(
                    boundaryEdges,
                    new Point(minX, maxZ),
                    new Point(minX, minZ)
            );
        }

        List<List<Point>> loops =
                traceBoundaryLoops(boundaryEdges);

        if (loops.isEmpty()) {
            return List.of();
        }

        List<Point> outerLoop = loops.stream()
                .max(
                        Comparator.comparingDouble(
                                loop -> Math.abs(
                                        calculateSignedArea(loop)
                                )
                        )
                )
                .orElse(List.of());

        return simplifyLoop(outerLoop)
                .stream()
                .map(point ->
                        new Vector2d(
                                point.x,
                                point.z
                        )
                )
                .toList();
    }

    private static void addOrRemoveEdge(
            Map<Edge, Edge> boundaryEdges,
            Point start,
            Point end
    ) {
        Edge edge = new Edge(start, end);
        Edge reverse = new Edge(end, start);

        if (boundaryEdges.remove(reverse) == null) {
            boundaryEdges.put(edge, edge);
        }
    }

    private static List<List<Point>> traceBoundaryLoops(
            Map<Edge, Edge> boundaryEdges
    ) {
        Map<Point, List<Edge>> outgoing =
                new HashMap<>();

        for (Edge edge : boundaryEdges.values()) {
            outgoing.computeIfAbsent(
                    edge.start,
                    ignored -> new ArrayList<>()
            ).add(edge);
        }

        Set<Edge> unused =
                new HashSet<>(boundaryEdges.values());

        List<List<Point>> loops =
                new ArrayList<>();

        while (!unused.isEmpty()) {
            Edge first = unused.iterator().next();
            List<Point> loop = new ArrayList<>();

            Edge current = first;
            Point startingPoint = first.start;

            while (current != null && unused.remove(current)) {
                loop.add(current.start);

                Point nextPoint = current.end;

                if (nextPoint.equals(startingPoint)) {
                    break;
                }

                current = chooseNextEdge(
                        outgoing.getOrDefault(
                                nextPoint,
                                List.of()
                        ),
                        unused
                );
            }

            if (loop.size() >= 3) {
                loops.add(loop);
            }
        }

        return loops;
    }

    private static Edge chooseNextEdge(
            List<Edge> candidates,
            Set<Edge> unused
    ) {
        for (Edge candidate : candidates) {
            if (unused.contains(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static List<Point> simplifyLoop(
            List<Point> loop
    ) {
        if (loop.size() < 3) {
            return loop;
        }

        List<Point> simplified = new ArrayList<>();

        for (int index = 0; index < loop.size(); index++) {
            Point previous =
                    loop.get(
                            Math.floorMod(index - 1, loop.size())
                    );

            Point current = loop.get(index);

            Point next =
                    loop.get(
                            (index + 1) % loop.size()
                    );

            boolean sameX =
                    previous.x == current.x
                            && current.x == next.x;

            boolean sameZ =
                    previous.z == current.z
                            && current.z == next.z;

            if (!sameX && !sameZ) {
                simplified.add(current);
            }
        }

        return simplified;
    }

    private static double calculateSignedArea(
            List<Point> loop
    ) {
        double area = 0.0D;

        for (int index = 0; index < loop.size(); index++) {
            Point current = loop.get(index);
            Point next =
                    loop.get(
                            (index + 1) % loop.size()
                    );

            area +=
                    (double) current.x * next.z
                            - (double) next.x * current.z;
        }

        return area / 2.0D;
    }

    private static boolean matchesDimension(
            String claimDimension,
            String blueMapWorldId
    ) {
        String normalizedClaim =
                normalizeDimension(claimDimension);

        String normalizedWorld =
                normalizeDimension(blueMapWorldId);

        return normalizedClaim.equals(normalizedWorld)
                || normalizedClaim.endsWith(normalizedWorld)
                || normalizedWorld.endsWith(normalizedClaim);
    }

    private static String normalizeDimension(
            String dimension
    ) {
        if (dimension == null) {
            return "";
        }

        String normalized =
                dimension.toLowerCase()
                        .replace("resourcekey", "")
                        .replace("minecraft:dimension", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(" ", "");

        if (normalized.contains("the_nether")
                || normalized.endsWith("nether")) {
            return "minecraft:the_nether";
        }

        if (normalized.contains("the_end")
                || normalized.endsWith("end")) {
            return "minecraft:the_end";
        }

        if (normalized.contains("overworld")) {
            return "minecraft:overworld";
        }

        return normalized;
    }

    private static String createDetail(Claim claim) {
        return "<div>"
                + "<strong>"
                + escapeHtml(claim.getName())
                + "</strong><br>"
                + "Owner: "
                + escapeHtml(claim.getOwnerName())
                + "<br>"
                + "Chunks: "
                + claim.getChunks().size()
                + "<br>"
                + "Description: "
                + escapeHtml(claim.getDescription())
                + "</div>";
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String createMarkerId(UUID claimId) {
        return "claim-" + claimId;
    }

    private static int createFingerprint(Claim claim) {
        List<String> sortedChunks =
                claim.getChunks()
                        .stream()
                        .map(ClaimChunk::toString)
                        .sorted()
                        .toList();

        return Objects.hash(
                claim.getClaimId(),
                claim.getName(),
                claim.getOwnerUuid(),
                claim.getOwnerName(),
                claim.getDescription(),
                sortedChunks
        );
    }

    private record Point(int x, int z) {
    }

    private record Edge(Point start, Point end) {
    }
}