package com.teamuhi.superbcamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.teamuhi.superbcamera.client.CameraController;
import com.teamuhi.superbcamera.client.CameraMath;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric stand-in for Forge's {@code ViewportEvent.ComputeCameraAngles} and {@code ViewportEvent.ComputeFov}.
 *
 * The very high priority makes these injections run after the ones other mods put at the same point, so the takeover
 * sees (and overrides) whatever they did to the camera, like the lowest priority event listener does on Forge.
 */
@Mixin(value = GameRenderer.class, priority = 10000)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Camera mainCamera;

    /**
     * Right after {@code Camera.setup} and before {@code renderLevel} applies the camera pitch and yaw to the world
     * pose stack. The roll is not part of the vanilla camera, so it is added to that pose stack here, exactly where
     * Forge's {@code ComputeCameraAngles} hook does it.
     */
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void superbcamera$takeOverCamera(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        Camera camera = this.mainCamera;
        CameraMath.Angles angles = CameraController.INSTANCE.onCameraSetup(
                camera, partialTick, camera.getYRot(), camera.getXRot(), 0.0F
        );
        if (angles != null) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(angles.getRoll()));
        }
    }

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
