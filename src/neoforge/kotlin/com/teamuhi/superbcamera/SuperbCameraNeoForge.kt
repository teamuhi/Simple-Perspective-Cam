package com.teamuhi.superbcamera

import com.teamuhi.superbcamera.client.NeoForgeClientSetup
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod

/**
 * NeoForge entry point (client dist only: on a dedicated server the class is never constructed, so no client class is
 * touched). The mod event bus is injected by the loader through the constructor.
 */
@Mod(value = SuperbCamera.MOD_ID, dist = [Dist.CLIENT])
class SuperbCameraNeoForge(modBus: IEventBus) {

    init {
        NeoForgeClientSetup.init(modBus)
    }
}
