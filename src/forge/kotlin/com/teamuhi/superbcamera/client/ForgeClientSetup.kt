package com.teamuhi.superbcamera.client

import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLPaths
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.nio.file.Path

/** Forge client wiring, called from the mod constructor on the physical client. */
object ForgeClientSetup {

    fun init() {
        Platform.install(object : PlatformServices {
            override val configDir: Path get() = FMLPaths.CONFIGDIR.get()
            override fun isModLoaded(id: String): Boolean = ModList.get().isLoaded(id)
        })
        ClientSetup.init()
        MOD_BUS.addListener { event: RegisterKeyMappingsEvent ->
            event.register(CameraKeys.EDITOR)
            event.register(CameraKeys.TOGGLE)
        }
        FORGE_BUS.register(ForgeClientEvents)
    }
}
