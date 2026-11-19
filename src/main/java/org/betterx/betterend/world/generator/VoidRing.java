package org.betterx.betterend.world.generator;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.betterx.bclib.api.v2.generator.BCLibEndBiomeSource;
import org.betterx.bclib.api.v2.generator.config.BCLEndBiomeSourceConfig;
import org.betterx.betterend.BetterEnd;
import org.betterx.worlds.together.world.event.WorldBootstrap;
import org.betterx.worlds.together.worldPreset.TogetherWorldPreset;

public final class VoidRing {
   public static final int DEFAULT_CONFIG_SIZE = 1000;
   public static final long VANILLA_ISLAND_LIMIT = 4096L;
   private static volatile long vanillaIslandLimit = 4096L;
   private static volatile long terrainRadiusSqr = 1000000L;

   private VoidRing() {
   }

   public static long vanillaIslandLimit() {
      return vanillaIslandLimit;
   }

   public static long terrainRadiusSqr() {
      return terrainRadiusSqr;
   }

   public static void onEndLevelInit(BiomeSource biomeSource) {
      if (biomeSource instanceof BCLibEndBiomeSource bcl) {
         long radiusSqr = bcl.getTogetherConfig().innerVoidRadiusSquared;
         vanillaIslandLimit = Math.max(0L, radiusSqr) / 256L;
         terrainRadiusSqr = radiusSqr == BCLEndBiomeSourceConfig.DEFAULT.innerVoidRadiusSquared ? GeneratorOptions.getIslandDistBlockSqr() : radiusSqr;
      } else {
         vanillaIslandLimit = 4096L;
         terrainRadiusSqr = GeneratorOptions.getIslandDistBlockSqr();
      }
   }

   public static Holder<WorldPreset> adaptWorldPreset(Holder<WorldPreset> currentPreset, WorldDimensions worldDims) {
      int size = GeneratorOptions.getIslandDistBlock();
      if (size != 1000 && size > 0) {
         int radiusSqr = (int)Math.min(2147483647L, (long)size * size);
         Set<BCLibEndBiomeSource> sources = Collections.newSetFromMap(new IdentityHashMap<>());
         if (currentPreset != null && currentPreset.value() instanceof TogetherWorldPreset preset) {
            addSource(sources, preset.getDimension(LevelStem.END));
         }

         if (worldDims != null) {
            addSource(sources, (LevelStem)worldDims.dimensions().get(LevelStem.END));
         }

         RegistryAccess access = WorldBootstrap.getLastRegistryAccess();
         if (access != null) {
            access.registry(Registries.LEVEL_STEM).ifPresent(r -> addSource(sources, (LevelStem)r.get(LevelStem.END)));
         }

         for (BCLibEndBiomeSource source : sources) {
            BCLEndBiomeSourceConfig cfg = source.getTogetherConfig();
            if (cfg.innerVoidRadiusSquared == BCLEndBiomeSourceConfig.DEFAULT.innerVoidRadiusSquared) {
               source.setTogetherConfig(
                  new BCLEndBiomeSourceConfig(
                     cfg.mapVersion,
                     cfg.generatorVersion,
                     cfg.withVoidBiomes,
                     radiusSqr,
                     cfg.centerBiomesSize,
                     cfg.voidBiomesSize,
                     cfg.landBiomesSize,
                     cfg.barrensBiomesSize
                  )
               );
            }
         }

         if (!sources.isEmpty()) {
            BetterEnd.LOGGER.info("New world: End void ring radius set to " + size + " blocks (voidRingSize).");
         }

         return currentPreset;
      } else {
         return currentPreset;
      }
   }

   private static void addSource(Set<BCLibEndBiomeSource> sources, LevelStem stem) {
      if (stem != null && stem.generator().getBiomeSource() instanceof BCLibEndBiomeSource bcl) {
         sources.add(bcl);
      }
   }
}
