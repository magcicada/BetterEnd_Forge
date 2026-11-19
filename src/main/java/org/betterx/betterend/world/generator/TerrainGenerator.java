package org.betterx.betterend.world.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.awt.Point;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.NoiseChunk.NoiseInterpolator;
import org.betterx.bclib.api.v2.generator.config.BCLEndBiomeSourceConfig;
import org.betterx.bclib.api.v2.generator.config.BCLEndBiomeSourceConfig.EndBiomeGeneratorType;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.interfaces.BETargetChecker;
import org.betterx.betterend.mixin.common.NoiseBasedChunkGeneratorAccessor;
import org.betterx.betterend.mixin.common.NoiseChunkAccessor;
import org.betterx.betterend.mixin.common.NoiseInterpolatorAccessor;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.worlds.together.biomesource.BiomeSourceWithConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class TerrainGenerator {
   private static final Map<Point, TerrainBoolCache> TERRAIN_BOOL_CACHE_MAP = Maps.newHashMap();
   private static final ReentrantLock LOCKER = new ReentrantLock();
   private static final Point POS = new Point();
   private static final double SCALE_XZ = 8.0;
   private static final double SCALE_Y = 4.0;
   private static final float[] COEF;
   private static final Point[] OFFS;
   private static IslandLayer largeIslands;
   private static IslandLayer mediumIslands;
   private static IslandLayer smallIslands;
   private static OpenSimplexNoise noise1;
   private static OpenSimplexNoise noise2;
   private static BiomeSource biomeSource;
   public static BCLEndBiomeSourceConfig config;
   private static Sampler sampler;

   public static void initNoise(long seed, BiomeSource biomeSource, Sampler sampler) {
      config = resolveEndConfig(biomeSource);
      if (config == null) {
         BetterEnd.LOGGER.warning("End biome source config is not set, falling back to defaults.");
         config = BCLEndBiomeSourceConfig.DEFAULT;
      }

      RandomSource random = new LegacyRandomSource(seed);
      largeIslands = new IslandLayer(random.nextInt(), GeneratorOptions.bigOptions);
      mediumIslands = new IslandLayer(random.nextInt(), GeneratorOptions.mediumOptions);
      smallIslands = new IslandLayer(random.nextInt(), GeneratorOptions.smallOptions);
      noise1 = new OpenSimplexNoise(random.nextInt());
      noise2 = new OpenSimplexNoise(random.nextInt());
      TERRAIN_BOOL_CACHE_MAP.clear();
      TerrainGenerator.biomeSource = biomeSource;
      TerrainGenerator.sampler = sampler;
   }

   @Nullable
   private static BCLEndBiomeSourceConfig resolveEndConfig(BiomeSource biomeSource) {
      BiomeSource source = biomeSource;

      for (int depth = 0; depth < 8 && source != null; depth++) {
         if (source instanceof BiomeSourceWithConfig<?, ?> bcl && bcl.getTogetherConfig() instanceof BCLEndBiomeSourceConfig resolved) {
            return resolved;
         }

         BiomeSource original = TerrainGenerator.BlueprintCompat.unwrapOriginalSource(source);
         if (original == null || original == source) {
            break;
         }

         source = original;
      }

      return null;
   }

   public static void fillTerrainDensity(double[] buffer, int posX, int posZ, int scaleXZ, int scaleY, int maxHeight) {
      LOCKER.lock();

      try {
         float fadeOutDist = 27.0F;
         float fadOutStart = maxHeight - 28.0F;
         largeIslands.clearCache();
         mediumIslands.clearCache();
         smallIslands.clearCache();
         int x = Mth.floor(posX / scaleXZ);
         int z = Mth.floor(posZ / scaleXZ);
         double distortion1 = noise1.eval(x * 0.1, z * 0.1) * 20.0 + noise2.eval(x * 0.2, z * 0.2) * 10.0 + noise1.eval(x * 0.4, z * 0.4) * 5.0;
         double distortion2 = noise2.eval(x * 0.1, z * 0.1) * 20.0 + noise1.eval(x * 0.2, z * 0.2) * 10.0 + noise2.eval(x * 0.4, z * 0.4) * 5.0;
         double px = (double)x * scaleXZ + distortion1;
         double pz = (double)z * scaleXZ + distortion2;
         largeIslands.updatePositions(px, pz, maxHeight);
         mediumIslands.updatePositions(px, pz, maxHeight);
         smallIslands.updatePositions(px, pz, maxHeight);
         float height = getAverageDepth(x << 1, z << 1) * 0.5F;

         for (int y = 0; y < buffer.length; y++) {
            double py = (double)y * scaleY;
            float dist = largeIslands.getDensity(px, py, pz, height);
            dist = dist > 1.0F ? dist : MHelper.max(dist, mediumIslands.getDensity(px, py, pz, height));
            dist = dist > 1.0F ? dist : MHelper.max(dist, smallIslands.getDensity(px, py, pz, height));
            if (dist > -0.5F) {
               dist = (float)(dist + (noise1.eval(px * 0.01, py * 0.01, pz * 0.01) * 0.02 + 0.02));
               dist = (float)(dist + (noise2.eval(px * 0.05, py * 0.05, pz * 0.05) * 0.01 + 0.01));
               dist = (float)(dist + (noise1.eval(px * 0.1, py * 0.1, pz * 0.1) * 0.005 + 0.005));
            }

            if (py >= maxHeight) {
               dist = -1.0F;
            } else if (py > fadOutStart) {
               dist = (float)Mth.lerp((py - fadOutStart) / 27.0, dist, -1.0);
            }

            buffer[y] = dist;
         }
      } finally {
         LOCKER.unlock();
      }
   }

   private static float getAverageDepth(int x, int z) {
      if (biomeSource == null) {
         return 0.0F;
      } else {
         BCLBiome biome = getBiome(biomeSource, x, z);
         if (biome != null && biome.settings.getTerrainHeight() < 0.1F) {
            return 0.0F;
         } else {
            float depth = 0.0F;

            for (int i = 0; i < OFFS.length; i++) {
               int px = x + OFFS[i].x;
               int pz = z + OFFS[i].y;
               biome = getBiome(biomeSource, px, pz);
               depth += biome == null ? 0.0F : biome.settings.getTerrainHeight() * COEF[i];
            }

            return depth;
         }
      }
   }

   @Nullable
   private static BCLBiome getBiome(BiomeSource biomeSource, int x, int z) {
      return BiomeAPI.getBiome(biomeSource.getNoiseBiome(x, 0, z, sampler));
   }

   public static Boolean isLand(int x, int z, int maxHeight) {
      int sectionX = TerrainBoolCache.scaleCoordinate(x);
      int sectionZ = TerrainBoolCache.scaleCoordinate(z);
      int stepY = (int)Math.ceil(maxHeight / 4.0);
      LOCKER.lock();

      Boolean px;
      try {
         POS.setLocation(sectionX, sectionZ);
         TerrainBoolCache section = TERRAIN_BOOL_CACHE_MAP.get(POS);
         if (section == null) {
            if (TERRAIN_BOOL_CACHE_MAP.size() > 64) {
               TERRAIN_BOOL_CACHE_MAP.clear();
            }

            section = new TerrainBoolCache();
            TERRAIN_BOOL_CACHE_MAP.put(new Point(POS.x, POS.y), section);
         }

         byte value = section.getData(x, z);
         if (value <= 0) {
            double pxx = (x >> 1) + 0.5;
            double pz = (z >> 1) + 0.5;
            double distortion1 = noise1.eval(pxx * 0.1, pz * 0.1) * 20.0 + noise2.eval(pxx * 0.2, pz * 0.2) * 10.0 + noise1.eval(pxx * 0.4, pz * 0.4) * 5.0;
            double distortion2 = noise2.eval(pxx * 0.1, pz * 0.1) * 20.0 + noise1.eval(pxx * 0.2, pz * 0.2) * 10.0 + noise2.eval(pxx * 0.4, pz * 0.4) * 5.0;
            double var25 = pxx * 8.0 + distortion1;
            pz = pz * 8.0 + distortion2;
            largeIslands.updatePositions(var25, pz, maxHeight);
            mediumIslands.updatePositions(var25, pz, maxHeight);
            smallIslands.updatePositions(var25, pz, maxHeight);
            boolean result = false;

            for (int y = 0; y < stepY; y++) {
               double py = y * 4.0;
               float dist = largeIslands.getDensity(var25, py, pz);
               dist = dist > 1.0F ? dist : MHelper.max(dist, mediumIslands.getDensity(var25, py, pz));
               dist = dist > 1.0F ? dist : MHelper.max(dist, smallIslands.getDensity(var25, py, pz));
               if (dist > -0.5F) {
                  dist = (float)(dist + (noise1.eval(var25 * 0.01, py * 0.01, pz * 0.01) * 0.02 + 0.02));
                  dist = (float)(dist + (noise2.eval(var25 * 0.05, py * 0.05, pz * 0.05) * 0.01 + 0.01));
                  dist = (float)(dist + (noise1.eval(var25 * 0.1, py * 0.1, pz * 0.1) * 0.005 + 0.005));
               }

               if (dist > -0.01) {
                  result = true;
                  break;
               }
            }

            section.setData(x, z, (byte)(result ? 2 : 1));
            return result;
         }

         px = value > 1;
      } finally {
         LOCKER.unlock();
      }

      return px;
   }

   public static void onServerLevelInit(ServerLevel level, LevelStem levelStem, long seed) {
      if (level.dimension() == Level.END) {
         ChunkGenerator chunkGenerator = levelStem.generator();
         if (chunkGenerator instanceof NoiseBasedChunkGenerator) {
            Holder<NoiseGeneratorSettings> sHolder = ((NoiseBasedChunkGeneratorAccessor)chunkGenerator).be_getSettings();
            BCLEndBiomeSourceConfig endConfig = resolveEndConfig(chunkGenerator.getBiomeSource());
            if (endConfig != null) {
               BETargetChecker.class.cast(sHolder.value()).be_setTarget(endConfig.generatorVersion == EndBiomeGeneratorType.PAULEVS);
            } else {
               BETargetChecker.class.cast(sHolder.value()).be_setTarget(false);
            }
         }

         VoidRing.onEndLevelInit(chunkGenerator.getBiomeSource());
         initNoise(seed, chunkGenerator.getBiomeSource(), level.getChunkSource().randomState().sampler());
      }
   }

   public static void makeObsidianPlatform(ServerLevel serverLevel, CallbackInfo info) {
      if (!GeneratorOptions.generateObsidianPlatform()) {
         info.cancel();
      } else if (GeneratorOptions.changeSpawn()) {
         BlockPos blockPos = GeneratorOptions.getSpawn();
         int i = blockPos.getX();
         int j = blockPos.getY() - 2;
         int k = blockPos.getZ();
         BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach(blockPosx -> serverLevel.setBlockAndUpdate(blockPosx, Blocks.AIR.defaultBlockState()));
         BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach(blockPosx -> serverLevel.setBlockAndUpdate(blockPosx, Blocks.OBSIDIAN.defaultBlockState()));
         info.cancel();
      }
   }

   public static void fillSlice(boolean primarySlice, int x, List<NoiseInterpolator> interpolators, NoiseChunkAccessor accessor, NoiseSettings noiseSettings) {
      int sizeY = noiseSettings.getCellHeight();
      int sizeXZ = noiseSettings.getCellWidth();
      int cellSizeXZ = accessor.bnv_getCellCountXZ() + 1;
      int firstCellZ = accessor.bnv_getFirstCellZ();
      x *= sizeXZ;

      for (int cellXZ = 0; cellXZ < cellSizeXZ; cellXZ++) {
         int z = (firstCellZ + cellXZ) * sizeXZ;

         for (NoiseInterpolator noiseInterpolator : interpolators) {
            if (noiseInterpolator instanceof NoiseInterpolatorAccessor interpolator) {
               double[] ds = (primarySlice ? interpolator.be_getSlice0() : interpolator.be_getSlice1())[cellXZ];
               fillTerrainDensity(ds, x, z, sizeXZ, sizeY, noiseSettings.height());
            }
         }
      }
   }

   static {
      float sum = 0.0F;
      List<Float> coef = Lists.newArrayList();
      List<Point> pos = Lists.newArrayList();

      for (int x = -3; x <= 3; x++) {
         for (int z = -3; z <= 3; z++) {
            float dist = MHelper.length((float)x, (float)z) / 3.0F;
            if (dist <= 1.0F) {
               sum += dist;
               coef.add(dist);
               pos.add(new Point(x, z));
            }
         }
      }

      OFFS = pos.toArray(new Point[0]);
      COEF = new float[coef.size()];

      for (int i = 0; i < COEF.length; i++) {
         COEF[i] = coef.get(i) / sum;
      }
   }

   private static final class BlueprintCompat {
      private static final String CLASS_NAME = "com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource";
      private static final String FIELD_NAME = "originalSource";
      private static volatile boolean initialized;
      private static volatile Class<?> moddedBiomeSourceClass;
      private static volatile Field originalSourceField;

      @Nullable
      static BiomeSource unwrapOriginalSource(BiomeSource source) {
         ensureInitialized();
         if (moddedBiomeSourceClass == null || originalSourceField == null) {
            return null;
         } else if (!moddedBiomeSourceClass.isInstance(source)) {
            return null;
         } else {
            try {
               return originalSourceField.get(source) instanceof BiomeSource biomeSource ? biomeSource : null;
            } catch (IllegalAccessException var3) {
               return null;
            }
         }
      }

      private static void ensureInitialized() {
         if (!initialized) {
            initialized = true;

            try {
               Class<?> clazz = Class.forName(
                  "com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource", false, TerrainGenerator.BlueprintCompat.class.getClassLoader()
               );
               Field field = clazz.getDeclaredField("originalSource");
               field.setAccessible(true);
               moddedBiomeSourceClass = clazz;
               originalSourceField = field;
            } catch (NoSuchFieldException | ClassNotFoundException var2) {
               moddedBiomeSourceClass = null;
               originalSourceField = null;
            }
         }
      }
   }
}
