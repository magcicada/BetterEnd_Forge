package org.betterx.betterend.world.features.terrain;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeRegistry;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.operator.SDFSmoothUnion;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBiomes;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.features.bushes.TenaneaBushFeature;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class SpireFeature extends DefaultFeature {
   protected static final Function<BlockState, Boolean> REPLACE = state -> {
      if (state.is(CommonBlockTags.END_STONES)) {
         return true;
      } else {
         return !(state.getBlock() instanceof LeavesBlock) && !state.is(BlockTags.LEAVES) ? BlocksHelper.replaceableOrPlant(state) : true;
      }
   };

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      ChunkGenerator chunkGenerator = featureConfig.chunkGenerator();
      pos = getPosOnSurfaceWG(world, pos);
      if (pos.getY() >= 10
         && world.getBlockState(pos.below(3)).is(CommonBlockTags.GEN_END_STONES)
         && world.getBlockState(pos.below(6)).is(CommonBlockTags.GEN_END_STONES)) {
         SDF sdf = new SDFSphere().setRadius(MHelper.randRange(2, 3, random)).setBlock(Blocks.END_STONE);
         int count = MHelper.randRange(3, 7, random);

         for (int i = 0; i < count; i++) {
            float rMin = i * 1.3F + 2.5F;
            sdf = this.addSegment(sdf, MHelper.randRange(rMin, rMin + 1.5F, random), random);
         }

         OpenSimplexNoise noise = new OpenSimplexNoise(random.nextLong());
         sdf = new SDFDisplacement()
            .setFunction(
               vec -> (float)(
                  Math.abs(noise.eval(vec.x() * 0.1, vec.y() * 0.1, vec.z() * 0.1)) * 3.0
                     + Math.abs(noise.eval(vec.x() * 0.3, vec.y() * 0.3 + 100.0, vec.z() * 0.3)) * 1.3F
               )
            )
            .setSource(sdf);
         List<BlockPos> support = Lists.newArrayList();
         sdf.setReplaceFunction(REPLACE).addPostProcess(info -> {
            if (info.getStateUp().isAir()) {
               if (random.nextInt(16) == 0) {
                  support.add(info.getPos().above());
               }

               return EndBiome.findTopMaterial(world, info.getPos());
            } else {
               return info.getState(Direction.UP, 3).isAir() ? EndBiome.findUnderMaterial(world, info.getPos()) : info.getState();
            }
         }).fillRecursive(world, pos);
         support.forEach(
            bpos -> {
               if (GenerationArea.contains(world, bpos)) {
                  BCLBiome biome = BiomeAPI.getBiome(GenerationArea.getBiome(world, bpos));
                  if (!BCLBiomeRegistry.isEmptyBiome(biome) && biome.equals(EndBiomes.BLOSSOMING_SPIRES)) {
                     ((TenaneaBushFeature)EndFeatures.TENANEA_BUSH.getFeature())
                        .place(new FeaturePlaceContext(Optional.empty(), world, chunkGenerator, random, bpos, null));
                  }
               }
            }
         );
         return true;
      } else {
         return false;
      }
   }

   protected SDF addSegment(SDF sdf, float radius, RandomSource random) {
      SDF sphere = new SDFSphere().setRadius(radius).setBlock(Blocks.END_STONE);
      SDF offseted = new SDFTranslate().setTranslate(0.0F, radius + random.nextFloat() * 0.25F * radius, 0.0F).setSource(sdf);
      return new SDFSmoothUnion().setRadius(radius * 0.5F).setSourceA(sphere).setSourceB(offseted);
   }
}
