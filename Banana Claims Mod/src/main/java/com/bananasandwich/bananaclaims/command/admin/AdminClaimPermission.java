package com.bananasandwich.bananaclaims.command.admin;

import com.bananasandwich.bananaclaims.Bananaclaims;
import com.bananasandwich.bananaclaims.permission.ClaimPermission;
import com.bananasandwich.bananaclaims.localization.BananaClaimsMessages;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.IntSupplier;

/**
 * Central permission gate for Banana Claims administration commands.
 *
 * <p>The broad admin node grants every administrative command. Individual
 * subcommand nodes can also be granted without granting the broad node.</p>
 */
public final class AdminClaimPermission {

    private AdminClaimPermission() {
    }

    public static boolean canUse(
            CommandSourceStack source
    ) {
        for (ClaimPermission permission
                : ClaimPermission.adminPermissions()) {
            if (Bananaclaims.PERMISSION_SERVICE.has(
                    source,
                    permission
            )) {
                return true;
            }
        }

        return false;
    }

    public static boolean canUse(
            CommandSourceStack source,
            ClaimPermission permission
    ) {
        return Bananaclaims.PERMISSION_SERVICE.has(
                source,
                ClaimPermission.ADMIN_ROOT
        ) || Bananaclaims.PERMISSION_SERVICE.has(
                source,
                permission
        );
    }

    public static boolean canUseAny(
            CommandSourceStack source,
            ClaimPermission... permissions
    ) {
        if (Bananaclaims.PERMISSION_SERVICE.has(
                source,
                ClaimPermission.ADMIN_ROOT
        )) {
            return true;
        }

        if (permissions == null) {
            return false;
        }

        for (ClaimPermission permission : permissions) {
            if (Bananaclaims.PERMISSION_SERVICE.has(
                    source,
                    permission
            )) {
                return true;
            }
        }

        return false;
    }

    public static int runIfAllowed(
            CommandSourceStack source,
            ClaimPermission permission,
            IntSupplier action
    ) {
        if (!canUse(source, permission)) {
            source.sendFailure(
                    BananaClaimsMessages.text(
                            "command.bananaclaims.permission_denied"
                    )
            );

            return 0;
        }

        return action.getAsInt();
    }
}
