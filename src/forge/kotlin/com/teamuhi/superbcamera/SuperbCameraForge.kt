package com.teamuhi.superbcamera

import com.teamuhi.superbcamera.client.ForgeClientSetup
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment

/**
 * Forge entry point. Simple Perspective Camera is client-only: on a dedicated server nothing is registered and no client class is
 * touched.
 */
@Mod(SuperbCamera.MOD_ID)
object SuperbCameraForge {

    init {
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClientSetup.init()
    }
}
