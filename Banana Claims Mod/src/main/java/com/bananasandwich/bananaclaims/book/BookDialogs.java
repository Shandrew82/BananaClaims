package com.bananasandwich.bananaclaims.book;

import com.bananasandwich.bananaclaims.Bananaclaims;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;

import java.util.List;
import java.util.Optional;

/** Vanilla dialog builders used by the server-side Book GUI. */
public final class BookDialogs {

    private BookDialogs() {
    }

    public static Holder<Dialog> textInput(
            String title,
            String label,
            String initial,
            int maximumLength,
            boolean multiline,
            String commandPrefix
    ) {
        int safeMaximum = Math.max(1, maximumLength);

        CompoundTag additions = new CompoundTag();
        additions.putString(
                "prefix",
                commandPrefix == null ? "" : commandPrefix
        );

        TextInput.MultilineOptions multilineOptions =
                new TextInput.MultilineOptions(
                        Optional.of(6),
                        Optional.of(90)
                );

        Input input = new Input(
                "value",
                new TextInput(
                        280,
                        Component.literal(label == null ? "Value" : label),
                        true,
                        truncate(initial, safeMaximum),
                        safeMaximum,
                        multiline
                                ? Optional.of(multilineOptions)
                                : Optional.empty()
                )
        );

        CommonDialogData common = new CommonDialogData(
                Component.literal(title == null ? "Banana Claims" : title),
                Optional.empty(),
                true,
                false,
                DialogAction.CLOSE,
                List.of(new PlainMessage(
                        Component.literal("Enter the value, then choose Apply."),
                        280
                )),
                List.of(input)
        );

        ActionButton apply = new ActionButton(
                new CommonButtonData(Component.literal("Apply"), 120),
                Optional.of(new CustomAll(
                        Bananaclaims.id("book_prompt"),
                        Optional.of(additions)
                ))
        );

        ActionButton cancel = new ActionButton(
                new CommonButtonData(CommonComponents.GUI_CANCEL, 120),
                Optional.empty()
        );

        return Holder.direct(new ConfirmationDialog(
                common,
                apply,
                cancel
        ));
    }

    public static Holder<Dialog> confirmation(
            String title,
            String subject,
            String command
    ) {
        CompoundTag payload = new CompoundTag();
        payload.putString("command", command == null ? "" : command);

        CommonDialogData common = new CommonDialogData(
                Component.literal(title == null ? "Confirm Action" : title),
                Optional.empty(),
                true,
                false,
                DialogAction.CLOSE,
                List.of(new PlainMessage(
                        Component.literal(subject == null ? "" : subject),
                        280
                )),
                List.of()
        );

        ActionButton confirm = new ActionButton(
                new CommonButtonData(Component.literal("Confirm"), 120),
                Optional.of(new StaticAction(new ClickEvent.Custom(
                        Bananaclaims.id("book_command"),
                        Optional.of(payload)
                )))
        );

        ActionButton cancel = new ActionButton(
                new CommonButtonData(CommonComponents.GUI_CANCEL, 120),
                Optional.empty()
        );

        return Holder.direct(new ConfirmationDialog(
                common,
                confirm,
                cancel
        ));
    }

    private static String truncate(String value, int maximumLength) {
        if (value == null) {
            return "";
        }

        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }
}
