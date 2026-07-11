package com.bananasandwich.bananaclaims.previewv2;

import java.util.Locale;

/**
 * Serializable configuration for Renderer V2.
 *
 * <p>The configuration intentionally contains only data and validation logic so
 * the same model can later be edited by commands or the Book GUI without
 * coupling either interface to the renderer implementation.</p>
 */
public final class PreviewV2Config {

    public static final int CURRENT_CONFIG_VERSION = 3;

    public static final String DEFAULT_GLOW_COLOR =
            "#A855F7";

    public static final String DEFAULT_BORDER_MATERIAL =
            "minecraft:amethyst_block";

    public static final String DEFAULT_CORNER_MATERIAL =
            "minecraft:lapis_block";

    public static final String DEFAULT_GUIDE_MATERIAL =
            "minecraft:amethyst_block";

    private int configVersion;
    private int durationTicks = 10 * 20;
    private float viewRange = 4.0F;
    private boolean glowEnabled = true;
    private String glowColor = DEFAULT_GLOW_COLOR;
    private float shadowRadius = 0.0F;
    private float shadowStrength = 0.0F;
    private int testPreviewSize = 8;
    private double testPreviewDistance = 7.0D;
    private float minimumColumnHeight = 1.0F;

    private BorderSettings border =
            new BorderSettings();

    private CornerSettings corners =
            new CornerSettings();

    private GuideSettings guides =
            new GuideSettings();

    private TerrainSettings terrain =
            new TerrainSettings();

    private AnimationSettings animations =
            new AnimationSettings();

    public static PreviewV2Config defaults() {
        return new PreviewV2Config();
    }

    public PreviewV2Config copy() {
        PreviewV2Config copy =
                new PreviewV2Config();

        copy.configVersion = configVersion;
        copy.durationTicks = durationTicks;
        copy.viewRange = viewRange;
        copy.glowEnabled = glowEnabled;
        copy.glowColor = glowColor;
        copy.shadowRadius = shadowRadius;
        copy.shadowStrength = shadowStrength;
        copy.testPreviewSize = testPreviewSize;
        copy.testPreviewDistance = testPreviewDistance;
        copy.minimumColumnHeight = minimumColumnHeight;
        copy.border =
                border == null
                        ? null
                        : border.copy();
        copy.corners =
                corners == null
                        ? null
                        : corners.copy();
        copy.guides =
                guides == null
                        ? null
                        : guides.copy();
        copy.terrain =
                terrain == null
                        ? null
                        : terrain.copy();
        copy.animations =
                animations == null
                        ? null
                        : animations.copy();

        return copy;
    }

