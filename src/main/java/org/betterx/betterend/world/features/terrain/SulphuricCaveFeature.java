package org.betterx.betterend.world.features.terrain;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.blocks.EndBlockProperties;
import org.betterx.betterend.blocks.SulphurCrystalBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.util.BlockFixer;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class SulphuricCaveFeature extends DefaultFeature {
   private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   private static final BlockState WATER = Blocks.WATER.defaultBlockState();
   private static final Direction[] HORIZONTAL = BlocksHelper.makeHorizontal();

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      int radius = MHelper.randRange(10, 30, random);
      int top = world.getHeight(Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
      MutableBlockPos bpos = new MutableBlockPos();
      bpos.setX(pos.getX());
      bpos.setZ(pos.getZ());
      bpos.setY(top - 1);

      BlockState state;
      for (state = world.getBlockState(bpos); !state.is(CommonBlockTags.GEN_END_STONES) && bpos.getY() > 5; state = world.getBlockState(bpos)) {
         bpos.setY(bpos.getY() - 1);
      }

      if (bpos.getY() < 10) {
         return false;
      } else {
         for (top = (int)(bpos.getY() - (radius * 1.3F + 5.0F));
            state.is(CommonBlockTags.GEN_END_STONES) || !state.getFluidState().isEmpty() && bpos.getY() > 5;
            state = world.getBlockState(bpos)
         ) {
            bpos.setY(bpos.getY() - 1);
         }

         int bottom = (int)(bpos.getY() + radius * 1.3F + 5.0F);
         if (top <= bottom) {
            return false;
         } else {
            MutableBlockPos mut = new MutableBlockPos();
            pos = new BlockPos(pos.getX(), MHelper.randRange(bottom, top, random), pos.getZ());
            OpenSimplexNoise noise = new OpenSimplexNoise(MHelper.getSeed(534, pos.getX(), pos.getZ()));
            int x1 = pos.getX() - radius - 5;
            int z1 = pos.getZ() - radius - 5;
            int x2 = pos.getX() + radius + 5;
            int z2 = pos.getZ() + radius + 5;
            int y1 = MHelper.floor(pos.getY() - (radius + 5) / 1.6);
            int y2 = MHelper.floor(pos.getY() + (radius + 5) / 1.6);
            double hr = radius * 0.75;
            double nr = radius * 0.25;
            Set<BlockPos> brimstone = Sets.newHashSet();
            BlockState rock = EndBlocks.SULPHURIC_ROCK.stone.defaultBlockState();
            int waterLevel = pos.getY() + MHelper.randRange(MHelper.floor(radius * 0.8), radius, random);
            int areaX1 = Math.max(x1, GenerationArea.minX(world));
            int areaX2 = Math.min(x2, GenerationArea.maxX(world));
            int areaZ1 = Math.max(z1, GenerationArea.minZ(world));
            int areaZ2 = Math.min(z2, GenerationArea.maxZ(world));

            for (int x = areaX1; x <= areaX2; x++) {
               int xsq = x - pos.getX();
               xsq *= xsq;
               mut.setX(x);

               for (int z = areaZ1; z <= areaZ2; z++) {
                  int zsq = z - pos.getZ();
                  zsq *= zsq;
                  mut.setZ(z);

                  for (int y = y1; y <= y2; y++) {
                     int ysq = y - pos.getY();
                     ysq = (int)(ysq * 1.6);
                     ysq *= ysq;
                     mut.setY(y);
                     double r = noise.eval(x * 0.1, y * 0.1, z * 0.1) * nr + hr;
                     double r2 = r + 5.0;
                     double dist = xsq + ysq + zsq;
                     if (dist < r * r) {
                        state = world.getBlockState(mut);
                        if (this.isReplaceable(state)) {
                           BlocksHelper.setWithoutUpdate(world, mut, y < waterLevel ? WATER : CAVE_AIR);
                        }
                     } else if (dist < r2 * r2) {
                        state = world.getBlockState(mut);
                        if (state.is(CommonBlockTags.GEN_END_STONES) || state.is(Blocks.AIR)) {
                           double v = noise.eval(x * 0.1, y * 0.1, z * 0.1) + noise.eval(x * 0.03, y * 0.03, z * 0.03) * 0.5;
                           if (v > 0.4) {
                              brimstone.add(mut.immutable());
                           } else {
                              BlocksHelper.setWithoutUpdate(world, mut, rock);
                           }
                        }
                     }
                  }
               }
            }

            brimstone.forEach(blockPos -> this.placeBrimstone(world, blockPos, random));
            if (random.nextInt(4) == 0) {
               int count = MHelper.randRange(5, 20, random);

               for (int i = 0; i < count; i++) {
                  mut.set(pos).move(MHelper.floor(random.nextGaussian() * 2.0 + 0.5), 0, MHelper.floor(random.nextGaussian() * 2.0 + 0.5));
                  int dist = MHelper.floor(3.0F - MHelper.length((float)(mut.getX() - pos.getX()), (float)(mut.getZ() - pos.getZ())))
                     + random.nextInt(2);
                  if (dist > 0 && GenerationArea.contains(world, mut, 1)) {
                     for (state = world.getBlockState(mut); !state.getFluidState().isEmpty() || state.is(CommonBlockTags.WATER_PLANT); state = world.getBlockState(mut)) {
                        mut.setY(mut.getY() - 1);
                     }

                     if (state.is(CommonBlockTags.GEN_END_STONES) && !world.getBlockState(mut.above()).is(EndBlocks.HYDROTHERMAL_VENT)) {
                        for (int j = 0; j <= dist; j++) {
                           BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.SULPHURIC_ROCK.stone);
                           Direction[] shuffled = (Direction[])HORIZONTAL.clone();
                           MHelper.shuffle(shuffled, random);

                           for (Direction dir : shuffled) {
                              BlockPos p = mut.relative(dir);
                              if (random.nextBoolean() && world.getBlockState(p).is(Blocks.WATER)) {
                                 BlocksHelper.setWithoutUpdate(
                                    world, p, (BlockState)EndBlocks.TUBE_WORM.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir)
                                 );
                              }
                           }

                           mut.setY(mut.getY() + 1);
                        }

                        BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.HYDROTHERMAL_VENT);
                        mut.setY(mut.getY() + 1);

                        for (BlockState var48 = world.getBlockState(mut); var48.is(Blocks.WATER); var48 = world.getBlockState(mut)) {
                           BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.VENT_BUBBLE_COLUMN.defaultBlockState());
                           world.scheduleTick(mut.immutable(), EndBlocks.VENT_BUBBLE_COLUMN, MHelper.randRange(8, 32, random));
                           mut.setY(mut.getY() + 1);
                        }
                     }
                  }
               }
            }

            BlockFixer.fixBlocks(world, new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
            return true;
         }
      }
   }

   private boolean isReplaceable(BlockState state) {
      return state.is(CommonBlockTags.GEN_END_STONES)
         || state.is(EndBlocks.HYDROTHERMAL_VENT)
         || state.is(EndBlocks.VENT_BUBBLE_COLUMN)
         || state.is(EndBlocks.SULPHUR_CRYSTAL)
         || BlocksHelper.replaceableOrPlant(state)
         || state.is(CommonBlockTags.WATER_PLANT)
         || state.is(BlockTags.LEAVES);
   }

   private void placeBrimstone(WorldGenLevel world, BlockPos pos, RandomSource random) {
      BlockState state = this.getBrimstone(world, pos);
      BlocksHelper.setWithoutUpdate(world, pos, state);
      if ((Boolean)state.getValue(EndBlockProperties.ACTIVE)) {
         this.makeShards(world, pos, random);
      }
   }

   private BlockState getBrimstone(WorldGenLevel world, BlockPos pos) {
      for (Direction dir : BlocksHelper.DIRECTIONS) {
         if (GenerationArea.getBlockState(world, pos.relative(dir)).is(Blocks.WATER)) {
            return (BlockState)EndBlocks.BRIMSTONE.defaultBlockState().setValue(EndBlockProperties.ACTIVE, true);
         }
      }

      return EndBlocks.BRIMSTONE.defaultBlockState();
   }

   private void makeShards(WorldGenLevel world, BlockPos pos, RandomSource random) {
      for (Direction dir : BlocksHelper.DIRECTIONS) {
         BlockPos side;
         if (random.nextInt(16) == 0 && GenerationArea.getBlockState(world, side = pos.relative(dir)).is(Blocks.WATER)) {
            BlockState state = (BlockState)((BlockState)((BlockState)EndBlocks.SULPHUR_CRYSTAL.defaultBlockState().setValue(SulphurCrystalBlock.WATERLOGGED, true))
                  .setValue(SulphurCrystalBlock.FACING, dir))
               .setValue(SulphurCrystalBlock.AGE, random.nextInt(3));
            BlocksHelper.setWithoutUpdate(world, side, state);
         }
      }
   }
}
