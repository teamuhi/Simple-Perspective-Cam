package com.teamuhi.superbcamera.mixin;

import com.teamuhi.superbcamera.client.FreeLook;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Shared by both loaders: while Left Alt free-look is active the mouse movement of the frame rotates the custom camera
 * instead of the player, so the player's body and aim are not turned.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void superbcamera$freeLook(LocalPlayer player, double dx, double dy) {
        if (!FreeLook.INSTANCE.onTurn(dx, dy)) {
            player.turn(dx, dy);
        }
    }
}
