package net.barribob.boss.particle

import net.barribob.maelstrom.static_utilities.RandomUtils
import net.barribob.maelstrom.static_utilities.VecUtils
import net.barribob.maelstrom.static_utilities.coerceAtLeast
import net.barribob.maelstrom.static_utilities.coerceAtMost
import net.minecraft.client.particle.ParticleTextureSheet
import net.minecraft.client.particle.SpriteBillboardParticle
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class SimpleParticle(
    private val particleContext: ParticleContext,
    particleAge: Int,
    private val particleGeometry: IParticleGeometry,
    private val cycleSprites: Boolean = true,
    doCollision: Boolean = true
) :
    SpriteBillboardParticle(
        particleContext.world,
        particleContext.pos.x,
        particleContext.pos.y,
        particleContext.pos.z
    ) {

    private var brightnessOverride: ((Float) -> Int)? = null
    private var colorOverride: ((Float) -> Vec3d)? = null
    private var scaleOverride: ((Float) -> Float)? = null
    private var colorVariation: Vec3d = Vec3d.ZERO
    private var velocityOverride: ((SimpleParticle) -> Vec3d)? = null
    private var positionOverride: ((SimpleParticle) -> Vec3d)? = null
    private var rotationOverride: ((SimpleParticle) -> Float)? = null

    private var rotation = 0f
    private var prevRotation = 0f
    var ageRatio = 1f
        private set

    override fun getType(): ParticleTextureSheet = ParticleTextureSheet.PARTICLE_SHEET_OPAQUE

    fun getPos(): Vec3d{
        return Vec3d(x, y, z)
    }

    fun getAge(): Int = age

    override fun tick() {
        super.tick()
        if (isAlive) {
            if (cycleSprites) setSpriteForAge(particleContext.spriteProvider)
            ageRatio = age / maxAge.toFloat()
            setColorFromOverride(colorOverride, ageRatio)
            setScaleFromOverride(scaleOverride, ageRatio)
            setVelocityFromOverride(velocityOverride)
            setPositionFromOverride(positionOverride)
            setRotationFromOverride(rotationOverride)
        }
    }

    private fun setRotationFromOverride(rotationOverride: ((SimpleParticle) -> Float)?) {
        if (rotationOverride != null) {
            val rot = rotationOverride(this)
            prevRotation = rotation
            rotation = rot
        }
    }

    private fun setVelocityFromOverride(velocityOverride: ((SimpleParticle) -> Vec3d)?) {
        if (velocityOverride != null) {
            val velocity = velocityOverride(this)
            velocityX = velocity.x
            velocityY = velocity.y
            velocityZ = velocity.z
        }
    }

    private fun setPositionFromOverride(positionOverride: ((SimpleParticle) -> Vec3d)?) {
        if (positionOverride != null) {
            val pos = positionOverride(this)
            setPos(pos.x, pos.y, pos.z)
        }
    }

    private fun setScaleFromOverride(scaleOverride: ((Float) -> Float)?, ageRatio: Float) {
        if (scaleOverride != null) {
            scale = scaleOverride(ageRatio)
            setBoundingBoxSpacing(0.2f * scale, 0.2f * scale)
        }
    }

    private fun setColorFromOverride(colorOverride: ((Float) -> Vec3d)?, ageRatio: Float) {
        if (colorOverride != null) {
            val color = colorOverride(ageRatio)
            val variedColor = color.add(colorVariation).coerceAtLeast(Vec3d.ZERO).coerceAtMost(VecUtils.unit)
            setColor(variedColor.x.toFloat(), variedColor.y.toFloat(), variedColor.z.toFloat())
        }
    }

    fun setBrightnessOverride(override: ((Float) -> Int)?) {
        brightnessOverride = override
    }

    fun setColorOverride(override: ((Float) -> Vec3d)?) {
        colorOverride = override
        setColorFromOverride(override, 0f)
    }

    fun setScaleOverride(override: ((Float) -> Float)?) {
        scaleOverride = override
        setScaleFromOverride(override, 0f)
    }

    fun setColorVariation(variation: Double) {
        colorVariation = RandomUtils.randVec().multiply(variation)
        setColorFromOverride(colorOverride, 0f)
    }

    fun setVelocityOverride(override: ((SimpleParticle) -> Vec3d)?) {
        velocityOverride = override
    }

    fun setPositionOverride(override: ((SimpleParticle) -> Vec3d)?) {
        positionOverride = override
    }

    fun setRotationOverride(override: ((SimpleParticle) -> Float)?) {
        rotationOverride = override
        rotationOverride?.let {
            rotation = it(this)
            prevRotation = it(this)
        }
    }

    override fun getBrightness(tint: Float): Int =
        brightnessOverride?.invoke(ageRatio) ?: super.getBrightness(tint)

    override fun buildGeometry(vertexConsumer: VertexConsumer?, camera: Camera, tickDelta: Float) {
        val vector3fs = particleGeometry.getGeometry(
            camera,
            tickDelta,
            prevPosX, prevPosY, prevPosZ,
            x, y, z,
            getSize(tickDelta),
            MathHelper.lerp(tickDelta, prevRotation, rotation)
        )

        val l = this.minU
        val m = this.maxU
        val n = this.minV
        val o = this.maxV
        val p = getBrightness(tickDelta)
        vertexConsumer!!.vertex(
            vector3fs[0].x, vector3fs[0].y,
            vector3fs[0].z
        ).texture(m, o).color(red, green, blue, alpha).light(p)
        vertexConsumer.vertex(
            vector3fs[1].x, vector3fs[1].y,
            vector3fs[1].z
        ).texture(m, n).color(red, green, blue, alpha).light(p)
        vertexConsumer.vertex(
            vector3fs[2].x, vector3fs[2].y,
            vector3fs[2].z
        ).texture(l, n).color(red, green, blue, alpha).light(p)
        vertexConsumer.vertex(
            vector3fs[3].x, vector3fs[3].y,
            vector3fs[3].z
        ).texture(l, o).color(red, green, blue, alpha).light(p)
    }

    init {
        this.maxAge = particleAge
        if (cycleSprites) setSpriteForAge(particleContext.spriteProvider) else setSprite(particleContext.spriteProvider)
        velocityX = particleContext.vel.x
        velocityY = particleContext.vel.y
        velocityZ = particleContext.vel.z
        collidesWithWorld = doCollision
    }
}