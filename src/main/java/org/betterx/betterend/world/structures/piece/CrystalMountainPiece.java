package org.betterx.betterend.world.structures.piece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndStructures;
import org.betterx.betterend.util.GlobalState;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class CrystalMountainPiece extends MountainPiece {
   private BlockState top = EndBlocks.CRYSTAL_MOSS.defaultBlockState();

   public CrystalMountainPiece(BlockPos center, float radius, float height, RandomSource random, Holder<Biome> biome) {
      super(EndStructures.MOUNTAIN_PIECE, center, radius, height, random, biome);
   }

   public CrystalMountainPiece(StructurePieceSerializationContext type, CompoundTag tag) {
      super(EndStructures.MOUNTAIN_PIECE, tag);
   }

   @Override
   protected void fromNbt(CompoundTag tag) {
      super.fromNbt(tag);
   }

   public void postProcess(
      WorldGenLevel world, StructureManager arg, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox blockBox, ChunkPos chunkPos, BlockPos blockPos
   ) {
      int sx = chunkPos.getMinBlockX();
      int sz = chunkPos.getMinBlockZ();
      MutableBlockPos pos = GlobalState.stateForThread().POS;
      ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z);
      Heightmap map = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE);

      for (int x = 0; x < 16; x++) {
         int px = x + sx;
         int px2 = px - this.center.getX();
         px2 *= px2;
         pos.setX(x);

         for (int z = 0; z < 16; z++) {
            int pz = z + sz;
            int pz2 = pz - this.center.getZ();
            pz2 *= pz2;
            float dist = px2 + pz2;
            if (dist < this.r2) {
               pos.setZ(z);
               dist = 1.0F - (float)Math.pow(dist / this.r2, 0.3);
               int minY = map.getFirstAvailable(x, z);
               if (minY >= 10) {
                  pos.setY(minY);

                  while (
                     !chunk.getBlockState(pos).is(CommonBlockTags.GEN_END_STONES)
                        && pos.getY() > 56
                        && chunk.getBlockState(pos.below()).is(Blocks.CAVE_AIR)
                  ) {
                     pos.setY(pos.getY() - 1);
                  }

                  minY = pos.getY();
                  minY = Math.max(minY, this.getWorldgenSurface(world, px, pz));
                  if (minY > this.center.getY() - 8) {
                     float maxY = dist * this.height * this.getHeightClamp(world, 12, px, pz);
                     if (maxY > 0.0F) {
                        maxY *= (float)this.noise1.eval(px * 0.05, pz * 0.05) * 0.3F + 0.7F;
                        maxY *= (float)this.noise1.eval(px * 0.1, pz * 0.1) * 0.1F + 0.8F;
                        maxY += this.center.getY();
                        int maxYI = (int)maxY;
                        int cover = maxYI - 1;
                        double noise = SplitNoiseCondition.DEFAULT.getNoise(px, pz);
                        boolean needCover = noise > -0.2;

                        for (int y = minY - 1; y < maxYI; y++) {
                           pos.setY(y);
                           if (needCover && y == cover) {
                              chunk.setBlockState(pos, this.top, false);
                           } else {
                              chunk.setBlockState(pos, Blocks.END_STONE.defaultBlockState(), false);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      map = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE);
      int count = (map.getFirstAvailable(8, 8) - (this.center.getY() + 24)) / 7;
      count = Mth.clamp(count, 0, 8);

      for (int i = 0; i < count; i++) {
         int radius = MHelper.randRange(2, 3, random);
         float fill = MHelper.randRange(0.0F, 1.0F, random);
         int x = MHelper.randRange(radius, 15 - radius, random);
         int zx = MHelper.randRange(radius, 15 - radius, random);
         int yx = map.getFirstAvailable(x, zx);
         if (yx > 60) {
            pos.set(x, yx, zx);
            if (chunk.getBlockState(pos.below()).is(Blocks.END_STONE)) {
               int height = MHelper.floor(radius * MHelper.randRange(1.5F, 3.0F, random) + (yx - 80) * 0.3F);
               this.crystal(chunk, pos, radius, height, fill, random);
            }
         }
      }

      count = (map.getFirstAvailable(8, 8) - (this.center.getY() + 24)) / 2;
      count = Mth.clamp(count, 4, 8);

      for (int ix = 0; ix < count; ix++) {
         int radius = MHelper.randRange(1, 2, random);
         float fill = random.nextBoolean() ? 0.0F : 1.0F;
         int x = MHelper.randRange(radius, 15 - radius, random);
         int zx = MHelper.randRange(radius, 15 - radius, random);
         int yx = map.getFirstAvailable(x, zx);
         if (yx > 20) {
            pos.set(x, yx, zx);
            if (chunk.getBlockState(pos.below()).getBlock() == Blocks.END_STONE) {
               int height = MHelper.floor(radius * MHelper.randRange(1.5F, 3.0F, random) + (yx - 80) * 0.3F);
               this.crystal(chunk, pos, radius, height, fill, random);
            }
         }
      }
   }

   private void crystal(ChunkAccess chunk, BlockPos pos, int radius, int height, float fill, RandomSource random) {
      MutableBlockPos mut = new MutableBlockPos();
      int max = MHelper.floor(fill * radius + radius + 0.5F);
      height += pos.getY();
      Heightmap map = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE);
      int coefX = MHelper.randRange(-1, 1, random);
      int coefZ = MHelper.randRange(-1, 1, random);

      for (int x = -radius; x <= radius; x++) {
         mut.setX(x + pos.getX());
         if (mut.getX() >= 0 && mut.getX() < 16) {
            int ax = Math.abs(x);

            for (int z = -radius; z <= radius; z++) {
               mut.setZ(z + pos.getZ());
               if (mut.getZ() >= 0 && mut.getZ() < 16) {
                  int az = Math.abs(z);
                  if (ax + az < max) {
                     int minY = map.getFirstAvailable(mut.getX(), mut.getZ()) - MHelper.randRange(3, 7, random);
                     if (pos.getY() - minY > 8) {
                        minY = pos.getY() - 8;
                     }

                     int h = coefX * x + coefZ * z + height;

                     for (int y = minY; y < h; y++) {
                        mut.setY(y);
                        chunk.setBlockState(mut, EndBlocks.AURORA_CRYSTAL.defaultBlockState(), false);
                     }
                  }
               }
            }
         }
      }
   }
}
