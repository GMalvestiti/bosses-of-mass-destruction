package net.barribob.invasion.mob

import net.barribob.maelstrom.MaelstromMod
import net.barribob.maelstrom.general.event.TimedEvent
import net.barribob.maelstrom.mob.BaseEntity
import net.barribob.maelstrom.mob.ai.TimedAttackGoal
import net.barribob.maelstrom.mob.animation.GeckolibAnimationManager
import net.barribob.maelstrom.static_utilities.ClientServerUtils
import net.barribob.maelstrom.static_utilities.MobUtils
import net.barribob.maelstrom.static_utilities.yOffset
import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.goal.SwimGoal
import net.minecraft.entity.ai.goal.WanderAroundFarGoal
import net.minecraft.entity.attribute.AttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.HostileEntity.createHostileAttributes
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class MaelstromScoutEntity(entityType: EntityType<out MaelstromScoutEntity>, world: World) : BaseEntity(entityType, world) {

    override fun initializeGeckoManager(): GeckolibAnimationManager<out BaseEntity> =
        GeckolibAnimationManager(this, MaelstromScoutAnimations())

    override fun getAttributes(): AttributeContainer {
        val attributeBuilder = createHostileAttributes()
        attributeBuilder.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 25.0)
        return AttributeContainer(attributeBuilder.build())
    }

    override fun initGoals() {
        super.initGoals()
        this.goalSelector.add(1, SwimGoal(this))
        this.goalSelector.add(3, TimedAttackGoal(this, 3F, 2.5F, 5, ::handleAttack))
        this.goalSelector.add(4, WanderAroundFarGoal(this, 1.0))
        this.targetSelector.add(1, MobUtils.getRevengeGoal(this))
        this.targetSelector.add(2, MobUtils.getTargetSelectGoal(this))
    }

    private fun handleAttack(): Int {
        MobUtils.leapTowards(this, this.target!!.pos, 0.4, 0.0)
        ClientServerUtils.startAnimation(this, "attack")

        val shouldCancel = { this.target == null || !this.isAlive || this.health <= 0 }
        val callback = {
            val pos: Vec3d = this.pos.yOffset(1.0).add(this.rotationVector)
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F / (this.random.nextFloat() * 0.4F + 0.8F))
            MobUtils.handleAreaImpact(
                0.6,
                this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat(),
                this,
                pos,
                DamageSource.mob(this),
                0.20,
                damageDecay = false
            )
        }
        MaelstromMod.serverEventScheduler.addEvent(
            TimedEvent(
                callback,
                10,
                shouldCancel = shouldCancel
            )
        )

        return 20
    }
}