    /**
     * Repairs null, non-finite, and out-of-range values.
     *
     * @return true when at least one value was changed
     */
    boolean sanitize() {
        boolean changed = false;

        int previousConfigVersion =
                configVersion;

        if (configVersion != CURRENT_CONFIG_VERSION) {
            configVersion = CURRENT_CONFIG_VERSION;
            changed = true;
        }

        int sanitizedDuration =
                clamp(durationTicks, 1, 20 * 60 * 30);
        changed |= sanitizedDuration != durationTicks;
        durationTicks = sanitizedDuration;

        float sanitizedViewRange =
                finiteFloat(viewRange, 4.0F, 0.25F, 64.0F);
        changed |= Float.compare(sanitizedViewRange, viewRange) != 0;
        viewRange = sanitizedViewRange;

        String sanitizedGlowColor =
                normalizeColor(glowColor);
        changed |= !sanitizedGlowColor.equals(glowColor);
        glowColor = sanitizedGlowColor;

        float sanitizedShadowRadius =
                finiteFloat(shadowRadius, 0.0F, 0.0F, 64.0F);
        changed |= Float.compare(sanitizedShadowRadius, shadowRadius) != 0;
        shadowRadius = sanitizedShadowRadius;

        float sanitizedShadowStrength =
                finiteFloat(shadowStrength, 0.0F, 0.0F, 1.0F);
        changed |= Float.compare(sanitizedShadowStrength, shadowStrength) != 0;
        shadowStrength = sanitizedShadowStrength;

        int sanitizedTestSize =
                clamp(testPreviewSize, 1, 512);
        changed |= sanitizedTestSize != testPreviewSize;
        testPreviewSize = sanitizedTestSize;

        double sanitizedTestDistance =
                finiteDouble(testPreviewDistance, 7.0D, 0.0D, 128.0D);
        changed |= Double.compare(
                sanitizedTestDistance,
                testPreviewDistance
        ) != 0;
        testPreviewDistance = sanitizedTestDistance;

        float sanitizedMinimumColumnHeight =
                finiteFloat(
                        minimumColumnHeight,
                        1.0F,
                        0.01F,
                        64.0F
                );
        changed |= Float.compare(
                sanitizedMinimumColumnHeight,
                minimumColumnHeight
        ) != 0;
        minimumColumnHeight = sanitizedMinimumColumnHeight;

        if (border == null) {
            border = new BorderSettings();
            changed = true;
        }

        if (corners == null) {
            corners = new CornerSettings();
            changed = true;
        }

        if (guides == null) {
            guides = new GuideSettings();
            changed = true;
        }

        if (terrain == null) {
            terrain = new TerrainSettings();
            changed = true;
        }

        if (animations == null) {
            animations = new AnimationSettings();
            changed = true;
        }

        if (previousConfigVersion < 3) {
            changed |= animations.applyVersion3Defaults();
        }

        changed |= border.sanitize();
        changed |= corners.sanitize();
        changed |= guides.sanitize();
        changed |= terrain.sanitize();
        changed |= animations.sanitize();

        return changed;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public float getViewRange() {
        return viewRange;
    }

    public void setViewRange(float viewRange) {
        this.viewRange = viewRange;
    }

    public boolean isGlowEnabled() {
        return glowEnabled;
    }

    public void setGlowEnabled(boolean glowEnabled) {
        this.glowEnabled = glowEnabled;
    }

    public String getGlowColor() {
        return glowColor;
    }

    public void setGlowColor(String glowColor) {
        this.glowColor = glowColor;
    }

    public int getGlowColorRgb() {
        return Integer.parseInt(
                glowColor.substring(1),
                16
        );
    }

    public float getShadowRadius() {
        return shadowRadius;
    }

    public void setShadowRadius(float shadowRadius) {
        this.shadowRadius = shadowRadius;
    }

    public float getShadowStrength() {
        return shadowStrength;
    }

    public void setShadowStrength(float shadowStrength) {
        this.shadowStrength = shadowStrength;
    }

    public int getTestPreviewSize() {
        return testPreviewSize;
    }

    public void setTestPreviewSize(int testPreviewSize) {
        this.testPreviewSize = testPreviewSize;
    }

    public double getTestPreviewDistance() {
        return testPreviewDistance;
    }

    public void setTestPreviewDistance(double testPreviewDistance) {
        this.testPreviewDistance = testPreviewDistance;
    }

    public float getMinimumColumnHeight() {
        return minimumColumnHeight;
    }

    public void setMinimumColumnHeight(float minimumColumnHeight) {
        this.minimumColumnHeight = minimumColumnHeight;
    }

    public BorderSettings getBorder() {
        return border;
    }

    public void setBorder(BorderSettings border) {
        this.border = border;
    }

    public CornerSettings getCorners() {
        return corners;
    }

    public void setCorners(CornerSettings corners) {
        this.corners = corners;
    }

    public GuideSettings getGuides() {
        return guides;
    }

    public void setGuides(GuideSettings guides) {
        this.guides = guides;
    }

    public TerrainSettings getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainSettings terrain) {
        this.terrain = terrain;
    }

    public AnimationSettings getAnimations() {
        return animations;
    }

    public void setAnimations(AnimationSettings animations) {
        this.animations = animations;
    }

    public static final class BorderSettings {

        private String material =
                DEFAULT_BORDER_MATERIAL;

        private boolean glowEnabled = true;
        private float thickness = 0.35F;
        private float height = 0.35F;
        private double terrainOffset = 0.18D;

        private BorderSettings copy() {
            BorderSettings copy =
                    new BorderSettings();

            copy.material = material;
            copy.glowEnabled = glowEnabled;
            copy.thickness = thickness;
            copy.height = height;
            copy.terrainOffset = terrainOffset;

            return copy;
        }

