package org.betterx.betterend.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.betterx.bclib.behaviours.interfaces.BehaviourWaterPlantSeed;
import org.betterx.bclib.blocks.UnderwaterPlantWithAgeBlock;
import org.betterx.bclib.blocks.BlockProperties.TripleShape;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.util.GrowthPlacer;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class EndLilySeedBlock extends UnderwaterPlantWithAgeBlock implements BehaviourWaterPlantSeed {
   @Override
   public void grow(WorldGenLevel world, RandomSource random, BlockPos pos) {
      if (this.canGrow(world, pos)) {
         GrowthPlacer placer = new GrowthPlacer(world);
         placer.set(pos, (BlockState)EndBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.BOTTOM));

         BlockPos up;
         for (up = pos.above(); world.getFluidState(up).isSource(); up = up.above()) {
            placer.set(up, (BlockState)EndBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.MIDDLE));
         }

         placer.set(up, (BlockState)EndBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.TOP));
         placer.notifyNeighbours();
      }
   }

   private boolean canGrow(WorldGenLevel world, BlockPos pos) {
      BlockPos up = pos.above();

      while (world.getBlockState(up).getFluidState().getType().equals(Fluids.WATER.getSource())) {
         up = up.above();
      }

      return world.isEmptyBlock(up);
   }

   @Override
   protected boolean isTerrain(BlockState state) {
      return state.is(CommonBlockTags.END_STONES);
   }
}
