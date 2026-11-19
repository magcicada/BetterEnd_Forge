package org.betterx.betterend.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.util.GlobalState;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public abstract class ScatterFeature<FC extends ScatterFeatureConfig> extends Feature<FC> {
   public ScatterFeature(Codec<FC> codec) {
      super(codec);
   }

   public abstract boolean canGenerate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4, BlockPos var5, float var6);

   public abstract void generate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4);

   protected void clearPlacementCache() {
   }

   protected BlockPos getCenterGround(FC cfg, WorldGenLevel world, BlockPos pos) {
      return world.isEmptyBlock(pos) && world.getBlockState(pos.below()).is(CommonBlockTags.END_STONES) ? pos : DefaultFeature.getPosOnSurfaceWG(world, pos);
   }

   protected boolean canSpawn(FC cfg, WorldGenLevel world, BlockPos pos) {
      return pos.getY() < 5 ? false : world.getBlockState(pos.below()).is(CommonBlockTags.END_STONES);
   }

   protected boolean getGroundPlant(FC cfg, WorldGenLevel world, MutableBlockPos pos) {
      int down = BlocksHelper.downRay(world, pos, 16);
      if (down > Math.abs(this.getYOffset() * 2)) {
         return false;
      } else {
         pos.setY(pos.getY() - down);
         return true;
      }
   }

   protected int getYOffset() {
      return 5;
   }

   protected int getChance() {
      return 1;
   }

   public boolean place(FeaturePlaceContext<FC> featureConfig) {
      FC cfg = (FC)featureConfig.config();
      MutableBlockPos POS = GlobalState.stateForThread().POS;
      RandomSource random = featureConfig.random();
      BlockPos center = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      center = this.getCenterGround(cfg, world, center);
      if (!this.canSpawn(cfg, world, center)) {
         return false;
      } else {
         float r = MHelper.randRange(cfg.radius * 0.5F, (float)cfg.radius, random);
         int count = MHelper.floor(r * r * MHelper.randRange(1.5F, 3.0F, random));
         synchronized (this) {
            for (int i = 0; i < count; i++) {
               float pr = r * (float)Math.sqrt(random.nextFloat());
               float theta = random.nextFloat() * (float) (Math.PI * 2);
               float x = pr * (float)Math.cos(theta);
               float z = pr * (float)Math.sin(theta);
               POS.set(center.getX() + x, center.getY() + this.getYOffset(), center.getZ() + z);
               if (GenerationArea.contains(world, POS, 1) && this.getGroundPlant(cfg, world, POS)) {
                  boolean canGenerate = this.canGenerate(cfg, world, random, center, POS, r);
                  boolean shouldGenerate = canGenerate && (this.getChance() < 2 || random.nextInt(this.getChance()) == 0);
                  if (shouldGenerate) {
                     this.generate(cfg, world, random, POS);
                  } else {
                     this.clearPlacementCache();
                  }
               }
            }

            return true;
         }
      }
   }
}
