package com.teamuhi.superbcamera.client

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

/**
 * The only door to Superb Warfare. Superb Warfare is an optional dependency reached by reflection ([SbwImpl]), so this
 * mod builds without its jar and one jar runs with or without it. When the mod is missing, or its API cannot be
 * resolved (one warning is logged), every function degrades to "no vehicle" / null and never throws.
 */
object SbwBridge {

    private const val MOD_ID = "superbwarfare"

    private val modLoaded: Boolean by lazy { Platform.isModLoaded(MOD_ID) }

    /**
     * True when Superb Warfare is installed and its API has not been found unusable. The mod list is cached (it does
     * not change after startup); the reflection handles are resolved on the first real call.
     */
    val available: Boolean get() = modLoaded && SbwImpl.usable

    /** The first Superb Warfare vehicle in the riding chain of [entity] (the entity itself included), or null. */
    fun findVehicle(entity: Entity?): Entity? {
        if (!available || entity == null) return null
        return SbwImpl.findVehicle(entity)
    }

    /** True when [entity] is a Superb Warfare vehicle. */
    fun isVehicle(entity: Entity): Boolean = available && SbwImpl.isVehicle(entity)

    /** Current and maximum health of a vehicle, or null when [entity] is not one. */
    fun health(entity: Entity): Pair<Float, Float>? = if (available) SbwImpl.health(entity) else null

    /** Pivot and rotation of a Superb Warfare vehicle, or null when [vehicle] is not one. */
    fun frame(vehicle: Entity, partialTick: Float, inheritPitch: Boolean, inheritRoll: Boolean): Pair<Vec3, Quaternionf>? =
        if (available) SbwImpl.frame(vehicle, partialTick, inheritPitch, inheritRoll) else null
}
