package org.betterx.betterend.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.betterx.bclib.util.BlocksHelper;

public final class GrowthPlacer {
   private final LevelAccessor world;
   private final List<BlockPos> placed;

   public GrowthPlacer(LevelAccessor world) {
      this.world = world;
      this.placed = world instanceof Level ? new ArrayList<>() : null;
   }

   public void set(BlockPos pos, BlockState state) {
      BlocksHelper.setWithoutUpdate(this.world, pos, state);
      if (this.placed != null) {
         this.placed.add(pos.immutable());
      }
   }

   public void notifyNeighbours() {
      if (this.placed != null && this.world instanceof Level level) {
         for (BlockPos pos : this.placed) {
            BlockState state = level.getBlockState(pos);
            level.updateNeighborsAt(pos, state.getBlock());
            state.updateNeighbourShapes(level, pos, 2);
         }

         this.placed.clear();
      }
   }
}
