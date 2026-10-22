package com.teamuhi.superbcamera.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teamuhi.superbcamera.client.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric stand-in for the HUD hiding of Forge's {@code RenderGuiOverlayEvent.Pre}: while the active perspective hides
 * the HUD, every layer of {@code Gui.render} is skipped except the debug screen and the chat, which are drawn the way
 * vanilla draws them. Cancelling at the head also hides what other mods add to the end of {@code Gui.render}, and it
 * leaves the player's own F1 state untouched.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private DebugScreenOverlay debugScreen;

    @Shadow
    @Final
    private ChatComponent chat;

    @Shadow
    private int tickCount;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void superbcamera$hideHud(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (!CameraController.INSTANCE.hidesHud()) return;

        if (this.minecraft.options.renderDebug) {
            this.debugScreen.render(graphics);
        }

        // The chat takes the cursor position in GUI units to highlight the hovered message.
        Window window = this.minecraft.getWindow();
        int mouseX = Mth.floor(this.minecraft.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth());
        int mouseY = Mth.floor(this.minecraft.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight());
        RenderSystem.enableBlend();
        this.chat.render(graphics, this.tickCount, mouseX, mouseY);
        ci.cancel();
    }
}
