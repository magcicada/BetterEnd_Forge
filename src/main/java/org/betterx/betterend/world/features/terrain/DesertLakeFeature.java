package org.betterx.betterend.world.features.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.util.BlockFixer;
import org.betterx.betterend.util.GlobalState;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class DesertLakeFeature extends DefaultFeature {
   private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
   private static final OpenSimplexNoise NOISE = new OpenSimplexNoise(15152L);

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      MutableBlockPos POS = GlobalState.stateForThread().POS;
      RandomSource random = featureConfig.random();
      BlockPos blockPos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      double radius = MHelper.randRange(8.0, 15.0, random);
      double depth = radius * 0.5 * MHelper.randRange(0.8, 1.2, random);
      int dist = MHelper.floor(radius);
      int dist2 = MHelper.floor(radius * 1.5);
      int bott = MHelper.floor(depth);
      blockPos = getPosOnSurfaceWG(world, blockPos);
      if (blockPos.getY() < 10) {
         return false;
      } else {
         int waterLevel = blockPos.getY();
         BlockPos pos = getPosOnSurfaceRaycast(world, blockPos.north(dist).above(10), 20);
         if (Math.abs(blockPos.getY() - pos.getY()) > 5) {
            return false;
         } else {
            waterLevel = MHelper.min(pos.getY(), waterLevel);
            pos = getPosOnSurfaceRaycast(world, blockPos.south(dist).above(10), 20);
            if (Math.abs(blockPos.getY() - pos.getY()) > 5) {
               return false;
            } else {
               waterLevel = MHelper.min(pos.getY(), waterLevel);
               pos = getPosOnSurfaceRaycast(world, blockPos.east(dist).above(10), 20);
               if (Math.abs(blockPos.getY() - pos.getY()) > 5) {
                  return false;
               } else {
                  waterLevel = MHelper.min(pos.getY(), waterLevel);
                  pos = getPosOnSurfaceRaycast(world, blockPos.west(dist).above(10), 20);
                  if (Math.abs(blockPos.getY() - pos.getY()) > 5) {
                     return false;
                  } else {
                     waterLevel = MHelper.min(pos.getY(), waterLevel);
                     int minX = blockPos.getX() - dist2;
                     int maxX = blockPos.getX() + dist2;
                     int minZ = blockPos.getZ() - dist2;
                     int maxZ = blockPos.getZ() + dist2;
                     int maskMinX = minX - 1;
                     int maskMinZ = minZ - 1;
                     int areaMinX = Math.max(minX, GenerationArea.minX(world));
                     int areaMaxX = Math.min(maxX, GenerationArea.maxX(world));
                     int areaMinZ = Math.max(minZ, GenerationArea.minZ(world));
                     int areaMaxZ = Math.min(maxZ, GenerationArea.maxZ(world));
                     boolean[][] mask = new boolean[maxX - minX + 3][maxZ - minZ + 3];

                     for (int x = areaMinX; x <= areaMaxX; x++) {
                        POS.setX(x);
                        int mx = x - maskMinX;

                        for (int z = areaMinZ; z <= areaMaxZ; z++) {
                           POS.setZ(z);
                           int mz = z - maskMinZ;
                           if (!mask[mx][mz]) {
                              for (int y = waterLevel + 1; y <= waterLevel + 20; y++) {
                                 POS.setY(y);
                                 FluidState fluid = world.getFluidState(POS);
                                 if (!fluid.isEmpty()) {
                                    for (int i = -1; i < 2; i++) {
                                       int px = mx + i;

                                       for (int j = -1; j < 2; j++) {
                                          int pz = mz + j;
                                          mask[px][pz] = true;
                                       }
                                    }
                                    break;
                                 }
                              }
                           }
                        }
                     }

                     for (int x = areaMinX; x <= areaMaxX; x++) {
                        POS.setX(x);
                        int x2 = x - blockPos.getX();
                        x2 *= x2;
                        int mx = x - maskMinX;

                        for (int zx = areaMinZ; zx <= areaMaxZ; zx++) {
                           POS.setZ(zx);
                           int z2 = zx - blockPos.getZ();
                           z2 *= z2;
                           int mz = zx - maskMinZ;
                           if (!mask[mx][mz]) {
                              double size = 1.0;

                              for (int yx = blockPos.getY(); yx <= blockPos.getY() + 20; yx++) {
                                 POS.setY(yx);
                                 double add = yx - blockPos.getY();
                                 if (add > 5.0) {
                                    size *= 0.8;
                                    add = 5.0;
                                 }

                                 double r = add * 1.8 + radius * (NOISE.eval(x * 0.2, yx * 0.2, zx * 0.2) * 0.25 + 0.75) - 1.0 / size;
                                 if (!(r > 0.0)) {
                                    break;
                                 }

                                 r *= r;
                                 if (x2 + z2 <= r) {
                                    BlockState state = world.getBlockState(POS);
                                    if (state.is(CommonBlockTags.GEN_END_STONES)) {
                                       BlocksHelper.setWithoutUpdate(world, POS, AIR);
                                    }

                                    pos = POS.below();
                                    if (world.getBlockState(pos).is(CommonBlockTags.GEN_END_STONES)) {
                                       state = EndBiome.findTopMaterial(world, pos);
                                       if (yx > waterLevel + 1) {
                                          BlocksHelper.setWithoutUpdate(world, pos, state);
                                       } else if (yx > waterLevel) {
                                          BlocksHelper.setWithoutUpdate(world, pos, random.nextBoolean() ? state : EndBlocks.ENDSTONE_DUST.defaultBlockState());
                                       } else {
                                          BlocksHelper.setWithoutUpdate(world, pos, EndBlocks.ENDSTONE_DUST.defaultBlockState());
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     double aspect = radius / depth;

                     for (int x = Math.max(blockPos.getX() - dist, areaMinX); x <= Math.min(blockPos.getX() + dist, areaMaxX); x++) {
                        POS.setX(x);
                        int x2 = x - blockPos.getX();
                        x2 *= x2;
                        int mx = x - maskMinX;

                        for (int zxx = Math.max(blockPos.getZ() - dist, areaMinZ); zxx <= Math.min(blockPos.getZ() + dist, areaMaxZ); zxx++) {
                           POS.setZ(zxx);
                           int z2 = zxx - blockPos.getZ();
                           z2 *= z2;
                           int mz = zxx - maskMinZ;
                           if (!mask[mx][mz]) {
                              for (int yx = blockPos.getY() - bott; yx < blockPos.getY(); yx++) {
                                 POS.setY(yx);
                                 double y2 = (yx - blockPos.getY()) * aspect;
                                 y2 *= y2;
                                 double rx = radius * (NOISE.eval(x * 0.2, yx * 0.2, zxx * 0.2) * 0.25 + 0.75);
                                 double rb = rx * 1.2;
                                 rx *= rx;
                                 rb *= rb;
                                 if (y2 + x2 + z2 <= rx) {
                                    BlockState statex = world.getBlockState(POS);
                                    if (this.canReplace(statex)) {
                                       statex = world.getBlockState(POS.above());
                                       statex = this.canReplace(statex) ? (yx < waterLevel ? WATER : AIR) : statex;
                                       BlocksHelper.setWithoutUpdate(world, POS, statex);
                                    }

                                    pos = POS.below();
                                    if (world.getBlockState(pos).is(CommonBlockTags.GEN_END_STONES)) {
                                       BlocksHelper.setWithoutUpdate(world, pos, EndBlocks.ENDSTONE_DUST.defaultBlockState());
                                    }

                                    for (pos = POS.above();
                                       this.canReplace(statex = world.getBlockState(pos)) && !statex.isAir() && statex.getFluidState().isEmpty();
                                       pos = pos.above()
                                    ) {
                                       BlocksHelper.setWithoutUpdate(world, pos, pos.getY() < waterLevel ? WATER : AIR);
                                    }
                                 } else if (y2 + x2 + z2 <= rb) {
                                    BlockState statexx = world.getBlockState(POS);
                                    if (statexx.is(CommonBlockTags.GEN_END_STONES) && world.isEmptyBlock(POS.above())) {
                                       BlocksHelper.setWithoutUpdate(world, POS, EndBlocks.END_MOSS);
                                    } else if (yx < waterLevel) {
                                       if (world.isEmptyBlock(POS.above())) {
                                          statexx = EndBiome.findTopMaterial(world, pos);
                                          BlocksHelper.setWithoutUpdate(world, POS, random.nextBoolean() ? statexx : EndBlocks.ENDSTONE_DUST.defaultBlockState());
                                          BlocksHelper.setWithoutUpdate(world, POS.below(), END_STONE);
                                       } else {
                                          BlocksHelper.setWithoutUpdate(world, POS, EndBlocks.ENDSTONE_DUST.defaultBlockState());
                                          BlocksHelper.setWithoutUpdate(world, POS.below(), END_STONE);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     BlockFixer.fixBlocks(world, new BlockPos(minX - 2, waterLevel - 2, minZ - 2), new BlockPos(maxX + 2, blockPos.getY() + 20, maxZ + 2));
                     return true;
                  }
               }
            }
         }
      }
   }

   private boolean canReplace(BlockState state) {
      return state.is(CommonBlockTags.GEN_END_STONES)
         || state.is(EndBlocks.ENDSTONE_DUST)
         || BlocksHelper.replaceableOrPlant(state)
         || state.is(CommonBlockTags.WATER_PLANT);
   }
}
