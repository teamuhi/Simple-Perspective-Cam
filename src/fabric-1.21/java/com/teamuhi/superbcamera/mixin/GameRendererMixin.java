package com.teamuhi.superbcamera.mixin;

import com.teamuhi.superbcamera.client.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric stand-in for NeoForge's {@code ViewportEvent.ComputeFov}. The camera takeover itself is {@link CameraMixin}.
 *
 * The very high priority makes this run after the injections other mods put at the same point, like the lowest
 * priority event listener does on NeoForge.
 */
@Mixin(value = GameRenderer.class, priority = 10000)
public abstract class GameRendererMixin {

    /**
     * Replaces the field of view of the main pass. {@code useFovSetting} is false for the hand pass, which keeps its
     * own fov (same rule as {@code ViewportEvent.ComputeFov#usedConfiguredFov}).
     */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void superbcamera$overrideFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (!useFovSetting) return;
        Double fov = CameraController.INSTANCE.fovOverride();
        if (fov != null) cir.setReturnValue(fov);
    }
}
