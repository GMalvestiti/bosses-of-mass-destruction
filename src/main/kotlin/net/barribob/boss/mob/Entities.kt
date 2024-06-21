package net.barribob.boss.mob

import me.shedaniel.autoconfig.AutoConfig
import net.barribob.boss.Mod
import net.barribob.boss.animation.PauseAnimationTimer
import net.barribob.boss.config.ModConfig
import net.barribob.boss.item.ChargedEnderPearlEntity
import net.barribob.boss.item.SoulStarEntity
import net.barribob.boss.mob.mobs.gauntlet.*
import net.barribob.boss.mob.mobs.lich.*
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithArmorRenderer
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithBoneLight
import net.barribob.boss.mob.mobs.obsidilith.ObsidilithEntity
import net.barribob.boss.mob.mobs.void_blossom.*
import net.barribob.boss.mob.utils.SimpleLivingGeoRenderer
import net.barribob.boss.particle.ClientParticleBuilder
import net.barribob.boss.particle.ParticleFactories
import net.barribob.boss.particle.Particles
import net.barribob.boss.projectile.*
import net.barribob.boss.projectile.comet.CometCodeAnimations
import net.barribob.boss.projectile.comet.CometProjectile
import net.barribob.boss.render.*
import net.barribob.boss.utils.ModColors
import net.barribob.maelstrom.general.data.WeakHashPredicate
import net.barribob.maelstrom.static_utilities.RandomUtils
import net.barribob.maelstrom.static_utilities.VecUtils
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.entity.FlyingItemEntityRenderer
import net.minecraft.client.util.GlfwUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.world.World

object Entities {
    private val mobConfig = AutoConfig.getConfigHolder(ModConfig::class.java).config

    val LICH: EntityType<LichEntity> = registerConfiguredMob("lich",
        { type, world -> LichEntity(type, world, mobConfig.lichConfig) })
    { it.dimensions(1.8f, 3.0f).trackingTickInterval(1) }

