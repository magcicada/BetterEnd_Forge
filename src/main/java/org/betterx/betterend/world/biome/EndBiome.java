package org.betterx.betterend.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeBuilder;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeSettings;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeBuilder.BiomeSupplier;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI.BiomeType;
import org.betterx.bclib.api.v2.levelgen.surface.SurfaceRuleBuilder;
import org.betterx.bclib.interfaces.SurfaceMaterialProvider;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.registry.EndSounds;
import org.betterx.betterend.registry.EndTags;
import org.jetbrains.annotations.NotNull;

public class EndBiome extends BCLBiome implements SurfaceMaterialProvider {
   public static final Codec<EndBiome> CODEC = RecordCodecBuilder.create(
      instance -> codecWithSettings(
            instance,
            Codec.BOOL.fieldOf("has_caves").orElse(true).forGetter(o -> o.hasCaves),
            SurfaceMaterialProvider.CODEC.fieldOf("surface").orElse(EndBiome.Config.DEFAULT_MATERIAL).forGetter(o -> o.surfMatProv)
         )
         .apply(instance, EndBiome::new)
   );
   public static final KeyDispatchDataCodec<EndBiome> KEY_CODEC = KeyDispatchDataCodec.of(CODEC);
   private boolean hasCaves = true;
   protected SurfaceMaterialProvider surfMatProv = EndBiome.Config.DEFAULT_MATERIAL;

   @Override
   public KeyDispatchDataCodec<? extends BCLBiome> codec() {
      return KEY_CODEC;
   }

   protected EndBiome(
      float terrainHeight,
      float fogDensity,
      float genChance,
      int edgeSize,
      boolean vertical,
      Optional<ResourceLocation> edge,
      ResourceLocation biomeID,
      Optional<List<ParameterPoint>> parameterPoints,
      Optional<ResourceLocation> biomeParent,
      Optional<String> intendedType,
      boolean hasCaves,
      SurfaceMaterialProvider surface
   ) {
      super(terrainHeight, fogDensity, genChance, edgeSize, vertical, edge, biomeID, parameterPoints, biomeParent, intendedType);
      this.hasCaves = hasCaves;
      this.surfMatProv = surface;
   }

   void setHasCaves(boolean v) {
      this.hasCaves = v;
   }

   public boolean hasCaves() {
      return this.hasCaves;
   }

   public EndBiome(ResourceKey<Biome> biomeID, BCLBiomeSettings settings) {
      super(biomeID, settings);
   }

   public static EndBiome create(EndBiome.Config biomeConfig, BiomeType type) {
      return create(biomeConfig, type, null);
   }

   public static EndBiome createSubBiome(EndBiome.Config data, @NotNull BCLBiome parentBiome) {
      return create(data, parentBiome.getIntendedType(), parentBiome);
   }

   private static EndBiome create(EndBiome.Config biomeConfig, BiomeType type, BCLBiome parentBiome) {
      BCLBiomeBuilder builder = BCLBiomeBuilder.start(biomeConfig.ID)
         .music(SoundEvents.MUSIC_END)
         .waterColor(BCLBiomeBuilder.DEFAULT_END_WATER_COLOR)
         .waterFogColor(BCLBiomeBuilder.DEFAULT_END_WATER_FOG_COLOR)
         .fogColor(BCLBiomeBuilder.DEFAULT_END_FOG_COLOR)
         .skyColor(BCLBiomeBuilder.DEFAULT_END_SKY_COLOR)
         .mood(EndSounds.AMBIENT_DUST_WASTELANDS)
         .temperature(BCLBiomeBuilder.DEFAULT_END_TEMPERATURE)
         .wetness(BCLBiomeBuilder.DEFAULT_END_WETNESS)
         .parentBiome(parentBiome)
         .precipitation(Precipitation.NONE)
         .surface(biomeConfig.surfaceMaterial().surface().build())
         .type(type);
      biomeConfig.addCustomBuildData(builder);
      EndFeatures.addDefaultFeatures(biomeConfig.ID, builder, biomeConfig.hasCaves());
      if (biomeConfig.hasReturnGateway()) {
         builder.feature(Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN);
      }

      EndBiome biome = builder.build(biomeConfig.getSupplier()).biome();
      biome.setHasCaves(biomeConfig.hasCaves());
      biome.setSurfaceMaterial(biomeConfig.surfaceMaterial());
      EndTags.addBiomeSurfaceToEndGroup(biome);
      return biome;
   }

   protected void setSurfaceMaterial(SurfaceMaterialProvider prov) {
      this.surfMatProv = prov;
   }

   @Override
   public BlockState getTopMaterial() {
      return this.surfMatProv.getTopMaterial();
   }

   @Override
   public BlockState getUnderMaterial() {
      return this.surfMatProv.getUnderMaterial();
   }

   @Override
   public BlockState getAltTopMaterial() {
      return this.surfMatProv.getAltTopMaterial();
   }

