package com.bananasandwich.bananaclaims.mixin;

import com.bananasandwich.bananaclaims.Bananaclaims;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes Banana Claims custom text click events to the requesting player. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCustomClickActionMixin {

    @Inject(
            method = "handleCustomClickAction",
            at = @At("TAIL")
    )
    private void bananaclaims$handleCustomClickAction(
            ServerboundCustomClickActionPacket packet,
            CallbackInfo callbackInfo
    ) {
        if (!Bananaclaims.MOD_ID.equals(packet.id().getNamespace())) {
            return;
        }

        if ((Object) this instanceof ServerGamePacketListenerImpl gameListener) {
            Bananaclaims.BOOK_MANAGER.handleCustomClick(
                    gameListener.player,
                    packet.id(),
                    packet.payload()
            );
        }
    }
}
