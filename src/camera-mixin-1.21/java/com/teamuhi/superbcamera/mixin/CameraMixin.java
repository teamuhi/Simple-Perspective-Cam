package com.teamuhi.superbcamera.mixin;

import com.teamuhi.superbcamera.client.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The camera takeover on Minecraft 1.21+, shared by every loader.
 *
 * Since 1.21 the loader's {@code ViewportEvent.ComputeCameraAngles} is posted at the start of {@code Camera.setup}
 * (NeoForge), before the position is set and before the third-person offset is applied, so it can no longer move the
 * camera. This injection runs at the very end of {@code Camera.setup} instead, when the vanilla camera is final, and
 * replaces the position and rotation (including the roll, which the renderer reads from {@code Camera.rotation()}).
 *
 * The very high priority makes it run after the injections other mods (Superb Warfare) put at the same point, so the
 * takeover sees and overrides whatever they did to the camera.
 */
@Mixin(value = Camera.class, priority = 10000)
public abstract class CameraMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    private void superbcamera$takeOverCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        CameraController.INSTANCE.onCameraSetup(camera, partialTick, camera.getYRot(), camera.getXRot(), 0.0F);
    }
}
