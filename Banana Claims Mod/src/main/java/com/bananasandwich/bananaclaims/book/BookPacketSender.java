package com.bananasandwich.bananaclaims.book;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Creates, identifies, updates, and opens the physical Banana Claims book. */
public final class BookPacketSender {

    private static final String BOOK_MARKER_KEY =
            "bananaclaims_management_book";

    private static final String SELECTED_CLAIM_KEY =
            "selected_claim";

    private BookPacketSender() {
    }

    public static ItemStack createBook(
            String title,
            List<Component> pages,
            UUID selectedClaimId
    ) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        updateBook(book, title, pages, selectedClaimId);
        return book;
    }

    public static void updateBook(
            ItemStack book,
            String title,
            List<Component> pages,
            UUID selectedClaimId
    ) {
        if (book == null || book.isEmpty()) {
            return;
        }

        List<Component> safePages =
                pages == null || pages.isEmpty()
                        ? List.of(Component.literal("Banana Claims"))
                        : pages;

        List<Filterable<Component>> filteredPages = safePages.stream()
                .map(Filterable::passThrough)
                .toList();

        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(truncateTitle(title)),
                        "Banana Claims",
                        0,
                        filteredPages,
                        true
                )
        );

        book.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Banana Claims")
        );

        CompoundTag customData = new CompoundTag();
        customData.putBoolean(BOOK_MARKER_KEY, true);

        if (selectedClaimId != null) {
            customData.putString(
                    SELECTED_CLAIM_KEY,
                    selectedClaimId.toString()
            );
        }

        CustomData.set(
                DataComponents.CUSTOM_DATA,
                book,
                customData
        );
    }

    public static boolean isClaimBook(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || stack.getItem() != Items.WRITTEN_BOOK) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

        return customData != null
                && customData.copyTag().getBooleanOr(
                        BOOK_MARKER_KEY,
                        false
                );
    }

    public static Optional<UUID> getSelectedClaimId(ItemStack stack) {
        if (!isClaimBook(stack)) {
            return Optional.empty();
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        String value = customData.copyTag()
                .getString(SELECTED_CLAIM_KEY)
                .orElse("");

        try {
            return value.isBlank()
                    ? Optional.empty()
                    : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * Opens the supplied book while leaving the player's actual selected item
     * and server inventory unchanged.
     */
    public static void open(
            ServerPlayer player,
            ItemStack book
    ) {
        if (player == null
                || book == null
                || book.isEmpty()) {
            return;
        }

        int selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack original = player.getInventory().getSelectedItem().copy();

        player.connection.send(
                new ClientboundSetPlayerInventoryPacket(
                        selectedSlot,
                        book.copy()
                )
        );
        player.connection.send(
                new ClientboundOpenBookPacket(
                        InteractionHand.MAIN_HAND
                )
        );
        player.connection.send(
                new ClientboundSetPlayerInventoryPacket(
                        selectedSlot,
                        original
                )
        );
    }

    public static void showDialog(
            ServerPlayer player,
            Holder<Dialog> dialog
    ) {
        if (player == null || dialog == null) {
            return;
        }

        player.connection.send(
                new ClientboundShowDialogPacket(dialog)
        );
    }

    private static String truncateTitle(String title) {
        String normalized = title == null
                ? "Banana Claims"
                : title;

        return normalized.length() <= 32
                ? normalized
                : normalized.substring(0, 32);
    }
}
