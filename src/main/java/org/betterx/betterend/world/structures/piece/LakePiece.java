package org.betterx.betterend.world.structures.piece;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.FluidState;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndStructures;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class LakePiece extends BasePiece {
   private static final BlockState ENDSTONE = Blocks.END_STONE.defaultBlockState();
   private static final BlockState WATER = Blocks.WATER.defaultBlockState();
   private static final int SHORE_WIDTH = 16;
   private static final double OUTER_WATER_WEIGHT_MIN = 0.1;
   private static final double OUTER_WATER_WEIGHT_MAX = 0.3;
   private static final int GROUND_SEARCH_DEPTH = 32;
   private static final int BRIDGE_DEPTH = 8;
   private static final int SHELL_DEPTH = 4;
   private static BlockState[] rimPlants;
   private final Map<Integer, Byte> heightmap = new ConcurrentHashMap<>();
   private OpenSimplexNoise noise;
   private BlockPos center;
   private float radius;
   private float aspect;
   private float depth;
   private int seed;
   private int waterLevel;
   private ResourceKey<Biome> biomeID;
   private BlockState lastSurfaceMaterial;
   private static final int NO_GROUND = Integer.MIN_VALUE;
   private static final int FLUID_BELOW = -2147483647;
   private static final int MAX_AIR_FILL = 4;

   private static BlockState[] rimPlants() {
      if (rimPlants == null) {
         rimPlants = new BlockState[]{EndBlocks.JUNGLE_GRASS.defaultBlockState(), EndBlocks.UMBRELLA_MOSS.defaultBlockState()};
      }

      return rimPlants;
   }

   public LakePiece(BlockPos center, float radius, float depth, RandomSource random, Holder<Biome> biome, int waterLevel) {
      super(EndStructures.LAKE_PIECE, random.nextInt(), null);
      this.center = center;
      this.radius = radius;
      this.depth = depth;
      this.seed = random.nextInt();
      this.noise = new OpenSimplexNoise(this.seed);
      this.aspect = radius / depth;
      this.biomeID = (ResourceKey<Biome>)biome.unwrapKey().orElse(null);
      this.waterLevel = waterLevel;
      this.makeBoundingBox();
   }

   public LakePiece(StructurePieceSerializationContext type, CompoundTag tag) {
      super(EndStructures.LAKE_PIECE, tag);
      this.makeBoundingBox();
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.put("center", NbtUtils.writeBlockPos(this.center));
      tag.putFloat("radius", this.radius);
      tag.putFloat("depth", this.depth);
      tag.putInt("seed", this.seed);
      tag.putInt("water_level", this.waterLevel);
      tag.putString("biome", this.biomeID.location().toString());
   }

   @Override
   protected void fromNbt(CompoundTag tag) {
      this.center = NbtUtils.readBlockPos(tag.getCompound("center"));
      this.radius = tag.contains("radius", 5) ? tag.getFloat("radius") : 0.0F;
      this.depth = tag.contains("depth", 5) ? tag.getFloat("depth") : 0.0F;
      this.seed = tag.contains("seed", 3) ? tag.getInt("seed") : 0;
      this.waterLevel = tag.contains("water_level", 3) ? tag.getInt("water_level") : this.center.getY();
      this.noise = new OpenSimplexNoise(this.seed);
      this.aspect = this.radius / this.depth;
      this.biomeID = ResourceKey.create(Registries.BIOME, new ResourceLocation(tag.getString("biome")));
   }

   private BlockState surfaceMaterial(WorldGenLevel world) {
      if (this.lastSurfaceMaterial != null) {
         return this.lastSurfaceMaterial;
      } else {
         if (this.biomeID != null) {
            Holder<Biome> biome = (Holder<Biome>)world.registryAccess().lookupOrThrow(Registries.BIOME).get(this.biomeID).orElse(null);
            if (biome != null) {
               return EndBiome.findTopMaterial((Biome)biome.value());
            }
         }

         return ENDSTONE;
      }
   }

   private BlockState shorePatch(WorldGenLevel world, int worldX, int worldZ, double endstoneBias) {
      return this.noise.eval(worldX * 0.1, worldZ * 0.1, 200.0) < endstoneBias ? ENDSTONE : this.surfaceMaterial(world);
   }

   public void postProcess(
      WorldGenLevel world, StructureManager arg, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox blockBox, ChunkPos chunkPos, BlockPos blockPos
   ) {
      int minY = this.carveMinY();
      int maxY = this.boundingBox.maxY();
      int sx = SectionPos.sectionToBlockCoord(chunkPos.x);
      int sz = SectionPos.sectionToBlockCoord(chunkPos.z);
      MutableBlockPos mut = new MutableBlockPos();
      ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z);

      for (int x = 0; x < 16; x++) {
         mut.setX(x);
         int wx = x | sx;
         double nx = wx * 0.1;
         int x2 = wx - this.center.getX();

         for (int z = 0; z < 16; z++) {
            mut.setZ(z);
            int wz = z | sz;
            double nz = wz * 0.1;
            int z2 = wz - this.center.getZ();
            float clamp = this.getHeightClamp(world, 8, wx, wz);
            if (!(clamp < 0.01)) {
               double n = this.noise.eval(nx, nz) * 1.5 + 1.5;
               double x3 = MHelper.sqr(x2 + this.noise.eval(nx, nz, 100.0) * 10.0);
               double z3 = MHelper.sqr(z2 + this.noise.eval(nx, nz, -100.0) * 10.0);

               for (int y = maxY; y >= minY; y--) {
                  mut.setY((int)(y + n));
                  double y2 = MHelper.sqr((y - this.center.getY()) * this.aspect);
                  double r2 = this.radius * clamp;
                  double r3 = r2 + 8.0;
                  r2 *= r2;
                  r3 = r3 * r3 + 100.0;
                  double dist = x3 + y2 + z3;
                  if (dist < r2) {
                     BlockState state = chunk.getBlockState(mut);
                     if (state.is(CommonBlockTags.TERRAIN) && !state.is(Blocks.END_STONE) && !state.is(EndBlocks.ENDSTONE_DUST)) {
                        this.lastSurfaceMaterial = state;
                     }

                     if (state.is(CommonBlockTags.END_STONES) || state.isAir()) {
                        state = mut.getY() < this.waterLevel ? WATER : CAVE_AIR;
                        chunk.setBlockState(mut, state, false);
                     }
                  } else if (dist <= r3 && mut.getY() < this.center.getY()) {
                     BlockState statex = chunk.getBlockState(mut);
                     BlockPos worldPos = mut.offset(sx, 0, sz);
                     if (!statex.isCollisionShapeFullBlock(world, worldPos) && !statex.isRedstoneConductor(world, worldPos)) {
                        BlockState above3 = chunk.getBlockState(mut.above(3));
                        BlockState stateAbove = chunk.getBlockState(mut.above());
                        if (stateAbove.isAir() && above3.isAir()) {
                           statex = this.shorePatch(world, worldPos.getX(), worldPos.getZ(), -0.55);
                        } else if (stateAbove.isAir()) {
                           statex = this.shorePatch(world, worldPos.getX(), worldPos.getZ(), 0.0);
                        } else {
                           statex = above3.getFluidState().isEmpty() ? ENDSTONE : EndBlocks.ENDSTONE_DUST.defaultBlockState();
                        }

                        double edgeT = (dist - r2) / (r3 - r2);
                        double placeChance = edgeT > 0.85 ? 0.2 : (edgeT > 0.6 ? 0.5 : 1.0);
                        if (placeChance >= 1.0 || random.nextDouble() < placeChance) {
                           chunk.setBlockState(mut, statex, false);
                           this.fillDownToGround(chunk, mut, mut.getY() - 1);
                           if (stateAbove.isAir() && random.nextDouble() < 1.0 - 0.75 * edgeT) {
                              this.placeRimPlant(world, chunk, mut, worldPos, random);
                           }
                        }
                     }
                  }
               }

               double rc = this.radius * clamp;
               double ys = (this.waterLevel - 1 - n - this.center.getY()) * this.aspect;
               double edgeSq = rc * rc - ys * ys;
               if (edgeSq > 0.0) {
                  double dOut = Math.sqrt(x3 + z3) - Math.sqrt(edgeSq);
                  if (dOut <= 17.0) {
                     this.groundColumn(world, chunk, mut, wx, wz, dOut);
                  }
               }
            }
         }
      }

      this.fixWater(world, chunk, mut, random, sx, sz);
   }

   private void groundColumn(WorldGenLevel world, ChunkAccess chunk, MutableBlockPos mut, int wx, int wz, double dOut) {
      int surfaceY = this.waterLevel - 1;
      mut.setY(surfaceY);
      if (chunk.getBlockState(mut).getFluidState().isEmpty()) {
         int groundTop = this.findGroundBelow(chunk, mut, surfaceY);
         if (groundTop != -2147483647) {
            if (groundTop < surfaceY) {
               if (groundTop != Integer.MIN_VALUE) {
                  double t = Math.min(1.0, dOut / 16.0);
                  double minW = 1.0 - t * 0.9;
                  double maxW = 1.0 - t * 0.7;
                  double noise01 = this.noise.eval(wx * 0.1, wz * 0.1, 300.0) * 0.5 + 0.5;
                  double waterWeight = minW + noise01 * (maxW - minW);
                  int target = (int)Math.round(waterWeight * surfaceY + (1.0 - waterWeight) * groundTop);
                  if (target > groundTop) {
                     mut.setY(target);
                     BlockState state = chunk.getBlockState(mut);
                     if (state.getFluidState().isEmpty()) {
                        if (state.isAir() || BlocksHelper.replaceableOrPlant(state)) {
                           chunk.setBlockState(mut, this.shorePatch(world, wx, wz, -0.55), false);
                           this.fillDownToGround(chunk, mut, target - 1);
                        }
                     }
                  }
               }
            }
         }
      } else {
         int bottom;
         for (bottom = surfaceY; bottom - 1 > chunk.getMinBuildHeight(); bottom--) {
            mut.setY(bottom - 1);
            if (chunk.getBlockState(mut).getFluidState().isEmpty()) {
               break;
            }
         }

         this.fillBelow(chunk, mut, bottom - 1);
      }
   }

   private void fillDownToGround(ChunkAccess chunk, MutableBlockPos mut, int from) {
      int origY = mut.getY();
      int groundY = Integer.MIN_VALUE;
      int floor = Math.max(from - 4, chunk.getMinBuildHeight());

      for (int y = from; y >= floor; y--) {
         mut.setY(y);
         BlockState state = chunk.getBlockState(mut);
         if (!state.getFluidState().isEmpty()) {
            mut.setY(origY);
            return;
         }

         if (!state.isAir() && !BlocksHelper.replaceableOrPlant(state)) {
            groundY = y;
            break;
         }
      }

      if (groundY == Integer.MIN_VALUE) {
         mut.setY(origY);
      } else {
         for (int y = from; y >= groundY; y--) {
            mut.setY(y);
            chunk.setBlockState(mut, ENDSTONE, false);
         }

         mut.setY(origY);
      }
   }

   private int findGroundBelow(ChunkAccess chunk, MutableBlockPos mut, int from) {
      int floor = Math.max(from - 32, chunk.getMinBuildHeight());

      for (int y = from; y > floor; y--) {
         mut.setY(y);
         BlockState state = chunk.getBlockState(mut);
         if (!state.getFluidState().isEmpty()) {
            return -2147483647;
         }

         if (!state.isAir() && !BlocksHelper.replaceableOrPlant(state)) {
            return y;
         }
      }

      return Integer.MIN_VALUE;
   }

   private void fillBelow(ChunkAccess chunk, MutableBlockPos mut, int from) {
      int groundTop = this.findGroundBelow(chunk, mut, from);
      int stopAt;
      if (groundTop != Integer.MIN_VALUE && groundTop != -2147483647 && from - groundTop <= 8) {
         stopAt = groundTop;
      } else {
         stopAt = from - 4;
      }

      for (int y = from; y > stopAt; y--) {
         mut.setY(y);
         BlockState state = chunk.getBlockState(mut);
         if (!state.getFluidState().isEmpty() || !state.isAir() && !BlocksHelper.replaceableOrPlant(state)) {
            break;
         }

         chunk.setBlockState(mut, ENDSTONE, false);
      }
   }

   private void placeRimPlant(WorldGenLevel world, ChunkAccess chunk, MutableBlockPos surface, BlockPos surfaceWorld, RandomSource random) {
      BlockState[] plants = rimPlants();
      BlockPos plantLocal = surface.above();
      BlockPos plantWorld = surfaceWorld.above();
      int start = random.nextInt(plants.length);

      for (int i = 0; i < plants.length; i++) {
         BlockState plant = plants[(start + i) % plants.length];
         if (plant.canSurvive(world, plantWorld)) {
            chunk.setBlockState(plantLocal, plant, false);
            return;
         }
      }
   }

   private void fixWater(WorldGenLevel world, ChunkAccess chunk, MutableBlockPos mut, RandomSource random, int sx, int sz) {
      int minY = this.carveMinY();
      int maxY = this.boundingBox.maxY();

      for (int x = 0; x < 16; x++) {
         mut.setX(x);

         for (int z = 0; z < 16; z++) {
            mut.setZ(z);

            for (int y = minY; y <= maxY; y++) {
               mut.setY(y);
               FluidState state = chunk.getFluidState(mut);
               if (!state.isEmpty()) {
                  mut.setY(y - 1);
                  if (chunk.getBlockState(mut).isAir()) {
                     mut.setY(y + 1);
                     BlockState bState = chunk.getBlockState(mut);
                     if (bState.isAir()) {
                        bState = this.shorePatch(world, x | sx, z | sz, 0.0);
                     } else {
                        bState = bState.getFluidState().isEmpty() ? ENDSTONE : EndBlocks.ENDSTONE_DUST.defaultBlockState();
                     }

                     mut.setY(y);
                     this.makeEndstonePillar(chunk, mut, bState);
                  } else if (x > 1 && x < 15 && z > 1 && z < 15) {
                     mut.setY(y);

                     for (Direction dir : BlocksHelper.HORIZONTAL) {
                        BlockPos wPos = mut.offset(dir.getStepX(), 0, dir.getStepZ());
                        if (chunk.getBlockState(wPos).isAir()) {
                           mut.setY(y + 1);
                           BlockState bState = chunk.getBlockState(mut);
                           if (bState.isAir()) {
                              bState = this.shorePatch(world, x | sx, z | sz, 0.0);
                           } else {
                              bState = bState.getFluidState().isEmpty() ? ENDSTONE : EndBlocks.ENDSTONE_DUST.defaultBlockState();
                           }

                           mut.setY(y);
                           this.makeEndstonePillar(chunk, mut, bState);
                           break;
                        }
                     }
                  } else if (chunk.getBlockState(mut.move(Direction.UP)).isAir()) {
                     chunk.markPosForPostprocessing(mut.move(Direction.DOWN).immutable());
                  }
               } else if (chunk.getBlockState(mut).isRandomlyTicking()) {
                  chunk.markPosForPostprocessing(mut.immutable());
               }
            }
         }
      }
   }

   private void makeEndstonePillar(ChunkAccess chunk, MutableBlockPos mut, BlockState terrain) {
      chunk.setBlockState(mut, terrain, false);
      mut.setY(mut.getY() - 1);

      while (!chunk.getFluidState(mut).isEmpty()) {
         chunk.setBlockState(mut, ENDSTONE, false);
         mut.setY(mut.getY() - 1);
      }
   }

   private int getHeight(WorldGenLevel world, BlockPos pos) {
      int p = (pos.getX() & 2047) << 11 | pos.getZ() & 2047;
      int h = this.heightmap.getOrDefault(p, (byte)-128);
      if (h > -128) {
         return h;
      } else if (!GenerationArea.getBiome(world, pos).is(this.biomeID)) {
         this.heightmap.put(p, (byte)0);
         return 0;
      } else {
         h = worldgenSurfaceHeight(world, pos.getX(), pos.getZ());
         h = Mth.abs(h - this.center.getY());
         h = h < 8 ? 1 : 0;
         this.heightmap.put(p, (byte)h);
         return h;
      }
   }

   private float getHeightClamp(WorldGenLevel world, int radius, int posX, int posZ) {
      MutableBlockPos mut = new MutableBlockPos();
      int r2 = radius * radius;
      float height = 0.0F;
      float max = 0.0F;

      for (int x = -radius; x <= radius; x++) {
         mut.setX(posX + x);
         int x2 = x * x;

         for (int z = -radius; z <= radius; z++) {
            mut.setZ(posZ + z);
            int z2 = z * z;
            if (x2 + z2 < r2) {
               float mult = 1.0F - (float)Math.sqrt(x2 + z2) / radius;
               max += mult;
               height += this.getHeight(world, mut) * mult;
            }
         }
      }

      height /= max;
      return Mth.clamp(height, 0.0F, 1.0F);
   }

   private int carveMinY() {
      return MHelper.floor(this.center.getY() - this.depth - 8.0F);
   }

   private void makeBoundingBox() {
      int margin = 34;
      int minX = MHelper.floor(this.center.getX() - this.radius - 34.0F);
      int minY = this.carveMinY() - 8 - 4;
      int minZ = MHelper.floor(this.center.getZ() - this.radius - 34.0F);
      int maxX = MHelper.floor(this.center.getX() + this.radius + 34.0F);
      int maxY = MHelper.floor(this.center.getY() + this.depth);
      int maxZ = MHelper.floor(this.center.getZ() + this.radius + 34.0F);
      this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
