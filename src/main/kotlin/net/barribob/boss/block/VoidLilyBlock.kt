package net.barribob.boss.block

import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.FlowerBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class VoidLilyBlock(
    private val factory: (FabricBlockEntityTypeBuilder.Factory<VoidLilyBlockEntity>)?,
    settings: Settings?
) : FlowerBlock(StatusEffects.GLOWING, 0f, settings), BlockEntityProvider {
    override fun createBlockEntity(pos: BlockPos?, state: BlockState?): BlockEntity? = factory?.create(pos, state)
    override fun <T : BlockEntity?> getTicker(
        world: World?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ): BlockEntityTicker<T> = BlockEntityTicker { tickerWorld, pos, tickerState, blockEntity ->
        if (blockEntity is VoidLilyBlockEntity) VoidLilyBlockEntity.tick(
            tickerWorld,
            pos,
            tickerState,
            blockEntity
        )
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: Item.TooltipContext?,
        tooltip: MutableList<Text>?,
        options: TooltipType?
    ) {
        tooltip?.add(Text.translatable("block.bosses_of_mass_destruction.void_lily.tooltip").formatted(Formatting.DARK_GRAY))
    }
}