package org.betterx.betterend.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.api.v2.levelgen.structures.templatesystem.DestructionStructureProcessor;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public abstract class NBTFeature<FC extends NBTFeatureConfig> extends Feature<FC> {
   protected static final DestructionStructureProcessor DESTRUCTION = new DestructionStructureProcessor();

   public NBTFeature(Codec<FC> codec) {
      super(codec);
   }

   protected abstract StructureTemplate getStructure(FC var1, WorldGenLevel var2, BlockPos var3, RandomSource var4);

   protected abstract boolean canSpawn(WorldGenLevel var1, BlockPos var2, RandomSource var3);

   protected abstract Rotation getRotation(WorldGenLevel var1, BlockPos var2, RandomSource var3);

   protected abstract Mirror getMirror(WorldGenLevel var1, BlockPos var2, RandomSource var3);

   protected abstract int getYOffset(StructureTemplate var1, WorldGenLevel var2, BlockPos var3, RandomSource var4);

   protected abstract NBTFeature.TerrainMerge getTerrainMerge(WorldGenLevel var1, BlockPos var2, RandomSource var3);

   protected abstract void addStructureData(StructurePlaceSettings var1);

   protected BlockPos getGround(WorldGenLevel world, BlockPos center) {
      Holder<Biome> biome = GenerationArea.getBiome(world, center);
      ResourceLocation id = BiomeAPI.getBiomeID(biome);
      if (!id.getNamespace().contains("moutain") && !id.getNamespace().contains("lake")) {
         int y = this.getAverageYWG(world, center);
         return new BlockPos(center.getX(), y, center.getZ());
      } else {
         int y = this.getAverageY(world, center);
         return new BlockPos(center.getX(), y, center.getZ());
      }
   }

   protected int getAverageY(WorldGenLevel world, BlockPos center) {
      int y = DefaultFeature.getYOnSurface(world, center.getX(), center.getZ());
      y += DefaultFeature.getYOnSurface(world, center.getX() - 2, center.getZ() - 2);
      y += DefaultFeature.getYOnSurface(world, center.getX() + 2, center.getZ() - 2);
      y += DefaultFeature.getYOnSurface(world, center.getX() - 2, center.getZ() + 2);
      y += DefaultFeature.getYOnSurface(world, center.getX() + 2, center.getZ() + 2);
      return y / 5;
   }

   protected int getAverageYWG(WorldGenLevel world, BlockPos center) {
      int y = DefaultFeature.getYOnSurfaceWG(world, center.getX(), center.getZ());
      y += DefaultFeature.getYOnSurfaceWG(world, center.getX() - 2, center.getZ() - 2);
      y += DefaultFeature.getYOnSurfaceWG(world, center.getX() + 2, center.getZ() - 2);
      y += DefaultFeature.getYOnSurfaceWG(world, center.getX() - 2, center.getZ() + 2);
      y += DefaultFeature.getYOnSurfaceWG(world, center.getX() + 2, center.getZ() + 2);
      return y / 5;
   }

   public boolean place(FeaturePlaceContext<FC> context) {
      FC cfg = (FC)context.config();
      WorldGenLevel world = context.level();
      RandomSource random = context.random();
      BlockPos center = context.origin();
      center = new BlockPos(center.getX() >> 4 << 4 | 8, 128, center.getZ() >> 4 << 4 | 8);
      center = this.getGround(world, center);
      if (!this.canSpawn(world, center, random)) {
         return false;
      } else {
         int posY = center.getY() + 1;
         StructureTemplate structure = this.getStructure(cfg, world, center, random);
         Rotation rotation = this.getRotation(world, center, random);
         Mirror mirror = this.getMirror(world, center, random);
         BlockPos offset = StructureTemplate.transform(new BlockPos(structure.getSize()), mirror, rotation, BlockPos.ZERO);
         center = center.offset(0, (int)(this.getYOffset(structure, world, center, random) + 0.5), 0);
         BoundingBox bounds = this.makeBox(center);
         StructurePlaceSettings placementData = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).setBoundingBox(bounds);
         this.addStructureData(placementData);
         center = center.offset((int)(-offset.getX() * 0.5), 0, (int)(-offset.getZ() * 0.5));
         structure.placeInWorld(world, center, center, placementData, random, 4);
         NBTFeature.TerrainMerge merge = this.getTerrainMerge(world, center, random);
         int x1 = center.getX();
         int z1 = center.getZ();
         int x2 = x1 + offset.getX();
         int z2 = z1 + offset.getZ();
         if (merge != NBTFeature.TerrainMerge.NONE) {
            MutableBlockPos mut = new MutableBlockPos();
            if (x2 < x1) {
               int a = x1;
               x1 = x2;
               x2 = a;
            }

            if (z2 < z1) {
               int a = z1;
               z1 = z2;
               z2 = a;
            }

            x1 = Math.max(x1, GenerationArea.minX(world));
            x2 = Math.min(x2, GenerationArea.maxX(world));
            z1 = Math.max(z1, GenerationArea.minZ(world));
            z2 = Math.min(z2, GenerationArea.maxZ(world));
            int surfMax = posY - 1;

            for (int x = x1; x <= x2; x++) {
               mut.setX(x);

               for (int z = z1; z <= z2; z++) {
                  mut.setZ(z);
                  mut.setY(surfMax);
                  BlockState state = world.getBlockState(mut);
                  if (!this.isTerrain(state) && state.isFaceSturdy(world, mut, Direction.DOWN)) {
                     for (int i = 0; i < 10; i++) {
                        mut.setY(mut.getY() - 1);
                        BlockState stateSt = world.getBlockState(mut);
                        if (this.isTerrain(stateSt)) {
                           if (this.isTerrain(state) && state.isSolid()) {
                              if (merge == NBTFeature.TerrainMerge.SURFACE) {
                                 Holder<Biome> b = GenerationArea.getBiome(world, mut);
                                 BlockState bottom = BiomeAPI.findUnderMaterial(b).orElse(cfg.defaultBlock);
                                 BlocksHelper.setWithoutUpdate(world, mut, bottom);
                              } else {
                                 BlocksHelper.setWithoutUpdate(world, mut, state);
                              }
                           }
                           break;
                        }

                        if (merge == NBTFeature.TerrainMerge.SURFACE) {
                           boolean isTop = mut.getY() == surfMax && state.isSolid();
                           Holder<Biome> b = GenerationArea.getBiome(world, mut);
                           BlockState top = (isTop ? BiomeAPI.findTopMaterial(b) : BiomeAPI.findUnderMaterial(b)).orElse(cfg.defaultBlock);
                           BlocksHelper.setWithoutUpdate(world, mut, top);
                        } else {
                           BlocksHelper.setWithoutUpdate(world, mut, state);
                        }
                     }
                  }
               }
            }
         }

         return true;
      }
   }

   private boolean isTerrain(BlockState state) {
      return state.is(CommonBlockTags.END_STONES) || state.is(CommonBlockTags.NETHER_STONES);
   }

   protected BoundingBox makeBox(BlockPos pos) {
      int sx = (pos.getX() >> 4 << 4) - 16;
      int sz = (pos.getZ() >> 4 << 4) - 16;
      int ex = sx + 47;
      int ez = sz + 47;
      return BoundingBox.fromCorners(new Vec3i(sx, 0, sz), new Vec3i(ex, 255, ez));
   }

   public static enum TerrainMerge implements StringRepresentable {
      NONE,
      SURFACE,
      OBJECT;

      public static final Codec<NBTFeature.TerrainMerge> CODEC = StringRepresentable.fromEnum(NBTFeature.TerrainMerge::values);

      public static NBTFeature.TerrainMerge getFromString(String type) {
         if (type.equals("surface")) {
            return SURFACE;
         } else {
            return type.equals("object") ? OBJECT : NONE;
         }
      }

      public String getSerializedName() {
         return this.name();
      }
   }
}
