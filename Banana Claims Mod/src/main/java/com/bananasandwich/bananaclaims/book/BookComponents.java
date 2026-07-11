package com.bananasandwich.bananaclaims.book;

import com.bananasandwich.bananaclaims.Bananaclaims;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.dialog.Dialog;

import java.util.Optional;

public final class BookComponents {

    private BookComponents() {
    }

    public static MutableComponent page() {
        return Component.empty();
    }

    public static Component title(String text) {
        return Component.literal(text)
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
    }

    public static Component section(String text) {
        return Component.literal(text)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    public static Component label(String text) {
        return Component.literal(text)
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component value(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(ChatFormatting.BLACK);
    }

    public static Component status(boolean enabled) {
        return Component.literal(enabled ? "PROTECTED" : "OPEN")
                .withStyle(enabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED);
    }

    /**
     * Runs a normal player command through Minecraft's custom click-action
     * packet. Unlike RUN_COMMAND, this does not display the unattended-command
     * warning and the written-book screen remains open on its current page.
     */
    public static Component action(
            String label,
            String command,
            ChatFormatting color
    ) {
        CompoundTag payload = new CompoundTag();
        payload.putString("command", command == null ? "" : command);

        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(color)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.Custom(
                                Bananaclaims.id("book_command"),
                                Optional.of(payload)
                        )));
    }

    public static Component input(
            String label,
            String fieldLabel,
            String initialValue,
            int maximumLength,
            boolean multiline,
            String commandPrefix,
            ChatFormatting color
    ) {
        Holder<Dialog> dialog = BookDialogs.textInput(
                cleanLabel(label),
                fieldLabel,
                initialValue,
                maximumLength,
                multiline,
                commandPrefix
        );

        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(color)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.ShowDialog(dialog)));
    }

    /** Convenience overload for a blank, single-line text input. */
    public static Component suggest(
            String label,
            String commandPrefix,
            ChatFormatting color
    ) {
        return input(
                label,
                "Enter value",
                "",
                256,
                false,
                commandPrefix,
                color
        );
    }

    public static Component pageLink(
            String label,
            int page,
            ChatFormatting color
    ) {
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(color)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.ChangePage(page)));
    }

    public static MutableComponent line(MutableComponent page, Component content) {
        return page.append(content).append("\n");
    }

    public static MutableComponent blank(MutableComponent page) {
        return page.append("\n");
    }

    private static String cleanLabel(String label) {
        if (label == null || label.isBlank()) {
            return "Banana Claims";
        }

        return label.replace("[", "")
                .replace("]", "")
                .trim();
    }
}
