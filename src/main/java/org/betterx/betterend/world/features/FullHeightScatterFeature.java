package org.betterx.betterend.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.util.GlobalState;

public abstract class FullHeightScatterFeature extends DefaultFeature {
   private final int radius;

   public FullHeightScatterFeature(int radius) {
      this.radius = radius;
   }

   public abstract boolean canGenerate(WorldGenLevel var1, RandomSource var2, BlockPos var3, BlockPos var4, float var5);

   public abstract void generate(WorldGenLevel var1, RandomSource var2, BlockPos var3);

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      MutableBlockPos POS = GlobalState.stateForThread().POS;
      RandomSource random = featureConfig.random();
      BlockPos center = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      int maxY = world.getHeight(Types.WORLD_SURFACE_WG, center.getX(), center.getZ());
      int minY = BlocksHelper.upRay(world, new BlockPos(center.getX(), 0, center.getZ()), maxY);

      for (int y = maxY; y > minY; y--) {
         POS.set(center.getX(), y, center.getZ());
         if (world.getBlockState(POS).isAir() && !world.getBlockState(POS.below()).isAir()) {
            float r = MHelper.randRange(this.radius * 0.5F, (float)this.radius, random);
            int count = MHelper.floor(r * r * MHelper.randRange(1.5F, 3.0F, random));

            for (int i = 0; i < count; i++) {
               float pr = r * (float)Math.sqrt(random.nextFloat());
               float theta = random.nextFloat() * (float) (Math.PI * 2);
               float x = pr * (float)Math.cos(theta);
               float z = pr * (float)Math.sin(theta);
               POS.set(center.getX() + x, y + 5, center.getZ() + z);
               if (GenerationArea.contains(world, POS, 1)) {
                  int down = BlocksHelper.downRay(world, POS, 16);
                  if (down <= 10) {
                     POS.setY(POS.getY() - down);
                     if (this.canGenerate(world, random, center, POS, r)) {
                        this.generate(world, random, POS);
                     }
                  }
               }
            }
         }
      }

      return true;
   }
}
