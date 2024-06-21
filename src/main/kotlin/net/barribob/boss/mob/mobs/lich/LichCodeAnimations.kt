package net.barribob.boss.mob.mobs.lich

import net.barribob.boss.mob.GeoModel
import net.barribob.boss.mob.utils.animation.ICodeAnimations
import net.barribob.maelstrom.static_utilities.MathUtils
import net.minecraft.util.math.MathHelper
import software.bernie.geckolib.animation.AnimationState

class LichCodeAnimations : ICodeAnimations<LichEntity> {
    override fun animate(animatable: LichEntity, data: AnimationState<*>, geoModel: GeoModel<LichEntity>) {
        val bodyYaw = MathHelper.lerpAngleDegrees(data.partialTick, animatable.prevBodyYaw, animatable.bodyYaw)
        val headYaw = MathHelper.lerpAngleDegrees(data.partialTick, animatable.prevHeadYaw, animatable.headYaw)

        val headPitch = MathHelper.lerp(data.partialTick, animatable.prevPitch, animatable.pitch)

        val velocity = MathUtils.lerpVec(data.partialTick, animatable.velocityHistory.get(1), animatable.velocityHistory.get())

        val neutralPoseDegree = 30
        val maxDegreeVariation = 15
        val bodyPitch = (MathUtils.directionToPitch(velocity) * (maxDegreeVariation / 90.0)) + neutralPoseDegree

        val yaw = headYaw - bodyYaw
        val adjustedHeadPitch = headPitch - bodyPitch

        val model = geoModel.getBakedModel(geoModel.getModelResource(animatable))
        model.getBone("code_root").ifPresent { it.rotX = -Math.toRadians(bodyPitch).toFloat() }
        model.getBone("headBase").ifPresent { it.rotX = -Math.toRadians(adjustedHeadPitch).toFloat() }
        model.getBone("headBase").ifPresent { it.rotY = Math.toRadians(yaw.toDouble()).toFloat() }
    }
}