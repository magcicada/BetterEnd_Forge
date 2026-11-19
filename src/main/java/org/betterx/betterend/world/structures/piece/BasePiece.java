package org.betterx.betterend.world.structures.piece;

import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class BasePiece extends StructurePiece {
   protected BasePiece(StructurePieceType type, int i, BoundingBox boundingBox) {
      super(type, i, boundingBox);
   }

   protected BasePiece(StructurePieceType type, CompoundTag tag) {
      super(type, tag);
      this.fromNbt(tag);
   }

   protected abstract void fromNbt(CompoundTag var1);

   protected static int worldgenSurfaceHeight(WorldGenLevel world, int x, int z) {
      ChunkAccess chunk = world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
      if (chunk.hasPrimedHeightmap(Types.WORLD_SURFACE_WG)) {
         return chunk.getHeight(Types.WORLD_SURFACE_WG, x, z) + 1;
      } else {
         ServerChunkCache source = world.getLevel().getChunkSource();
         return source.getGenerator().getBaseHeight(x, z, Types.WORLD_SURFACE_WG, world, source.randomState());
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
      this.addAdditionalSaveData(compoundTag);
   }
}
