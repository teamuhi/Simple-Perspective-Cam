package com.teamuhi.superbcamera.client

import com.teamuhi.superbcamera.SuperbCamera
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Reflection access to Superb Warfare, shared by every node so the same jar works with or without the mod installed
 * and no Superb Warfare jar is needed to build. Only Superb Warfare's own members go through reflection; vanilla
 * [Entity] members are used through normal compiled code (their names are obfuscated in production on Forge).
 *
 * Everything is resolved once, lazily, on the first use. If the class or any required member is missing (mod absent,
 * or an API that changed in another Superb Warfare version) one warning is logged and the integration is disabled for
 * the rest of the session: the public functions then return null / false and never throw. Only reached through
 * [SbwBridge].
 */
internal object SbwImpl {

    private const val VEHICLE_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity"

    private const val UNRESOLVED = 0
    private const val READY = 1
    private const val DISABLED = 2

    @Volatile
    private var state = UNRESOLVED

    // Handles, valid when state == READY.
    private lateinit var vehicleClass: Class<*>
    private lateinit var transformMethod: Method
    private lateinit var pitchMethod: Method
    private lateinit var rollMethod: Method
    private lateinit var maxHealthMethod: Method
    private var healthMethod: Method? = null
    private var healthField: Field? = null

    /** False once the integration failed to resolve or threw; true while untried or working. */
    val usable: Boolean get() = state != DISABLED

    /** Resolves the reflection handles once ([hint] widens the class loaders tried). True when they are usable. */
    private fun ready(hint: Entity?): Boolean {
        when (state) {
            READY -> return true
            DISABLED -> return false
        }
        synchronized(this) {
            if (state == UNRESOLVED) {
                try {
                    resolve(hint)
                    state = READY
                } catch (t: Throwable) {
                    disable("could not resolve the Superb Warfare vehicle API", t)
                }
            }
        }
        return state == READY
    }

    private fun disable(reason: String, t: Throwable) {
        synchronized(this) {
            if (state == DISABLED) return
            state = DISABLED
        }
        SuperbCamera.LOGGER.warn(
            "Superb Warfare vehicle support disabled: {} ({}). Vehicle pitch/roll following and vehicle health are " +
                    "unavailable; everything else works normally.", reason, t.toString()
        )
    }

    private fun resolve(hint: Entity?) {
        val cls = findClass(hint) ?: throw ClassNotFoundException(VEHICLE_CLASS)
        val float = Float::class.javaPrimitiveType!!
        transformMethod = cls.getMethod("getVehicleYOffsetTransform", float)
        require(Matrix4d::class.java.isAssignableFrom(transformMethod.returnType)) {
            "getVehicleYOffsetTransform returns ${transformMethod.returnType.name}, expected org.joml.Matrix4d"
        }
        pitchMethod = cls.getMethod("getPitch", float)
        rollMethod = cls.getMethod("getRoll", float)
        maxHealthMethod = cls.getMethod("getMaxHealth")
        // `health` is a property of the Kotlin / Java class: prefer the getter, fall back to a public field.
        healthMethod = try {
            cls.getMethod("getHealth")
        } catch (_: NoSuchMethodException) {
            null
        }
        if (healthMethod == null) healthField = cls.getField("health")
        vehicleClass = cls
    }

    /** Looks the vehicle class up through every class loader that could see it, without initialising it. */
    private fun findClass(hint: Entity?): Class<*>? {
        // The hint's own class hierarchy: a Superb Warfare vehicle has the class among its superclasses.
        var c: Class<*>? = hint?.javaClass
        while (c != null) {
            if (c.name == VEHICLE_CLASS) return c
            c = c.superclass
        }
        val loaders = LinkedHashSet<ClassLoader>()
        hint?.javaClass?.classLoader?.let { loaders.add(it) }
        SbwImpl::class.java.classLoader?.let { loaders.add(it) }
        Thread.currentThread().contextClassLoader?.let { loaders.add(it) }
        for (loader in loaders) {
            try {
                return Class.forName(VEHICLE_CLASS, false, loader)
            } catch (_: ClassNotFoundException) {
            } catch (_: LinkageError) {
            }
        }
        return try {
            Class.forName(VEHICLE_CLASS)
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    /** Runs [block]; any failure disables the integration (one warning) and yields null. */
    private inline fun <T> guarded(what: String, block: () -> T?): T? = try {
        block()
    } catch (t: Throwable) {
        disable("$what failed", t.cause ?: t)
        null
    }

    fun isVehicle(entity: Entity): Boolean = ready(entity) && vehicleClass.isInstance(entity)

    fun findVehicle(entity: Entity): Entity? {
        if (!ready(entity)) return null
        var current: Entity? = entity
        while (current != null) {
            if (vehicleClass.isInstance(current)) return current
            current = current.vehicle
        }
        return null
    }

    fun health(entity: Entity): Pair<Float, Float>? {
        if (!ready(entity) || !vehicleClass.isInstance(entity)) return null
        return guarded("reading the vehicle health") {
            val health = healthMethod?.invoke(entity) ?: healthField?.get(entity)
            val max = maxHealthMethod.invoke(entity)
            if (health is Number && max is Number) health.toFloat() to max.toFloat() else null
        }
    }

    fun frame(entity: Entity, partialTick: Float, inheritPitch: Boolean, inheritRoll: Boolean): Pair<Vec3, Quaternionf>? {
        if (!ready(entity) || !vehicleClass.isInstance(entity)) return null
        return guarded("reading the vehicle transform") {
            val transform = transformMethod.invoke(entity, partialTick) as? Matrix4d ?: return@guarded null
            val pivot = transform.getTranslation(Vector3d())
            val rotation = if (inheritPitch && inheritRoll) {
                val q = transform.getNormalizedRotation(Quaterniond())
                Quaternionf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())
            } else {
                CameraMath.body(
                    Mth.rotLerp(partialTick, entity.yRotO, entity.yRot).toDouble(),
                    if (inheritPitch) (pitchMethod.invoke(entity, partialTick) as Number).toDouble() else 0.0,
                    if (inheritRoll) (rollMethod.invoke(entity, partialTick) as Number).toDouble() else 0.0
                )
            }
            Vec3(pivot.x, pivot.y, pivot.z) to rotation
        }
    }
}
