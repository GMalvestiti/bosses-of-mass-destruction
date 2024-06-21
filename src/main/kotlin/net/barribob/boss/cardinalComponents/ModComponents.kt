package net.barribob.boss.cardinalComponents

import net.barribob.boss.Mod
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.ladysnake.cca.api.v3.component.ComponentKey
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer
import java.util.*

class ModComponents : WorldComponentInitializer, EntityComponentInitializer {
    companion object : IWorldEventScheduler, IPlayerMoveHistory {
        private val eventSchedulerComponentKey: ComponentKey<IWorldEventSchedulerComponent> =
            ComponentRegistryV3.INSTANCE.getOrCreate(
                Mod.identifier("event_scheduler"),
                IWorldEventSchedulerComponent::class.java
            )

        private val playerMoveHistoryComponentKey: ComponentKey<IPlayerMoveHistoryComponent> =
            ComponentRegistryV3.INSTANCE.getOrCreate(
                Mod.identifier("player_move_history"),
                IPlayerMoveHistoryComponent::class.java
            )

        private val chunkBlockCacheComponentKey: ComponentKey<IChunkBlockCacheComponent> =
            ComponentRegistryV3.INSTANCE.getOrCreate(
                Mod.identifier("chunk_block_cache_component"),
                IChunkBlockCacheComponent::class.java
            )

        override fun getWorldEventScheduler(world: World) = eventSchedulerComponentKey.get(world).get()

        override fun getPlayerPositions(serverPlayerEntity: ServerPlayerEntity): List<Vec3d> =
            playerMoveHistoryComponentKey.get(serverPlayerEntity).getHistoricalPositions()

        fun getChunkBlockCache(world: World): Optional<IChunkBlockCacheComponent> =
            chunkBlockCacheComponentKey.maybeGet(world)
    }

    override fun registerWorldComponentFactories(registry: WorldComponentFactoryRegistry) {
        registry.register(eventSchedulerComponentKey, WorldEventScheduler::class.java, ::WorldEventScheduler)
        registry.register(chunkBlockCacheComponentKey, ChunkBlockCacheComponent::class.java, ::ChunkBlockCacheComponent)
    }

    override fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
        registry.registerFor(ServerPlayerEntity::class.java, playerMoveHistoryComponentKey, ::PlayerMoveHistory)
    }
}