        private boolean sanitize() {
            boolean changed = false;

            String sanitizedMaterial =
                    normalizeMaterial(
                            material,
                            DEFAULT_BORDER_MATERIAL
                    );
            changed |= !sanitizedMaterial.equals(material);
            material = sanitizedMaterial;

            float sanitizedThickness =
                    finiteFloat(thickness, 0.35F, 0.01F, 16.0F);
            changed |= Float.compare(
                    sanitizedThickness,
                    thickness
            ) != 0;
            thickness = sanitizedThickness;

            float sanitizedHeight =
                    finiteFloat(height, 0.35F, 0.01F, 16.0F);
            changed |= Float.compare(sanitizedHeight, height) != 0;
            height = sanitizedHeight;

            double sanitizedTerrainOffset =
                    finiteDouble(
                            terrainOffset,
                            0.18D,
                            -16.0D,
                            16.0D
                    );
            changed |= Double.compare(
                    sanitizedTerrainOffset,
                    terrainOffset
            ) != 0;
            terrainOffset = sanitizedTerrainOffset;

            return changed;
        }

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public boolean isGlowEnabled() {
            return glowEnabled;
        }

        public void setGlowEnabled(boolean glowEnabled) {
            this.glowEnabled = glowEnabled;
        }

        public float getThickness() {
            return thickness;
        }

        public void setThickness(float thickness) {
            this.thickness = thickness;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public double getTerrainOffset() {
            return terrainOffset;
        }

        public void setTerrainOffset(double terrainOffset) {
            this.terrainOffset = terrainOffset;
        }
    }

    public static final class CornerSettings {

        private String material =
                DEFAULT_CORNER_MATERIAL;

        private float size = 1.15F;
        private float height = 1.15F;
        private boolean anchorGlowEnabled = true;
        private boolean columnsEnabled = true;
        private boolean columnGlowEnabled = false;
        private float columnThickness = 0.28F;
        private double columnGap = 0.10D;

        private CornerSettings copy() {
            CornerSettings copy =
                    new CornerSettings();

            copy.material = material;
            copy.size = size;
            copy.height = height;
            copy.anchorGlowEnabled = anchorGlowEnabled;
            copy.columnsEnabled = columnsEnabled;
            copy.columnGlowEnabled = columnGlowEnabled;
            copy.columnThickness = columnThickness;
            copy.columnGap = columnGap;

            return copy;
        }

        private boolean sanitize() {
            boolean changed = false;

            String sanitizedMaterial =
                    normalizeMaterial(
                            material,
                            DEFAULT_CORNER_MATERIAL
                    );
            changed |= !sanitizedMaterial.equals(material);
            material = sanitizedMaterial;

            float sanitizedSize =
                    finiteFloat(size, 1.15F, 0.01F, 16.0F);
            changed |= Float.compare(sanitizedSize, size) != 0;
            size = sanitizedSize;

            float sanitizedHeight =
                    finiteFloat(height, 1.15F, 0.01F, 16.0F);
            changed |= Float.compare(sanitizedHeight, height) != 0;
            height = sanitizedHeight;

            float sanitizedColumnThickness =
                    finiteFloat(
                            columnThickness,
                            0.28F,
                            0.01F,
                            16.0F
                    );
            changed |= Float.compare(
                    sanitizedColumnThickness,
                    columnThickness
            ) != 0;
            columnThickness = sanitizedColumnThickness;

            double sanitizedColumnGap =
                    finiteDouble(
                            columnGap,
                            0.10D,
                            0.0D,
                            16.0D
                    );
            changed |= Double.compare(
                    sanitizedColumnGap,
                    columnGap
            ) != 0;
            columnGap = sanitizedColumnGap;

            return changed;
        }

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public float getSize() {
            return size;
        }

        public void setSize(float size) {
            this.size = size;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public boolean isAnchorGlowEnabled() {
            return anchorGlowEnabled;
        }

        public void setAnchorGlowEnabled(boolean anchorGlowEnabled) {
            this.anchorGlowEnabled = anchorGlowEnabled;
        }

        public boolean isColumnsEnabled() {
            return columnsEnabled;
        }

        public void setColumnsEnabled(boolean columnsEnabled) {
            this.columnsEnabled = columnsEnabled;
        }

        public boolean isColumnGlowEnabled() {
            return columnGlowEnabled;
        }

        public void setColumnGlowEnabled(boolean columnGlowEnabled) {
            this.columnGlowEnabled = columnGlowEnabled;
        }

        public float getColumnThickness() {
            return columnThickness;
        }

        public void setColumnThickness(float columnThickness) {
            this.columnThickness = columnThickness;
        }

        public double getColumnGap() {
            return columnGap;
        }

        public void setColumnGap(double columnGap) {
            this.columnGap = columnGap;
        }
    }

