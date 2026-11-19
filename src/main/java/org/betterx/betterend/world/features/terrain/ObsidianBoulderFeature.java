package org.betterx.betterend.world.features.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class ObsidianBoulderFeature extends DefaultFeature {
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      pos = getPosOnSurface(world, new BlockPos(pos.getX() + random.nextInt(16), pos.getY(), pos.getZ() + random.nextInt(16)));
      if (!GenerationArea.getBlockState(world, pos.below()).is(CommonBlockTags.END_STONES)) {
         return false;
      } else {
         int count = MHelper.randRange(1, 5, random);

         for (int i = 0; i < count; i++) {
            BlockPos p = getPosOnSurface(
               world, new BlockPos(pos.getX() + random.nextInt(16) - 8, pos.getY(), pos.getZ() + random.nextInt(16) - 8)
            );
            this.makeBoulder(world, p, random);
         }

         return true;
      }
   }

   private void makeBoulder(WorldGenLevel world, BlockPos pos, RandomSource random) {
      if (GenerationArea.getBlockState(world, pos.below()).is(CommonBlockTags.END_STONES)) {
         float radius = MHelper.randRange(1.0F, 5.0F, random);
         SDF sphere = new SDFSphere().setRadius(radius).setBlock(Blocks.OBSIDIAN);
         float sx = MHelper.randRange(0.7F, 1.3F, random);
         float sy = MHelper.randRange(0.7F, 1.3F, random);
         float sz = MHelper.randRange(0.7F, 1.3F, random);
         sphere = new SDFScale3D().setScale(sx, sy, sz).setSource(sphere);
         OpenSimplexNoise noise = new OpenSimplexNoise(random.nextLong());
         sphere = new SDFDisplacement().setFunction(vec -> (float)(noise.eval(vec.x() * 0.2, vec.y() * 0.2, vec.z() * 0.2) * 1.5)).setSource(sphere);
         BlockState mossy = EndBlocks.MOSSY_OBSIDIAN.defaultBlockState();
         sphere.addPostProcess(info -> info.getStateUp().isAir() && random.nextFloat() > 0.1F ? mossy : info.getState())
            .setReplaceFunction(state -> state.is(CommonBlockTags.GEN_END_STONES) || BlocksHelper.replaceableOrPlant(state))
            .fillRecursive(world, pos);
      }
   }
}
