package org.betterx.betterend.mixin.common;

import org.betterx.betterend.registry.EndBlocks;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow private BlockState blockState;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z"
            )
    )
    private boolean be_skipRecursiveSlabReplaceCheck(BlockState targetState, BlockPlaceContext context) {
        if (this.blockState != null && this.blockState.is(EndBlocks.ENDSTONE_DUST) && targetState.getBlock() instanceof SlabBlock) {
            return false;
        }
        return targetState.canBeReplaced(context);
    }
}
