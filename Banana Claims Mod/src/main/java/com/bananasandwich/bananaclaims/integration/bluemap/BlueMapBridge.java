package com.bananasandwich.bananaclaims.integration.bluemap;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimChunk;
import com.bananasandwich.bananaclaims.claim.ClaimMember;
import com.bananasandwich.bananaclaims.claim.ClaimSubOwner;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeEvent;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeListener;
import com.bananasandwich.bananaclaims.claim.event.ClaimChangeType;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
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

/**
 * Optional BlueMap bridge kept in a separate class so Banana Claims can load
 * safely when the BlueMap API is absent.
 */
public final class BlueMapBridge {

    public static final String MARKER_SET_ID = "bananaclaims";

    private static final float MARKER_HEIGHT = 64.0F;

    private static final Color BORDER_COLOR =
            new Color(35, 120, 255, 1.0F);

    private static final Color FILL_COLOR =
            new Color(35, 120, 255, 0.22F);

    private static final Map<UUID, Integer> CLAIM_FINGERPRINTS =
            new HashMap<>();

    private static final Map<UUID, List<String>> CLAIM_MARKER_IDS =
            new HashMap<>();

    private static BlueMapAPI activeApi;
    private static boolean registered;

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
        CLAIM_MARKER_IDS.clear();

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
        CLAIM_MARKER_IDS.clear();
        activeApi = null;