    public static final class GuideSettings {

        private boolean enabled = true;
        private boolean glowEnabled = false;

        private String material =
                DEFAULT_GUIDE_MATERIAL;

        private int spacing = 16;
        private float width = 0.18F;

        private GuideSettings copy() {
            GuideSettings copy =
                    new GuideSettings();

            copy.enabled = enabled;
            copy.glowEnabled = glowEnabled;
            copy.material = material;
            copy.spacing = spacing;
            copy.width = width;

            return copy;
        }

        private boolean sanitize() {
            boolean changed = false;

            String sanitizedMaterial =
                    normalizeMaterial(
                            material,
                            DEFAULT_GUIDE_MATERIAL
                    );
            changed |= !sanitizedMaterial.equals(material);
            material = sanitizedMaterial;

            int sanitizedSpacing =
                    clamp(spacing, 1, 4096);
            changed |= sanitizedSpacing != spacing;
            spacing = sanitizedSpacing;

            float sanitizedWidth =
                    finiteFloat(width, 0.18F, 0.01F, 16.0F);
            changed |= Float.compare(sanitizedWidth, width) != 0;
            width = sanitizedWidth;

            return changed;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isGlowEnabled() {
            return glowEnabled;
        }

        public void setGlowEnabled(boolean glowEnabled) {
            this.glowEnabled = glowEnabled;
        }

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public int getSpacing() {
            return spacing;
        }

        public void setSpacing(int spacing) {
            this.spacing = spacing;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }
    }

    public static final class TerrainSettings {

        private boolean ignoreLogs = true;
        private boolean ignoreLeaves = true;

        private TerrainSettings copy() {
            TerrainSettings copy =
                    new TerrainSettings();

            copy.ignoreLogs = ignoreLogs;
            copy.ignoreLeaves = ignoreLeaves;

            return copy;
        }

        private boolean sanitize() {
            return false;
        }

        public boolean isIgnoreLogs() {
            return ignoreLogs;
        }

        public void setIgnoreLogs(boolean ignoreLogs) {
            this.ignoreLogs = ignoreLogs;
        }

        public boolean isIgnoreLeaves() {
            return ignoreLeaves;
        }

        public void setIgnoreLeaves(boolean ignoreLeaves) {
            this.ignoreLeaves = ignoreLeaves;
        }
    }

    /**
     * Reserved settings for the planned Renderer V2 animation milestone.
     * They are persisted now so future animation work can use the same config
     * schema without a breaking migration.
     */
    public static final class AnimationSettings {

        private boolean fadeEnabled = false;
        private int fadeInTicks = 10;
        private int fadeOutTicks = 10;
        private int fadeUpdateIntervalTicks = 1;
        private float fadeMinimumScale = 0.05F;
        private boolean pulseEnabled = false;
        private int pulsePeriodTicks = 20;
        private float pulseMinimumScale = 0.90F;

        private AnimationSettings copy() {
            AnimationSettings copy =
                    new AnimationSettings();

            copy.fadeEnabled = fadeEnabled;
            copy.fadeInTicks = fadeInTicks;
            copy.fadeOutTicks = fadeOutTicks;
            copy.fadeUpdateIntervalTicks = fadeUpdateIntervalTicks;
            copy.fadeMinimumScale = fadeMinimumScale;
            copy.pulseEnabled = pulseEnabled;
            copy.pulsePeriodTicks = pulsePeriodTicks;
            copy.pulseMinimumScale = pulseMinimumScale;

            return copy;
        }

        private boolean applyVersion3Defaults() {
            if (!fadeEnabled
                    && fadeInTicks == 0
                    && fadeOutTicks == 0) {
                fadeInTicks = 10;
                fadeOutTicks = 10;
                return true;
            }

            return false;
        }