   @Override
   public boolean generateFloorRule() {
      return this.surfMatProv.generateFloorRule();
   }

   @Override
   public SurfaceRuleBuilder surface() {
      return this.surfMatProv.surface();
   }

   public static BlockState findTopMaterial(BCLBiome biome) {
      return BiomeAPI.findTopMaterial(biome).orElse(EndBiome.Config.DEFAULT_MATERIAL.getTopMaterial());
   }

   public static BlockState findTopMaterial(Biome biome) {
      return findTopMaterial(BiomeAPI.getBiome(biome));
   }

   public static BlockState findTopMaterial(WorldGenLevel world, BlockPos pos) {
      return findTopMaterial(BiomeAPI.getBiome(GenerationArea.getBiome(world, pos)));
   }

   public static BlockState findUnderMaterial(BCLBiome biome) {
      return BiomeAPI.findUnderMaterial(biome).orElse(EndBiome.Config.DEFAULT_MATERIAL.getUnderMaterial());
   }

   public static BlockState findUnderMaterial(WorldGenLevel world, BlockPos pos) {
      return findUnderMaterial(BiomeAPI.getBiome(GenerationArea.getBiome(world, pos)));
   }

   public static List<BCLBiome> getAllBeBiomes() {
      return BiomeAPI.getAllBiomes(BiomeType.END);
   }

   public abstract static class Config {
      public static final SurfaceMaterialProvider DEFAULT_MATERIAL = new EndBiome.DefaultSurfaceMaterialProvider();
      protected static final RuleSource END_STONE = SurfaceRules.state(EndBiome.DefaultSurfaceMaterialProvider.END_STONE);
      protected static final RuleSource END_MOSS = SurfaceRules.state(EndBlocks.END_MOSS.defaultBlockState());
      protected static final RuleSource ENDSTONE_DUST = SurfaceRules.state(EndBlocks.ENDSTONE_DUST.defaultBlockState());
      protected static final RuleSource END_MYCELIUM = SurfaceRules.state(EndBlocks.END_MYCELIUM.defaultBlockState());
      protected static final RuleSource FLAVOLITE = SurfaceRules.state(EndBlocks.FLAVOLITE.stone.defaultBlockState());
      protected static final RuleSource SULPHURIC_ROCK = SurfaceRules.state(EndBlocks.SULPHURIC_ROCK.stone.defaultBlockState());
      protected static final RuleSource BRIMSTONE = SurfaceRules.state(EndBlocks.BRIMSTONE.defaultBlockState());
      protected static final RuleSource PALLIDIUM_FULL = SurfaceRules.state(EndBlocks.PALLIDIUM_FULL.defaultBlockState());
      protected static final RuleSource PALLIDIUM_HEAVY = SurfaceRules.state(EndBlocks.PALLIDIUM_HEAVY.defaultBlockState());
      protected static final RuleSource PALLIDIUM_THIN = SurfaceRules.state(EndBlocks.PALLIDIUM_THIN.defaultBlockState());
      protected static final RuleSource PALLIDIUM_TINY = SurfaceRules.state(EndBlocks.PALLIDIUM_TINY.defaultBlockState());
      protected static final RuleSource UMBRALITH = SurfaceRules.state(EndBlocks.UMBRALITH.stone.defaultBlockState());
      public final ResourceLocation ID;

      protected Config(String name) {
         this.ID = BetterEnd.makeID(name);
      }

      protected Config(ResourceLocation ID) {
         this.ID = ID;
      }

      protected abstract void addCustomBuildData(BCLBiomeBuilder var1);

      public BiomeSupplier<EndBiome> getSupplier() {
         return EndBiome::new;
      }

      protected boolean hasCaves() {
         return true;
      }

      protected boolean hasReturnGateway() {
         return true;
      }

      protected SurfaceMaterialProvider surfaceMaterial() {
         return DEFAULT_MATERIAL;
      }
   }

   public static class DefaultSurfaceMaterialProvider implements SurfaceMaterialProvider {
      public static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();

      @Override
      public BlockState getTopMaterial() {
         return this.getUnderMaterial();
      }

      @Override
      public BlockState getAltTopMaterial() {
         return this.getTopMaterial();
      }

      @Override
      public BlockState getUnderMaterial() {
         return END_STONE;
      }

      @Override
      public boolean generateFloorRule() {
         return true;
      }

      @Override
      public SurfaceRuleBuilder surface() {
         SurfaceRuleBuilder builder = SurfaceRuleBuilder.start();
         if (this.generateFloorRule() && this.getTopMaterial() != this.getUnderMaterial()) {
            if (this.getTopMaterial() != this.getAltTopMaterial()) {
               builder.floor(this.getTopMaterial());
            } else {
               builder.chancedFloor(this.getTopMaterial(), this.getAltTopMaterial());
            }
         }

         return builder.filler(this.getUnderMaterial());
      }
   }
}