        Bananaclaims.LOGGER.info(
                "Banana Claims BlueMap integration disabled."
        );
    }

    private static void handleClaimChange(
            ClaimChangeEvent event
    ) {
        if (activeApi == null || event == null) {
            return;
        }

        ClaimChangeType type = event.getType();
        Claim claim = event.getClaim();

        if (type == ClaimChangeType.LOADED
                || claim == null) {
            synchronizeAllClaims();
            return;
        }

        if (type == ClaimChangeType.DELETED) {
            removeClaimMarkers(
                    activeApi,
                    claim.getClaimId()
            );
            CLAIM_FINGERPRINTS.remove(
                    claim.getClaimId()
            );
            return;
        }

        synchronizeClaim(claim);
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
            activeClaimIds.add(claim.getClaimId());
            synchronizeClaim(claim);
        }

        List<UUID> removedClaimIds =
                CLAIM_FINGERPRINTS.keySet()
                        .stream()
                        .filter(claimId ->
                                !activeClaimIds.contains(claimId)
                        )
                        .toList();

        for (UUID removedClaimId : removedClaimIds) {
            removeClaimMarkers(
                    api,
                    removedClaimId
            );
            CLAIM_FINGERPRINTS.remove(
                    removedClaimId
            );
        }
    }

    private static void synchronizeClaim(
            Claim claim
    ) {
        BlueMapAPI api = activeApi;

        if (api == null
                || claim == null
                || claim.getClaimId() == null) {
            return;
        }

        UUID claimId = claim.getClaimId();
        int fingerprint = createFingerprint(claim);

        Integer previousFingerprint =
                CLAIM_FINGERPRINTS.get(claimId);

        if (previousFingerprint != null
                && previousFingerprint == fingerprint) {
            return;
        }

        removeClaimMarkers(api, claimId);
        addClaimMarkers(api, claim);

        CLAIM_FINGERPRINTS.put(
                claimId,
                fingerprint
        );
    }

    private static void addClaimMarkers(
            BlueMapAPI api,
            Claim claim
    ) {
        List<List<Vector2d>> outlines =
                createOutlines(claim);

        if (outlines.isEmpty()) {
            Bananaclaims.LOGGER.warn(
                    "Could not create a BlueMap outline for claim '{}'.",
                    claim.getName()
            );
            return;
        }

        List<String> markerIds = new ArrayList<>();
        String detail = createDetail(claim);

        for (BlueMapMap map : api.getMaps()) {
            if (!matchesDimension(
                    claim.getDimension(),
                    map.getWorld().getId()
            )) {
                continue;
            }

            MarkerSet markerSet = getOrCreateMarkerSet(map);

            for (int index = 0;
                 index < outlines.size();
                 index++) {
                String markerId = createMarkerId(
                        claim.getClaimId(),
                        index
                );

                ShapeMarker marker = createMarker(
                        claim,
                        outlines.get(index),
                        detail,
                        index == 0
                );

                markerSet.put(markerId, marker);

                if (!markerIds.contains(markerId)) {
                    markerIds.add(markerId);
                }
            }
        }

        if (!markerIds.isEmpty()) {
            CLAIM_MARKER_IDS.put(
                    claim.getClaimId(),
                    List.copyOf(markerIds)
            );
        }
    }

    private static ShapeMarker createMarker(
            Claim claim,
            List<Vector2d> outline,
            String detail,
            boolean primary
    ) {
        ShapeMarker marker = new ShapeMarker(
                claim.getName(),
                new Shape(outline),
                MARKER_HEIGHT
        );

        marker.setLineWidth(2);
        marker.setColors(BORDER_COLOR, FILL_COLOR);
        marker.setDepthTestEnabled(false);
        marker.setListed(primary);
        marker.setDetail(detail);

        return marker;
    }

    private static void removeClaimMarkers(
            BlueMapAPI api,
            UUID claimId
    ) {
        if (claimId == null) {
            return;
        }

        List<String> markerIds =
                CLAIM_MARKER_IDS.remove(claimId);

        if (markerIds == null || markerIds.isEmpty()) {
            markerIds = List.of(
                    createMarkerId(claimId, 0),
                    "claim-" + claimId
            );
        }

        for (BlueMapMap map : api.getMaps()) {
            MarkerSet markerSet =
                    map.getMarkerSets().get(MARKER_SET_ID);

            if (markerSet == null) {
                continue;
            }

            for (String markerId : markerIds) {
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
        map.getMarkerSets().put(MARKER_SET_ID, created);

        return created;
    }

    private static MarkerSet createMarkerSet() {
        MarkerSet markerSet = new MarkerSet(
                BananaClaimsMessages.string(
                        "bluemap.bananaclaims.marker_set"
                ),
                true,
                false
        );

        markerSet.setSorting(0);

        return markerSet;
    }

    private static List<List<Vector2d>> createOutlines(
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

        return traceBoundaryLoops(boundaryEdges)
                .stream()
                .map(BlueMapBridge::simplifyLoop)
                .filter(loop -> loop.size() >= 3)
                .filter(loop ->
                        calculateSignedArea(loop) > 0.0D
                )
                .sorted(
                        Comparator.comparingDouble(
                                loop -> -Math.abs(
                                        calculateSignedArea(loop)
                                )
                        )
                )
                .map(loop ->
                        loop.stream()
                                .map(point ->
                                        new Vector2d(
                                                point.x,
                                                point.z
                                        )
                                )
                                .toList()
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

        List<List<Point>> loops = new ArrayList<>();

        while (!unused.isEmpty()) {
            Edge first = unused.iterator().next();
            List<Point> loop = new ArrayList<>();

            Edge current = first;
            Point startingPoint = first.start;

            while (current != null
                    && unused.remove(current)) {
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

        for (int index = 0;
             index < loop.size();
             index++) {
            Point previous = loop.get(
                    Math.floorMod(
                            index - 1,
                            loop.size()
                    )
            );

            Point current = loop.get(index);
            Point next = loop.get(
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

        for (int index = 0;
             index < loop.size();
             index++) {
            Point current = loop.get(index);
            Point next = loop.get(
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

    private static String createDetail(
            Claim claim
    ) {
        List<String> subOwnerNames =
                getSortedSubOwnerNames(claim);

        List<String> memberNames =
                getSortedMemberNames(claim);

        String description =
                claim.getDescription().isBlank()
                        ? BananaClaimsMessages.string(
                        "bluemap.bananaclaims.no_description"
                )
                        : claim.getDescription();

        return "<div class=\"bananaclaims-marker\">"
                + "<h3 style=\"margin:0 0 6px 0\">"
                + escapeHtml(claim.getName())
                + "</h3>"
                + detailRow(
                "bluemap.bananaclaims.owner",
                escapeHtml(claim.getOwnerName())
        )
                + detailList(
                "bluemap.bananaclaims.subowners",
                subOwnerNames
        )
                + detailList(
                "bluemap.bananaclaims.members",
                memberNames
        )
                + detailRow(
                "bluemap.bananaclaims.chunks",
                Integer.toString(claim.getChunks().size())
        )
                + "<hr style=\"margin:6px 0\">"
                + "<strong>"
                + escapeHtml(BananaClaimsMessages.string(
                "bluemap.bananaclaims.description"
        ))
                + ":</strong><br>"
                + escapeHtml(description)
                + "</div>";
    }

    private static String detailRow(
            String labelKey,
            String value
    ) {
        return "<strong>"
                + escapeHtml(
                BananaClaimsMessages.string(labelKey)
        )
                + ":</strong> "
                + value
                + "<br>";
    }

    private static String detailList(
            String labelKey,
            List<String> names
    ) {
        return "<strong>"
                + escapeHtml(
                BananaClaimsMessages.string(labelKey)
        )
                + " ("
                + names.size()
                + "):</strong><br>"
                + toHtmlList(names)
                + "<br>";
    }

    private static String toHtmlList(
            List<String> names
    ) {
        if (names.isEmpty()) {
            return escapeHtml(
                    BananaClaimsMessages.string(
                            "bluemap.bananaclaims.none"
                    )
            );
        }

        return names.stream()
                .map(BlueMapBridge::escapeHtml)
                .map(name -> "&#8226; " + name)
                .reduce(
                        (first, second) ->
                                first + "<br>" + second
                )
                .orElse("");
    }

    private static List<String> getSortedSubOwnerNames(
            Claim claim
    ) {
        return claim.getSubOwners()
                .stream()
                .map(ClaimSubOwner::getName)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static List<String> getSortedMemberNames(
            Claim claim
    ) {
        return claim.getMembers()
                .stream()
                .map(ClaimMember::getName)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static String escapeHtml(
            String text
    ) {
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

    private static String createMarkerId(
            UUID claimId,
            int outlineIndex
    ) {
        return "claim-"
                + claimId
                + "-"
                + outlineIndex;
    }

    private static int createFingerprint(
            Claim claim
    ) {
        List<String> sortedChunks =
                claim.getChunks()
                        .stream()
                        .map(ClaimChunk::toString)
                        .sorted()
                        .toList();

        List<String> sortedSubOwners =
                claim.getSubOwners()
                        .stream()
                        .map(subOwner ->
                                subOwner.getUuid()
                                        + "|"
                                        + subOwner.getName()
                        )
                        .sorted()
                        .toList();

        List<String> sortedMembers =
                claim.getMembers()
                        .stream()
                        .map(member ->
                                member.getUuid()
                                        + "|"
                                        + member.getName()
                        )
                        .sorted()
                        .toList();

        return Objects.hash(
                claim.getClaimId(),
                claim.getName(),
                claim.getOwnerUuid(),
                claim.getOwnerName(),
                claim.getDescription(),
                claim.getDimension(),
                sortedChunks,
                sortedSubOwners,
                sortedMembers
        );
    }

    private record Point(int x, int z) {
    }

    private record Edge(
            Point start,
            Point end
    ) {
    }
}