    val MAGIC_MISSILE: EntityType<MagicMissileProjectile> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("blue_fireball"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::MagicMissileProjectile)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )

    val COMET: EntityType<CometProjectile> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("comet"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::CometProjectile)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )

    val SOUL_STAR: EntityType<SoulStarEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("soul_star"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::SoulStarEntity)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )

    val CHARGED_ENDER_PEARL: EntityType<ChargedEnderPearlEntity> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("charged_ender_pearl"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::ChargedEnderPearlEntity)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )

    val OBSIDILITH: EntityType<ObsidilithEntity> = registerConfiguredMob("obsidilith",
        { type, world -> ObsidilithEntity(type, world, mobConfig.obsidilithConfig) },
        { it.makeFireImmune().dimensions(2.0f, 4.4f) })

    val GAUNTLET: EntityType<GauntletEntity> = registerConfiguredMob("gauntlet",
        { type, world -> GauntletEntity(type, world, mobConfig.gauntletConfig) },
        { it.makeFireImmune().dimensions(5.0f, 4.0f).eyeHeight(4.0f * 0.4f) })

    val VOID_BLOSSOM: EntityType<VoidBlossomEntity> = registerConfiguredMob("void_blossom",
        { type, world -> VoidBlossomEntity(type, world, mobConfig.voidBlossomConfig) },
        { it.makeFireImmune().dimensions(8.0f, 10.0f).maxTrackingRange(3) })

    val SPORE_BALL: EntityType<SporeBallProjectile> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("spore_ball"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::SporeBallProjectile)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )

    val PETAL_BLADE: EntityType<PetalBladeProjectile> = Registry.register(
        Registries.ENTITY_TYPE,
        Mod.identifier("petal_blade"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, ::PetalBladeProjectile)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build()
    )


    private val killCounter = LichKillCounter(mobConfig.lichConfig.summonMechanic)

    private fun <T : Entity> registerConfiguredMob(
        name: String,
        factory: (EntityType<T>, World) -> T,
        augment: (EntityType.Builder<T>) -> EntityType.Builder<T>,
    ): EntityType<T> {
        val builder = EntityType.Builder.create(factory, SpawnGroup.MONSTER)
        return Registry.register(Registries.ENTITY_TYPE,  Mod.identifier(name), augment(builder).build())
    }

    fun init() {
        if(mobConfig.lichConfig.summonMechanic.isEnabled) {
            ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(killCounter)
        }

        FabricDefaultAttributeRegistry.register(LICH,
            HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 5.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, mobConfig.lichConfig.health)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, mobConfig.lichConfig.missile.damage)
        )

        FabricDefaultAttributeRegistry.register(OBSIDILITH,
            HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, mobConfig.obsidilithConfig.health)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, mobConfig.obsidilithConfig.attack)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 10.0)
                .add(EntityAttributes.GENERIC_ARMOR, mobConfig.obsidilithConfig.armor)
        )

        FabricDefaultAttributeRegistry.register(GAUNTLET, HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FLYING_SPEED, 4.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, mobConfig.gauntletConfig.health)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 10.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, mobConfig.gauntletConfig.attack)
            .add(EntityAttributes.GENERIC_ARMOR, mobConfig.gauntletConfig.armor))

        FabricDefaultAttributeRegistry.register(VOID_BLOSSOM,
            HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, mobConfig.voidBlossomConfig.health)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, mobConfig.voidBlossomConfig.attack)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 10.0)
                .add(EntityAttributes.GENERIC_ARMOR, mobConfig.voidBlossomConfig.armor)
        )
    }

    fun clientInit() {
        val pauseSecondTimer = PauseAnimationTimer({ GlfwUtil.getTime() }, { MinecraftClient.getInstance().isPaused })

        EntityRendererRegistry.register(LICH) { context ->
            val texture = Mod.identifier("textures/entity/lich.png")
            SimpleLivingGeoRenderer(
                context, GeoModel(
                    { Mod.identifier("geo/lich.geo.json") },
                    { texture },
                    Mod.identifier("animations/lich.animation.json"),
                    LichCodeAnimations(),
                    { RenderLayer.getEntityCutoutNoCull(it) }
                ),
                BoundedLighting(5),
                LichBoneLight(),
                EternalNightRenderer(),
            )
        }

        EntityRendererRegistry.register(OBSIDILITH) { context ->
            val runeColorHandler = ObsidilithBoneLight()
            val modelProvider = GeoModel<ObsidilithEntity>(
                { Mod.identifier("geo/obsidilith.geo.json") },
                { Mod.identifier("textures/entity/obsidilith.png") },
                Mod.identifier("animations/obsidilith.animation.json"),
            )
            val armorRenderer = ObsidilithArmorRenderer(modelProvider, context)
            val obsidilithRenderer = SimpleLivingGeoRenderer(
                context,
                modelProvider,
                renderer = CompositeRenderer(armorRenderer, runeColorHandler),
                renderWithModel = armorRenderer,
                iBoneLight = runeColorHandler,
                deathRotation = false
            )
            obsidilithRenderer
        }

        val missileTexture = Mod.identifier("textures/entity/blue_magic_missile.png")
        val magicMissileRenderLayer = RenderLayer.getEntityCutoutNoCull(missileTexture)
        EntityRendererRegistry.register(MAGIC_MISSILE) { context ->
            SimpleEntityRenderer(
                context,
                CompositeRenderer(
                    BillboardRenderer(context.renderDispatcher, magicMissileRenderLayer) { 0.5f },
                    ConditionalRenderer(
                        WeakHashPredicate<MagicMissileProjectile> { FrameLimiter(20f, pauseSecondTimer)::canDoFrame },
                        LerpedPosRenderer {
                            ParticleFactories.soulFlame().build(it.add(RandomUtils.randVec().multiply(0.25)))
                        })
                ),
                { missileTexture },
                FullRenderLight()
            )
        }

        EntityRendererRegistry.register(SOUL_STAR) { context ->
            FlyingItemEntityRenderer(context, 1.0f, true)
        }

        EntityRendererRegistry.register(CHARGED_ENDER_PEARL) { context ->
            FlyingItemEntityRenderer(context, 1.0f, true)
        }

        EntityRendererRegistry.register(COMET) { context ->
            SimpleLivingGeoRenderer(context, GeoModel(
                { Mod.identifier("geo/comet.geo.json") },
                { Mod.identifier("textures/entity/comet.png") },
                Mod.identifier("animations/comet.animation.json"),
                CometCodeAnimations()
            ),
                renderer = ConditionalRenderer(
                    WeakHashPredicate { FrameLimiter(60f, pauseSecondTimer)::canDoFrame },
                    LerpedPosRenderer {
                        ParticleFactories.cometTrail().build(it.add(RandomUtils.randVec().multiply(0.5)))
                    }),
                brightness = FullRenderLight()
            )
        }

        EntityRendererRegistry.register(GAUNTLET) { context ->
            val modelProvider = GeoModel(
                { Mod.identifier("geo/gauntlet.geo.json") },
                GauntletTextureProvider(),
                Mod.identifier("animations/gauntlet.animation.json"),
                GauntletCodeAnimations()
            )
            val energyRenderer = GauntletEnergyRenderer(modelProvider, context)
            val overlayOverride = GauntletOverlay()
            SimpleLivingGeoRenderer(
                context, modelProvider,
                renderer = CompositeRenderer(
                    GauntletLaserRenderer(),
                    ConditionalRenderer(
                        WeakHashPredicate { FrameLimiter(20f, pauseSecondTimer)::canDoFrame },
                        LaserParticleRenderer()
                    ),
                    energyRenderer,
                    overlayOverride
                ),
                renderWithModel = energyRenderer,
                deathRotation = false,
                overlayOverride = overlayOverride
            )
        }

        EntityRendererRegistry.register(VOID_BLOSSOM) { context ->
            val texture = Mod.identifier("textures/entity/void_blossom.png")
            val modelProvider = GeoModel(
                { Mod.identifier("geo/void_blossom.geo.json") },
                { texture },
                Mod.identifier("animations/void_blossom.animation.json"),
                VoidBlossomCodeAnimations()
            )
            val boneLight = VoidBlossomBoneLight()
            val overlay = NoRedOnDeathOverlay()
            SimpleLivingGeoRenderer(
                context, modelProvider, deathRotation = false,
                renderer = CompositeRenderer(VoidBlossomSpikeRenderer(), boneLight, overlay),
                iBoneLight = boneLight, overlayOverride = overlay
            )
        }

        EntityRendererRegistry.register(SPORE_BALL) { context ->
            val explosionFlasher = SporeBallOverlay()
            SimpleLivingGeoRenderer(context, GeoModel(
                { Mod.identifier("geo/comet.geo.json") },
                { Mod.identifier("textures/entity/spore.png") },
                Mod.identifier("animations/comet.animation.json"),
                SporeCodeAnimations()
            ),
                renderer = CompositeRenderer(ConditionalRenderer(
                    WeakHashPredicate { FrameLimiter(60f, pauseSecondTimer)::canDoFrame },
                    LerpedPosRenderer {
                        val projectileParticles = ClientParticleBuilder(Particles.OBSIDILITH_BURST)
                            .color(ModColors.GREEN)
                            .colorVariation(0.4)
                            .scale(0.5f)
                            .brightness(Particles.FULL_BRIGHT)
                        projectileParticles.build(it.add(RandomUtils.randVec().multiply(0.25)), VecUtils.yAxis.multiply(0.1))
                    }), explosionFlasher,
                SporeBallSizeRenderer()),
                brightness = FullRenderLight(),
                overlayOverride = explosionFlasher
            )
        }

        val petalTexture = Mod.identifier("textures/entity/petal_blade.png")
        val petalBladeRenderLayer = RenderLayer.getEntityCutoutNoCull(petalTexture)
        EntityRendererRegistry.register(PETAL_BLADE) { context ->
            SimpleEntityRenderer(
                context,
                CompositeRenderer(
                    PetalBladeRenderer(context.renderDispatcher, petalBladeRenderLayer),
                    ConditionalRenderer(
                        WeakHashPredicate { FrameLimiter(30f, pauseSecondTimer)::canDoFrame },
                        PetalBladeParticleRenderer()
                    )
                ),
                { petalTexture },
                FullRenderLight()
            )
        }
    }
}