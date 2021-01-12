package net.barribob.invasion.utils

import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.DragonFireballEntityRenderer
import net.minecraft.client.render.entity.EntityRenderDispatcher
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.entity.Entity
import net.minecraft.entity.ExperienceOrbEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.boss.dragon.EnderDragonEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.mob.FlyingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.world.*
import net.minecraft.world.explosion.Explosion.DestructionType

object VanillaCopies {
    /**
     * [FlyingEntity.travel]
     */
    fun travel(movementInput: Vec3d, entity: LivingEntity) {
        when {
            entity.isTouchingWater -> {
                entity.updateVelocity(0.02f, movementInput)
                entity.move(MovementType.SELF, entity.velocity)
                entity.velocity = entity.velocity.multiply(0.800000011920929)
            }
            entity.isInLava -> {
                entity.updateVelocity(0.02f, movementInput)
                entity.move(MovementType.SELF, entity.velocity)
                entity.velocity = entity.velocity.multiply(0.5)
            }
            else -> {
                val baseFrictionCoefficient = 0.91f
                val friction = if (entity.isOnGround) {
                    entity.world.getBlockState(BlockPos(entity.x, entity.y - 1.0, entity.z)).block
                        .slipperiness * baseFrictionCoefficient
                } else {
                    baseFrictionCoefficient
                }
                val g = 0.16277137f / (friction * friction * friction)

                entity.updateVelocity(if (entity.isOnGround) 0.1f * g else 0.02f, movementInput)
                entity.move(MovementType.SELF, entity.velocity)
                entity.velocity = entity.velocity.multiply(friction.toDouble())
            }
        }
        entity.method_29242(entity, false)
    }

    /**
     * Adapted from [MobEntity.lookAtEntity]
     */
    fun MobEntity.lookAtTarget(target: Vec3d, maxYawChange: Float, maxPitchChange: Float) {
        val d: Double = target.x - this.x
        val e: Double = target.z - this.z
        val g: Double = target.y - this.eyeY

        val h = MathHelper.sqrt(d * d + e * e).toDouble()
        val i = (MathHelper.atan2(e, d) * 57.2957763671875).toFloat() - 90.0f
        val j = (-(MathHelper.atan2(g, h) * 57.2957763671875)).toFloat()
        this.pitch = changeAngle(this.pitch, j, maxPitchChange)
        this.yaw = changeAngle(this.yaw, i, maxYawChange)
    }

    /**
     * [MobEntity.changeAngle]
     */
    private fun changeAngle(oldAngle: Float, newAngle: Float, maxChangeInAngle: Float): Float {
        var f = MathHelper.wrapDegrees(newAngle - oldAngle)
        if (f > maxChangeInAngle) {
            f = maxChangeInAngle
        }
        if (f < -maxChangeInAngle) {
            f = -maxChangeInAngle
        }
        return oldAngle + f
    }

    /**
     * [ClientPlayNetworkHandler.onEntitySpawn]
     */
    fun handleClientSpawnEntity(packetContext: PacketContext, packet: EntitySpawnS2CPacket) {
        val d: Double = packet.x
        val e: Double = packet.y
        val f: Double = packet.z
        val entityType = packet.entityTypeId
        val world = packetContext.player.world

        val entity15 = entityType.create(world)

        if (entity15 != null) {
            val i: Int = packet.id
            entity15.updateTrackedPosition(d, e, f)
            entity15.refreshPositionAfterTeleport(d, e, f)
            entity15.pitch = (packet.pitch * 360).toFloat() / 256.0f
            entity15.yaw = (packet.yaw * 360).toFloat() / 256.0f
            entity15.entityId = i
            entity15.uuid = packet.uuid
            val clientWorld = MinecraftClient.getInstance().world
            clientWorld?.addEntity(i, entity15 as Entity?)
        }
    }

    /**
     * [DragonFireballEntityRenderer.render]
     */
    fun renderBillboard(
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int,
        dispatcher: EntityRenderDispatcher,
        layer: RenderLayer,
    ) {
        matrixStack.push()
        matrixStack.multiply(dispatcher.rotation)
        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0f))
        val entry = matrixStack.peek()
        val matrix4f = entry.model
        val matrix3f = entry.normal
        val vertexConsumer = vertexConsumerProvider.getBuffer(layer)
        produceVertex(vertexConsumer, matrix4f, matrix3f, i, 0.0f, 0, 0, 1)
        produceVertex(vertexConsumer, matrix4f, matrix3f, i, 1.0f, 0, 1, 1)
        produceVertex(vertexConsumer, matrix4f, matrix3f, i, 1.0f, 1, 1, 0)
        produceVertex(vertexConsumer, matrix4f, matrix3f, i, 0.0f, 1, 0, 0)
        matrixStack.pop()
    }

    /**
     * [DragonFireballEntityRenderer.produceVertex]
     */
    private fun produceVertex(
        vertexConsumer: VertexConsumer,
        modelMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        light: Int,
        x: Float,
        y: Int,
        textureU: Int,
        textureV: Int,
    ) {
        vertexConsumer.vertex(modelMatrix, x - 0.5f, y.toFloat() - 0.25f, 0.0f).color(255, 255, 255, 255)
            .texture(textureU.toFloat(),
                textureV.toFloat()).overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(normalMatrix, 0.0f, 1.0f, 0.0f).next()
    }

    /**
     * [LivingEntity.canSee]
     */
    fun hasDirectLineOfSight(to: Vec3d, from: Vec3d, world: BlockView, entity: Entity): Boolean {
        return world.raycast(RaycastContext(to,
            from,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            entity)).type == HitResult.Type.MISS
    }

    /**
     * [EntityRenderer.getBlockLight]
     */
    fun getBlockLight(entity: Entity, blockPos: BlockPos?): Int {
        return if (entity.isOnFire) 15 else entity.world.getLightLevel(LightType.BLOCK, blockPos)
    }

    /**
     * [CreeperEntity.explode]
     */
    fun getEntityDestructionType(world: World): DestructionType {
        return if (world.gameRules.getBoolean(GameRules.DO_MOB_GRIEFING)) DestructionType.DESTROY else DestructionType.NONE
    }

    /**
     * Adapted from [EnderDragonEntity.awardExperience]
     */
    fun awardExperience(amount: Int, pos: Vec3d, world: World) {
        var amt = amount
        while (amt > 0) {
            val i = ExperienceOrbEntity.roundToOrbSize(amt)
            amt -= i
            world.spawnEntity(ExperienceOrbEntity(world, pos.x, pos.y, pos.z, i))
        }
    }
}