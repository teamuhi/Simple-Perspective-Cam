package com.teamuhi.superbcamera.client

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.common.NeoForge
import java.nio.file.Path

/** NeoForge client wiring, called from the mod constructor on the physical client. */
object NeoForgeClientSetup {

    fun init(modBus: IEventBus) {
        Platform.install(object : PlatformServices {
            override val configDir: Path get() = FMLPaths.CONFIGDIR.get()
            override fun isModLoaded(id: String): Boolean = ModList.get().isLoaded(id)
        })
        ClientSetup.init()
        modBus.addListener { event: RegisterKeyMappingsEvent ->
            event.register(CameraKeys.EDITOR)
            event.register(CameraKeys.TOGGLE)
        }
        NeoForge.EVENT_BUS.register(NeoForgeClientEvents)
    }
}
