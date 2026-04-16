package org.betterx.betterend.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class WallPlantOnLogFeature extends WallPlantFeature {
    @Override
    public boolean canGenerate(
            WallPlantFeatureConfig cfg,
            WorldGenLevel world,
            RandomSource random,
            BlockPos pos,
            Direction dir
    ) {
        if (!super.canGenerate(cfg, world, random, pos, dir)) {
            return false;
        }
        BlockPos blockPos = pos.relative(dir.getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.is(BlockTags.LOGS)) {
            return true;
        }
        clearCachedPlantState();
        return false;
    }
}
