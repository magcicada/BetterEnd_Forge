package org.betterx.betterend.world.features.terrain;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeRegistry;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBiomes;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.features.bushes.TenaneaBushFeature;

public class FloatingSpireFeature extends SpireFeature {
   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      ChunkGenerator chunkGenerator = featureConfig.chunkGenerator();
      int minY = getYOnSurface(world, pos.getX(), pos.getZ());
      int y = minY > 57 ? MHelper.floor(MHelper.randRange(minY, minY * 2, random) * 0.5F + 32.0F) : MHelper.randRange(64, 192, random);
      pos = new BlockPos(pos.getX(), y, pos.getZ());
      SDF sdf = new SDFSphere().setRadius(MHelper.randRange(2, 3, random)).setBlock(Blocks.END_STONE);
      int count = MHelper.randRange(3, 5, random);

      for (int i = 0; i < count; i++) {
         float rMin = i * 1.3F + 2.5F;
         sdf = this.addSegment(sdf, MHelper.randRange(rMin, rMin + 1.5F, random), random);
      }

      for (int i = count - 1; i > 0; i--) {
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
      });
      sdf.fillRecursive(world, pos);
      support.forEach(
         bpos -> {
            if (GenerationArea.contains(world, bpos)) {
               BCLBiome biome = BiomeAPI.getBiome(GenerationArea.getBiome(world, bpos));
               if (!BCLBiomeRegistry.isEmptyBiome(biome) && biome.is(EndBiomes.BLOSSOMING_SPIRES)) {
                  ((TenaneaBushFeature)EndFeatures.TENANEA_BUSH.getFeature())
                     .place(new FeaturePlaceContext(Optional.empty(), world, chunkGenerator, random, bpos, null));
               }
            }
         }
      );
      return true;
   }
}
