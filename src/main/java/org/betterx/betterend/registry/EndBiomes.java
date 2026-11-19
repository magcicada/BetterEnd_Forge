package org.betterx.betterend.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.betterx.bclib.api.v2.LifeCycleAPI;
import org.betterx.bclib.api.v2.generator.BiomePicker;
import org.betterx.bclib.api.v2.generator.BiomePicker.ActualBiome;
import org.betterx.bclib.api.v2.generator.map.hex.HexBiomeMap;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeRegistry;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI.BiomeType;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.biome.cave.EmptyAuroraCaveBiome;
import org.betterx.betterend.world.biome.cave.EmptyEndCaveBiome;
import org.betterx.betterend.world.biome.cave.EmptySmaragdantCaveBiome;
import org.betterx.betterend.world.biome.cave.EndCaveBiome;
import org.betterx.betterend.world.biome.cave.JadeCaveBiome;
import org.betterx.betterend.world.biome.cave.LushAuroraCaveBiome;
import org.betterx.betterend.world.biome.cave.LushSmaragdantCaveBiome;
import org.betterx.betterend.world.generator.GeneratorOptions;

public class EndBiomes {
   public static final BiomeType END_CAVE = new BiomeType("END_CAVE", BiomeType.END_IGNORE);
   public static BiomePicker CAVE_BIOMES = null;
   private static HexBiomeMap caveBiomeMap;
   private static long lastSeed;
   public static final ResourceKey<Biome> AMBER_LAND = cKey("amber_land");
   public static final ResourceKey<Biome> BLOSSOMING_SPIRES = cKey("blossoming_spires");
   public static final ResourceKey<Biome> CHORUS_FOREST = cKey("chorus_forest");
   public static final ResourceKey<Biome> CRYSTAL_MOUNTAINS = cKey("crystal_mountains");
   public static final ResourceKey<Biome> DRAGON_GRAVEYARDS = cKey("dragon_graveyards");
   public static final ResourceKey<Biome> DRY_SHRUBLAND = cKey("dry_shrubland");
   public static final ResourceKey<Biome> DUST_WASTELANDS = cKey("dust_wastelands");
   public static final ResourceKey<Biome> FOGGY_MUSHROOMLAND = cKey("foggy_mushroomland");
   public static final ResourceKey<Biome> FLOWER_ISLETS = cKey("flower_islets");
   public static final ResourceKey<Biome> GLOWING_GRASSLANDS = cKey("glowing_grasslands");
   public static final ResourceKey<Biome> ICE_STARFIELD = cKey("ice_starfield");
   public static final ResourceKey<Biome> LANTERN_WOODS = cKey("lantern_woods");
   public static final ResourceKey<Biome> MEGALAKE = cKey("megalake");
   public static final ResourceKey<Biome> MEGALAKE_GROVE = cKey("megalake_grove");
   public static final ResourceKey<Biome> NEON_OASIS = cKey("neon_oasis");
   public static final ResourceKey<Biome> PAINTED_MOUNTAINS = cKey("painted_mountains");
   public static final ResourceKey<Biome> SHADOW_FOREST = cKey("shadow_forest");
   public static final ResourceKey<Biome> SULPHUR_SPRINGS = cKey("sulphur_springs");
   public static final ResourceKey<Biome> UMBRA_VALLEY = cKey("umbra_valley");
   public static final ResourceKey<Biome> UMBRELLA_JUNGLE = cKey("umbrella_jungle");
   public static final ResourceKey<Biome> WATERFALL_PONDS = cKey("waterfall_ponds");
   public static final ResourceKey<Biome> EMPTY_AURORA_CAVE = cKey("empty_aurora_cave");
   public static final ResourceKey<Biome> EMPTY_END_CAVE = cKey("empty_end_cave");
   public static final ResourceKey<Biome> EMPTY_SMARAGDANT_CAVE = cKey("empty_smaragdant_cave");
   public static final ResourceKey<Biome> JADE_CAVE = cKey("jade_cave");
   public static final ResourceKey<Biome> LUSH_AURORA_CAVE = cKey("lush_aurora_cave");
   public static final ResourceKey<Biome> LUSH_SMARAGDANT_CAVE = cKey("lush_smaragdant_cave");

   private static ResourceKey<Biome> cKey(String path) {
      return ResourceKey.create(Registries.BIOME, BetterEnd.makeID(path));
   }

   public static void register() {
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("biome"), EndBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("cave_biome"), EndCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("empty_aurora_cave_biome"), EmptyAuroraCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("empty_end_cave_biome"), EmptyEndCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("empty_smaragdant_cave_biome"), EmptySmaragdantCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("jade_cave_biome"), JadeCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("lush_aurora_cave_biome"), LushAuroraCaveBiome.KEY_CODEC);
      BCLBiomeRegistry.registerBiomeCodec(BetterEnd.makeID("lush_smaragdant_cave_biome"), LushSmaragdantCaveBiome.KEY_CODEC);
      LifeCycleAPI.onLevelLoad(EndBiomes::onWorldLoad);
   }

   private static synchronized void onWorldLoad(ServerLevel level, long seed, Registry<Biome> registry) {
      boolean newPicker = false;
      if (CAVE_BIOMES == null || CAVE_BIOMES.biomeRegistry != registry) {
         BiomePicker picker = new BiomePicker(registry);
         registry.stream()
            .filter(biome -> registry.getResourceKey(biome).isPresent())
            .map(biome -> registry.getHolderOrThrow((ResourceKey)registry.getResourceKey(biome).get()))
            .map(biome -> ((ResourceKey)biome.unwrapKey().orElseThrow()).location())
            .filter(id -> BiomeAPI.wasRegisteredAs(id, END_CAVE))
            .map(BiomeAPI::getBiome)
            .filter(bcl -> !BCLBiomeRegistry.isEmptyBiome(bcl))
            .filter(BCLBiome::isPickable)
            .forEach(picker::addBiome);
         picker.rebuild();
         CAVE_BIOMES = picker;
         newPicker = true;
      }

      if (newPicker || caveBiomeMap == null || lastSeed != seed) {
         caveBiomeMap = new HexBiomeMap(seed, GeneratorOptions.getBiomeSizeCaves(), CAVE_BIOMES);
         lastSeed = seed;
      }
   }

   public static EndBiome registerSubBiomeIntegration(EndBiome.Config biomeConfig) {
      return EndBiome.create(biomeConfig, BiomeType.END_LAND);
   }

   public static void addSubBiomeIntegration(EndBiome biome, ResourceLocation parent) {
      BCLBiome parentBiome = BiomeAPI.getBiome(parent);
      if (!BCLBiomeRegistry.isEmptyBiome(parentBiome) && biome.getParentBiome().getID().equals(biome.getID())) {
         parentBiome.addSubBiome(biome);
      }
   }

   public static ActualBiome getCaveBiome(int x, int z) {
      return caveBiomeMap.getBiome(x, 5.0, z);
   }
}
