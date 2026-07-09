package com.bananasandwich.bananaclaims.notification;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimPopupSettings;
import com.bananasandwich.bananaclaims.claim.PopupDisplayMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class PopupRenderer {

    public static void showEnter(ServerPlayer player, Claim claim) {
        ClaimPopupSettings settings = claim.getPopupSettings();

        String title = settings.getEnterTitle();
        String subtitle = settings.getEnterSubtitle();

        if (title == null || title.isBlank()) {
            title = "%claim%";
        }

        if (subtitle == null || subtitle.isBlank()) {
            subtitle = "Now Entering";
        }

        render(player, claim, settings.getDisplayMode(), title, subtitle);
        playSound(player, settings.getEnterSound());
    }

    public static void showLeave(ServerPlayer player, Claim claim) {
        ClaimPopupSettings settings = claim.getPopupSettings();

        String title = settings.getLeaveTitle();
        String subtitle = settings.getLeaveSubtitle();

        if (title == null || title.isBlank()) {
            title = "%claim%";
        }

        if (subtitle == null || subtitle.isBlank()) {
            subtitle = "Now Leaving";
        }

        render(player, claim, settings.getDisplayMode(), title, subtitle);
        playSound(player, settings.getLeaveSound());
    }

    private static void render(ServerPlayer player, Claim claim, PopupDisplayMode displayMode, String title, String subtitle) {
        PopupDisplayMode mode = displayMode == null ? PopupDisplayMode.ACTIONBAR : displayMode;

        MutableComponent titleComponent = parseHexColors(applyPlaceholders(title, claim));
        MutableComponent subtitleComponent = parseHexColors(applyPlaceholders(subtitle, claim));

        switch (mode) {
            case TITLE -> renderTitle(player, titleComponent, subtitleComponent);
            case CHAT -> renderChat(player, titleComponent, subtitleComponent);
            case ACTIONBAR -> renderActionBar(player, titleComponent, subtitleComponent);
        }
    }

    private static void playSound(ServerPlayer player, String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return;
        }

        Identifier identifier = Identifier.tryParse(soundId.trim());

        if (identifier == null) {
            return;
        }

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(identifier);

        if (soundEvent == null) {
            return;
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                soundEvent,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static String applyPlaceholders(String text, Claim claim) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("%claim%", claim.getName())
                .replace("%owner%", claim.getOwnerName())
                .replace("%description%", claim.getDescription())
                .replace("%chunks%", String.valueOf(claim.getChunks().size()));
    }

    private static void renderActionBar(ServerPlayer player, MutableComponent title, MutableComponent subtitle) {
        MutableComponent message = Component.empty()
                .append(subtitle)
                .append(Component.literal(": "))
                .append(title);

        player.sendSystemMessage(message, true);
    }

    private static void renderChat(ServerPlayer player, MutableComponent title, MutableComponent subtitle) {
        MutableComponent message = Component.empty()
                .append(subtitle)
                .append(Component.literal(": "))
                .append(title);

        player.sendSystemMessage(message);
    }

    private static void renderTitle(ServerPlayer player, MutableComponent title, MutableComponent subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    private static MutableComponent parseHexColors(String text) {
        MutableComponent result = Component.empty();

        if (text == null || text.isEmpty()) {
            return result;
        }

        TextColor currentColor = null;
        StringBuilder currentText = new StringBuilder();

        int index = 0;

        while (index < text.length()) {
            if (isHexColorCodeAt(text, index)) {
                appendSegment(result, currentText, currentColor);
                currentText.setLength(0);

                String hex = text.substring(index + 2, index + 8);
                currentColor = TextColor.fromRgb(Integer.parseInt(hex, 16));
                index += 8;
                continue;
            }

            currentText.append(text.charAt(index));
            index++;
        }

        appendSegment(result, currentText, currentColor);

        return result;
    }

    private static boolean isHexColorCodeAt(String text, int index) {
        if (index + 8 > text.length()) {
            return false;
        }

        if (text.charAt(index) != '&' || text.charAt(index + 1) != '#') {
            return false;
        }

        for (int i = index + 2; i < index + 8; i++) {
            char character = text.charAt(i);

            boolean isDigit = character >= '0' && character <= '9';
            boolean isUpperHex = character >= 'A' && character <= 'F';
            boolean isLowerHex = character >= 'a' && character <= 'f';

            if (!isDigit && !isUpperHex && !isLowerHex) {
                return false;
            }
        }

        return true;
    }

    private static void appendSegment(MutableComponent result, StringBuilder text, TextColor color) {
        if (text.isEmpty()) {
            return;
        }

        MutableComponent segment = Component.literal(text.toString());

        if (color != null) {
            segment.setStyle(Style.EMPTY.withColor(color));
        }

        result.append(segment);
    }
}