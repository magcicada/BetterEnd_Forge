package org.betterx.betterend.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.primitive.SDFCappedCone;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.biome.EndBiome;

public class BiomeIslandFeature extends DefaultFeature {
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      int dist = BlocksHelper.downRay(world, pos, 10) + 1;
      BlockPos surfacePos = new BlockPos(pos.getX(), pos.getY() - dist, pos.getZ());
      BlockState topMaterial = EndBiome.findTopMaterial(world, surfacePos);
      BlockState topBlock;
      BlockState underBlock;
      if (BlocksHelper.isFluid(topMaterial)) {
         topBlock = Blocks.GRAVEL.defaultBlockState();
         underBlock = Blocks.STONE.defaultBlockState();
      } else {
         topBlock = Blocks.GRASS_BLOCK.defaultBlockState();
         underBlock = EndBiome.findUnderMaterial(world, surfacePos);
      }

      createSDFIsland(pos.immutable(), new OpenSimplexNoise(world.getSeed()), topBlock, underBlock).fillRecursive(world, pos.below());
      return true;
   }

   private static SDF createSDFIsland(BlockPos center, OpenSimplexNoise simplexNoise, BlockState topBlock, BlockState underBlock) {
      SDF sdfCone = new SDFCappedCone().setRadius1(0.0F).setRadius2(6.0F).setHeight(4.0F).setBlock(pos -> {
         if (pos.getY() > center.getY()) {
            return AIR;
         } else {
            return pos.getY() == center.getY() ? topBlock : underBlock;
         }
      });
      sdfCone = new SDFTranslate().setTranslate(0.0F, -2.0F, 0.0F).setSource(sdfCone);
      return new SDFDisplacement()
         .setFunction(
            pos -> {
               float deltaX = Math.abs(pos.x());
               float deltaY = Math.abs(pos.y());
               float deltaZ = Math.abs(pos.z());
               return !(deltaY < 2.0F) || !(deltaX < 3.0F) && !(deltaZ < 3.0F)
                  ? (float)simplexNoise.eval(center.getX() + pos.x(), center.getY() + pos.y(), center.getZ() + pos.z())
                  : 0.0F;
            }
         )
         .setSource(sdfCone)
         .setReplaceFunction(state -> BlocksHelper.isFluid(state) || state.canBeReplaced());
   }
}
