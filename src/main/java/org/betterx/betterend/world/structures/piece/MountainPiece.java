package org.betterx.betterend.world.structures.piece;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;

public abstract class MountainPiece extends BasePiece {
   protected Map<Integer, Integer> heightmap = new ConcurrentHashMap<>();
   protected Map<Integer, Integer> surface = new ConcurrentHashMap<>();
   protected OpenSimplexNoise noise1;
   protected OpenSimplexNoise noise2;
   protected BlockPos center;
   protected float radius;
   protected float height;
   protected float r2;
   protected ResourceLocation biomeID;
   protected int seed1;
   protected int seed2;

   public MountainPiece(StructurePieceType type, BlockPos center, float radius, float height, RandomSource random, Holder<Biome> biome) {
      super(type, random.nextInt(), null);
      this.center = center;
      this.radius = radius;
      this.height = height;
      this.r2 = radius * radius;
      this.seed1 = random.nextInt();
      this.seed2 = random.nextInt();
      this.noise1 = new OpenSimplexNoise(this.seed1);
      this.noise2 = new OpenSimplexNoise(this.seed2);
      this.biomeID = BiomeAPI.getBiomeID(biome);
      this.makeBoundingBox();
   }

   public MountainPiece(StructurePieceType type, CompoundTag tag) {
      super(type, tag);
      this.makeBoundingBox();
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.put("center", NbtUtils.writeBlockPos(this.center));
      tag.putFloat("radius", this.radius);
      tag.putFloat("height", this.height);
      tag.putString("biome", this.biomeID.toString());
      tag.putInt("seed1", this.seed1);
      tag.putInt("seed2", this.seed2);
   }

   @Override
   protected void fromNbt(CompoundTag tag) {
      this.center = NbtUtils.readBlockPos(tag.getCompound("center"));
      this.radius = tag.getFloat("radius");
      this.height = tag.getFloat("height");
      this.biomeID = new ResourceLocation(tag.getString("biome"));
      this.r2 = this.radius * this.radius;
      this.seed1 = tag.getInt("seed1");
      this.seed2 = tag.getInt("seed2");
      this.noise1 = new OpenSimplexNoise(this.seed1);
      this.noise2 = new OpenSimplexNoise(this.seed2);
   }

   protected int getWorldgenSurface(WorldGenLevel world, int x, int z) {
      int p = (x & 2047) << 11 | z & 2047;
      Integer h = this.surface.get(p);
      if (h == null) {
         h = worldgenSurfaceHeight(world, x, z);
         this.surface.put(p, h);
      }

      return h;
   }

   private int getHeight(WorldGenLevel world, BlockPos pos) {
      int p = (pos.getX() & 2047) << 11 | pos.getZ() & 2047;
      int h = this.heightmap.getOrDefault(p, Integer.MIN_VALUE);
      if (h > Integer.MIN_VALUE) {
         return h;
      } else if (!BiomeAPI.getBiomeID(GenerationArea.getBiome(world, pos)).equals(this.biomeID)) {
         this.heightmap.put(p, -10);
         return -10;
      } else {
         h = this.getWorldgenSurface(world, pos.getX(), pos.getZ());
         h = Mth.abs(h - this.center.getY());
         if (h > 4) {
            h = 4 - h;
            this.heightmap.put(p, h);
            return h;
         } else {
            h = MHelper.floor(
               this.noise2.eval(pos.getX() * 0.01, pos.getZ() * 0.01) * this.noise2.eval(pos.getX() * 0.002, pos.getZ() * 0.002) * 8.0
                  + 8.0
            );
            if (h < 0) {
               this.heightmap.put(p, 0);
               return 0;
            } else {
               this.heightmap.put(p, h);
               return h;
            }
         }
      }
   }

   protected float getHeightClamp(WorldGenLevel world, int radius, int posX, int posZ) {
      MutableBlockPos mut = new MutableBlockPos();
      float height = 0.0F;
      float max = 0.0F;

      for (int x = -radius; x <= radius; x++) {
         mut.setX(posX + x);
         int x2 = x * x;

         for (int z = -radius; z <= radius; z++) {
            mut.setZ(posZ + z);
            int z2 = z * z;
            float mult = 1.0F - (float)Math.sqrt(x2 + z2) / radius;
            if (mult > 0.0F) {
               max += mult;
               height += this.getHeight(world, mut) * mult;
            }
         }
      }

      height /= max;
      return Mth.clamp(height / radius, 0.0F, 1.0F);
   }

   private void makeBoundingBox() {
      int minX = MHelper.floor(this.center.getX() - this.radius);
      int minY = MHelper.floor(this.center.getY() - this.radius);
      int minZ = MHelper.floor(this.center.getZ() - this.radius);
      int maxX = MHelper.floor(this.center.getX() + this.radius + 1.0F);
      int maxY = MHelper.floor(this.center.getY() + this.radius + 1.0F);
      int maxZ = MHelper.floor(this.center.getZ() + this.radius + 1.0F);
      this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
