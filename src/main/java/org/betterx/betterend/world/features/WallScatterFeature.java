package org.betterx.betterend.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;

public abstract class WallScatterFeature<FC extends ScatterFeatureConfig> extends Feature<FC> {
   private static final Direction[] DIR = BlocksHelper.makeHorizontal();

   public WallScatterFeature(Codec<FC> codec) {
      super(codec);
   }

   public abstract boolean canGenerate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4, Direction var5);

   public abstract void generate(FC var1, WorldGenLevel var2, RandomSource var3, BlockPos var4, Direction var5);

   public boolean place(FeaturePlaceContext<FC> featureConfig) {
      FC cfg = (FC)featureConfig.config();
      RandomSource random = featureConfig.random();
      BlockPos center = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      Direction[] dirs = (Direction[])DIR.clone();
      int maxY = world.getHeight(Types.WORLD_SURFACE, center.getX(), center.getZ());
      int minY = BlocksHelper.upRay(world, new BlockPos(center.getX(), 0, center.getZ()), maxY);
      if (maxY >= 10 && maxY >= minY) {
         int py = MHelper.randRange(minY, maxY, random);
         synchronized (this) {
            MutableBlockPos mut = new MutableBlockPos();

            for (int x = -cfg.radius; x <= cfg.radius; x++) {
               mut.setX(center.getX() + x);

               for (int y = -cfg.radius; y <= cfg.radius; y++) {
                  mut.setY(py + y);

                  for (int z = -cfg.radius; z <= cfg.radius; z++) {
                     mut.setZ(center.getZ() + z);
                     if (GenerationArea.contains(world, mut, 1) && random.nextInt(4) == 0 && world.isEmptyBlock(mut)) {
                        this.shuffle(random, dirs);

                        for (Direction dir : dirs) {
                           if (this.canGenerate(cfg, world, random, mut, dir)) {
                              this.generate(cfg, world, random, mut, dir);
                              break;
                           }
                        }
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void shuffle(RandomSource random, Direction[] directions) {
      for (int i = 0; i < 4; i++) {
         int j = random.nextInt(4);
         Direction d = directions[i];
         directions[i] = directions[j];
         directions[j] = d;
      }
   }
}
