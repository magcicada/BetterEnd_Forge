package org.betterx.betterend.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.util.GlobalState;

public abstract class InvertedScatterFeature<FC extends ScatterFeatureConfig> extends Feature<FC> {
   public InvertedScatterFeature(Codec<FC> codec) {
      super(codec);
   }

   public abstract boolean canGenerate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4, BlockPos var5, float var6);

   public abstract void generate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4);

   public boolean place(FeaturePlaceContext<FC> featureConfig) {
      FC cfg = (FC)featureConfig.config();
      MutableBlockPos POS = GlobalState.stateForThread().POS;
      RandomSource random = featureConfig.random();
      BlockPos center = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      int maxY = world.getHeight(Types.WORLD_SURFACE, center.getX(), center.getZ());
      int minY = BlocksHelper.upRay(world, new BlockPos(center.getX(), 0, center.getZ()), maxY);
      synchronized (this) {
         for (int y = maxY; y > minY; y--) {
            POS.set(center.getX(), y, center.getZ());
            if (world.getBlockState(POS).isAir() && !world.getBlockState(POS.above()).isAir()) {
               float r = MHelper.randRange(cfg.radius * 0.5F, (float)cfg.radius, random);
               int count = MHelper.floor(r * r * MHelper.randRange(0.5F, 1.5F, random));

               for (int i = 0; i < count; i++) {
                  float pr = r * (float)Math.sqrt(random.nextFloat());
                  float theta = random.nextFloat() * (float) (Math.PI * 2);
                  float x = pr * (float)Math.cos(theta);
                  float z = pr * (float)Math.sin(theta);
                  POS.set(center.getX() + x, center.getY() - 7, center.getZ() + z);
                  if (GenerationArea.contains(world, POS, 1)) {
                     int up = BlocksHelper.upRay(world, POS, 16);
                     if (up <= 14) {
                        POS.setY(POS.getY() + up);
                        if (this.canGenerate(cfg, world, random, center, POS, r)) {
                           this.generate(cfg, world, random, POS);
                        }
                     }
                  }
               }
            }
         }

         return true;
      }
   }
}