        private boolean sanitize() {
            boolean changed = false;

            int sanitizedFadeInTicks =
                    clamp(fadeInTicks, 0, 20 * 60 * 30);
            changed |= sanitizedFadeInTicks != fadeInTicks;
            fadeInTicks = sanitizedFadeInTicks;

            int sanitizedFadeOutTicks =
                    clamp(fadeOutTicks, 0, 20 * 60 * 30);
            changed |= sanitizedFadeOutTicks != fadeOutTicks;
            fadeOutTicks = sanitizedFadeOutTicks;

            int sanitizedFadeUpdateIntervalTicks =
                    clamp(fadeUpdateIntervalTicks, 1, 20);
            changed |= sanitizedFadeUpdateIntervalTicks
                    != fadeUpdateIntervalTicks;
            fadeUpdateIntervalTicks =
                    sanitizedFadeUpdateIntervalTicks;

            float sanitizedFadeMinimumScale =
                    finiteFloat(
                            fadeMinimumScale,
                            0.05F,
                            0.001F,
                            1.0F
                    );
            changed |= Float.compare(
                    sanitizedFadeMinimumScale,
                    fadeMinimumScale
            ) != 0;
            fadeMinimumScale = sanitizedFadeMinimumScale;

            int sanitizedPulsePeriodTicks =
                    clamp(pulsePeriodTicks, 1, 20 * 60 * 30);
            changed |= sanitizedPulsePeriodTicks != pulsePeriodTicks;
            pulsePeriodTicks = sanitizedPulsePeriodTicks;

            float sanitizedPulseMinimumScale =
                    finiteFloat(
                            pulseMinimumScale,
                            0.90F,
                            0.01F,
                            1.0F
                    );
            changed |= Float.compare(
                    sanitizedPulseMinimumScale,
                    pulseMinimumScale
            ) != 0;
            pulseMinimumScale = sanitizedPulseMinimumScale;

            return changed;
        }

        public boolean isFadeEnabled() {
            return fadeEnabled;
        }

        public void setFadeEnabled(boolean fadeEnabled) {
            this.fadeEnabled = fadeEnabled;
        }

        public int getFadeInTicks() {
            return fadeInTicks;
        }

        public void setFadeInTicks(int fadeInTicks) {
            this.fadeInTicks = fadeInTicks;
        }

        public int getFadeOutTicks() {
            return fadeOutTicks;
        }

        public void setFadeOutTicks(int fadeOutTicks) {
            this.fadeOutTicks = fadeOutTicks;
        }

        public int getFadeUpdateIntervalTicks() {
            return fadeUpdateIntervalTicks;
        }

        public void setFadeUpdateIntervalTicks(
                int fadeUpdateIntervalTicks
        ) {
            this.fadeUpdateIntervalTicks =
                    fadeUpdateIntervalTicks;
        }

        public float getFadeMinimumScale() {
            return fadeMinimumScale;
        }

        public void setFadeMinimumScale(
                float fadeMinimumScale
        ) {
            this.fadeMinimumScale =
                    fadeMinimumScale;
        }

        public boolean isPulseEnabled() {
            return pulseEnabled;
        }

        public void setPulseEnabled(boolean pulseEnabled) {
            this.pulseEnabled = pulseEnabled;
        }

        public int getPulsePeriodTicks() {
            return pulsePeriodTicks;
        }

        public void setPulsePeriodTicks(int pulsePeriodTicks) {
            this.pulsePeriodTicks = pulsePeriodTicks;
        }

        public float getPulseMinimumScale() {
            return pulseMinimumScale;
        }

        public void setPulseMinimumScale(float pulseMinimumScale) {
            this.pulseMinimumScale = pulseMinimumScale;
        }
    }

    private static String normalizeColor(
            String value
    ) {
        if (value == null) {
            return DEFAULT_GLOW_COLOR;
        }

        String normalized =
                value.trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6) {
            return DEFAULT_GLOW_COLOR;
        }

        for (int index = 0;
             index < normalized.length();
             index++) {
            char character =
                    normalized.charAt(index);

            boolean hexadecimal =
                    character >= '0'
                            && character <= '9'
                            || character >= 'A'
                            && character <= 'F';

            if (!hexadecimal) {
                return DEFAULT_GLOW_COLOR;
            }
        }

        return "#" + normalized;
    }

    private static String normalizeMaterial(
            String value,
            String fallback
    ) {
        if (value == null
                || value.isBlank()) {
            return fallback;
        }

        String normalized =
                value.trim()
                        .toLowerCase(Locale.ROOT);

        if (!normalized.contains(":")) {
            normalized =
                    "minecraft:" + normalized;
        }

        return normalized;
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    private static float finiteFloat(
            float value,
            float fallback,
            float minimum,
            float maximum
    ) {
        if (!Float.isFinite(value)) {
            return fallback;
        }

        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    private static double finiteDouble(
            double value,
            double fallback,
            double minimum,
            double maximum
    ) {
        if (!Double.isFinite(value)) {
            return fallback;
        }

        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}





