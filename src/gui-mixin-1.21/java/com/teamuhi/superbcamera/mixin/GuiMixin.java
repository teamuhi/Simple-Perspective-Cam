package com.teamuhi.superbcamera.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamuhi.superbcamera.client.CameraController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD hiding on the loaders without a per-layer cancel event (Fabric, and Forge 1.21 whose layer API cannot cancel
 * vanilla layers); NeoForge's {@code RenderGuiLayerEvent.Pre} does the same job there. While the active perspective hides
 * the HUD, every layer of {@code Gui.render} is skipped except the debug screen and the chat, which are drawn through
 * vanilla's own code. Cancelling at the head also hides what other mods add to the end of {@code Gui.render}, and it
 * leaves the player's own F1 state untouched.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void superbcamera$hideHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!CameraController.INSTANCE.hidesHud()) return;

        RenderSystem.enableDepthTest();
        Gui gui = (Gui) (Object) this;
        DebugScreenOverlay debugOverlay = gui.getDebugOverlay();
        if (debugOverlay.showDebugScreen()) {
            debugOverlay.render(graphics);
        }
        // Vanilla's own chat layer (skipped while the chat screen is open, which draws the chat itself).
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        ((GuiAccessor) gui).superbcamera$renderChat(graphics, deltaTracker);
        graphics.pose().popPose();
        RenderSystem.disableDepthTest();
        ci.cancel();
    }
}
