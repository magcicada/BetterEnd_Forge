package org.betterx.betterend.world.features.terrain;

import com.mojang.math.Axis;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFCoordModify;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.operator.SDFInvert;
import org.betterx.bclib.sdf.operator.SDFRotation;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.operator.SDFSmoothUnion;
import org.betterx.bclib.sdf.operator.SDFSubtraction;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.operator.SDFUnion;
import org.betterx.bclib.sdf.primitive.SDFCappedCone;
import org.betterx.bclib.sdf.primitive.SDFFlatland;
import org.betterx.bclib.sdf.primitive.SDFPrimitive;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.blocks.HydrothermalVentBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.util.BlockFixer;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class GeyserFeature extends DefaultFeature {
   protected static final Function<BlockState, Boolean> REPLACE1 = state -> state.isAir() || state.is(CommonBlockTags.GEN_END_STONES);
   protected static final Function<BlockState, Boolean> REPLACE2 = state -> !state.is(CommonBlockTags.GEN_END_STONES)
         && !state.is(EndBlocks.HYDROTHERMAL_VENT)
         && !state.is(EndBlocks.SULPHUR_CRYSTAL)
      ? BlocksHelper.replaceableOrPlant(state)
      : true;
   private static final Function<BlockState, Boolean> IGNORE = state -> state.is(Blocks.WATER)
      || state.is(Blocks.CAVE_AIR)
      || state.is(EndBlocks.SULPHURIC_ROCK.stone)
      || state.is(EndBlocks.BRIMSTONE);
   private static final Direction[] HORIZONTAL = BlocksHelper.makeHorizontal();

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      WorldGenLevel world = featureConfig.level();
      BlockPos pos = getPosOnSurfaceWG(world, featureConfig.origin());
      ChunkGenerator chunkGenerator = featureConfig.chunkGenerator();
      if (pos.getY() < 10) {
         return false;
      } else {
         MutableBlockPos bpos = new MutableBlockPos().set(pos);
         bpos.setY(bpos.getY() - 1);

         for (BlockState state = world.getBlockState(bpos);
            state.is(CommonBlockTags.GEN_END_STONES) || !state.getFluidState().isEmpty() && bpos.getY() > 5;
            state = world.getBlockState(bpos)
         ) {
            bpos.setY(bpos.getY() - 1);
         }

         if (pos.getY() - bpos.getY() < 25) {
            return false;
         } else {
            int halfHeight = MHelper.randRange(10, 20, random);
            float radius1 = halfHeight * 0.5F;
            float radius2 = halfHeight * 0.1F + 0.5F;
            SDF sdf = new SDFCappedCone().setHeight(halfHeight).setRadius1(radius1).setRadius2(radius2).setBlock(EndBlocks.SULPHURIC_ROCK.stone);
            sdf = new SDFTranslate().setTranslate(0.0F, halfHeight - 3, 0.0F).setSource(sdf);
            int count = halfHeight;

            for (int i = 0; i < count; i++) {
               int py = i << 1;
               float delta = (float)i / (count - 1);
               float radius = Mth.lerp(delta, radius1, radius2) * 1.3F;
               SDF bowl = new SDFCappedCone().setHeight(radius).setRadius1(0.0F).setRadius2(radius).setBlock(EndBlocks.SULPHURIC_ROCK.stone);
               SDF brimstone = new SDFCappedCone().setHeight(radius).setRadius1(0.0F).setRadius2(radius).setBlock(EndBlocks.BRIMSTONE);
               brimstone = new SDFTranslate().setTranslate(0.0F, 2.0F, 0.0F).setSource(brimstone);
               bowl = new SDFSubtraction().setSourceA(bowl).setSourceB(brimstone);
               bowl = new SDFUnion().setSourceA(brimstone).setSourceB(bowl);
               SDF water = new SDFCappedCone().setHeight(radius).setRadius1(0.0F).setRadius2(radius).setBlock(Blocks.WATER);
               water = new SDFTranslate().setTranslate(0.0F, 4.0F, 0.0F).setSource(water);
               bowl = new SDFSubtraction().setSourceA(bowl).setSourceB(water);
               bowl = new SDFUnion().setSourceA(water).setSourceB(bowl);
               OpenSimplexNoise noise1 = new OpenSimplexNoise(random.nextLong());
               OpenSimplexNoise noise2 = new OpenSimplexNoise(random.nextLong());
               bowl = new SDFCoordModify().setFunction(vec -> {
                  float dx = (float)noise1.eval(vec.x() * 0.1, vec.y() * 0.1, vec.z() * 0.1);
                  float dz = (float)noise2.eval(vec.x() * 0.1, vec.y() * 0.1, vec.z() * 0.1);
                  vec.set(vec.x() + dx, vec.y(), vec.z() + dz);
               }).setSource(bowl);
               SDF cut = new SDFFlatland().setBlock(Blocks.AIR);
               cut = new SDFInvert().setSource(cut);
               cut = new SDFTranslate().setTranslate(0.0F, radius - 2.0F, 0.0F).setSource(cut);
               bowl = new SDFSubtraction().setSourceA(bowl).setSourceB(cut);
               bowl = new SDFTranslate().setTranslate(radius, py - radius, 0.0F).setSource(bowl);
               bowl = new SDFRotation().setRotation(Axis.YP, i * 4.0F).setSource(bowl);
               sdf = new SDFUnion().setSourceA(sdf).setSourceB(bowl);
            }

            sdf.setReplaceFunction(REPLACE2).fillRecursive(world, pos);
            radius2 *= 0.5F;
            if (radius2 < 0.7F) {
               radius2 = 0.7F;
            }

            OpenSimplexNoise noise = new OpenSimplexNoise(random.nextLong());
            SDFPrimitive obj1 = new SDFCappedCone().setHeight(halfHeight + 5).setRadius1(radius1 * 0.5F).setRadius2(radius2);
            sdf = new SDFTranslate().setTranslate(0.0F, halfHeight - 13, 0.0F).setSource(obj1);
            sdf = new SDFDisplacement().setFunction(vec -> (float)noise.eval(vec.x() * 0.3F, vec.y() * 0.3F, vec.z() * 0.3F) * 0.5F).setSource(sdf);
            SDFPrimitive obj2 = new SDFSphere().setRadius(radius1);
            SDF cave = new SDFScale3D().setScale(1.5F, 1.0F, 1.5F).setSource(obj2);
            cave = new SDFDisplacement().setFunction(vec -> (float)noise.eval(vec.x() * 0.1F, vec.y() * 0.1F, vec.z() * 0.1F) * 2.0F).setSource(cave);
            cave = new SDFTranslate().setTranslate(0.0F, -halfHeight - 10, 0.0F).setSource(cave);
            sdf = new SDFSmoothUnion().setRadius(5.0F).setSourceA(cave).setSourceB(sdf);
            obj1.setBlock(WATER);
            obj2.setBlock(WATER);
            sdf.setReplaceFunction(REPLACE2);
            sdf.fillRecursive(world, pos);
            obj1.setBlock(EndBlocks.BRIMSTONE);
            obj2.setBlock(EndBlocks.BRIMSTONE);
            new SDFDisplacement().setFunction(vec -> -2.0F).setSource(sdf).setReplaceFunction(REPLACE1).fillRecursiveIgnore(world, pos, IGNORE);
            obj1.setBlock(EndBlocks.SULPHURIC_ROCK.stone);
            obj2.setBlock(EndBlocks.SULPHURIC_ROCK.stone);
            new SDFDisplacement().setFunction(vec -> -4.0F).setSource(cave).setReplaceFunction(REPLACE1).fillRecursiveIgnore(world, pos, IGNORE);
            obj1.setBlock(Blocks.END_STONE);
            obj2.setBlock(Blocks.END_STONE);
            new SDFDisplacement().setFunction(vec -> -6.0F).setSource(cave).setReplaceFunction(REPLACE1).fillRecursiveIgnore(world, pos, IGNORE);
            BlocksHelper.setWithoutUpdate(world, pos, WATER);
            MutableBlockPos mut = new MutableBlockPos().set(pos);
            count = getYOnSurface(world, pos.getX(), pos.getZ()) - pos.getY();

            for (int i = 0; i < count; i++) {
               BlocksHelper.setWithoutUpdate(world, mut, WATER);

               for (Direction dir : BlocksHelper.HORIZONTAL) {
                  BlocksHelper.setWithoutUpdate(world, mut.relative(dir), WATER);
               }

               mut.setY(mut.getY() + 1);
            }

            for (int i = 0; i < 150; i++) {
               mut.set(pos).move(MHelper.floor(random.nextGaussian() * 4.0 + 0.5), -halfHeight - 10, MHelper.floor(random.nextGaussian() * 4.0 + 0.5));
               float distRaw = MHelper.length((float)(mut.getX() - pos.getX()), (float)(mut.getZ() - pos.getZ()));
               int dist = MHelper.floor(6.0F - distRaw) + random.nextInt(2);
               if (dist >= 0 && GenerationArea.contains(world, mut, 1)) {
                  BlockState var28;
                  for (var28 = world.getBlockState(mut); !var28.getFluidState().isEmpty() || var28.is(CommonBlockTags.WATER_PLANT); var28 = world.getBlockState(mut)) {
                     mut.setY(mut.getY() - 1);
                  }

                  if (var28.is(CommonBlockTags.GEN_END_STONES) && !world.getBlockState(mut.above()).is(EndBlocks.HYDROTHERMAL_VENT)) {
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

                     var28 = (BlockState)EndBlocks.HYDROTHERMAL_VENT.defaultBlockState().setValue(HydrothermalVentBlock.ACTIVATED, distRaw < 2.0F);
                     BlocksHelper.setWithoutUpdate(world, mut, var28);
                     mut.setY(mut.getY() + 1);

                     for (BlockState var30 = world.getBlockState(mut); var30.is(Blocks.WATER); var30 = world.getBlockState(mut)) {
                        BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.VENT_BUBBLE_COLUMN.defaultBlockState());
                        mut.setY(mut.getY() + 1);
                     }
                  }
               }
            }

            for (int ix = 0; ix < 10; ix++) {
               mut.set(pos).move(MHelper.floor(random.nextGaussian() * 0.7 + 0.5), -halfHeight - 10, MHelper.floor(random.nextGaussian() * 0.7 + 0.5));
               float distRaw = MHelper.length((float)(mut.getX() - pos.getX()), (float)(mut.getZ() - pos.getZ()));
               int dist = MHelper.floor(6.0F - distRaw) + random.nextInt(2);
               if (dist >= 0 && GenerationArea.contains(world, mut, 1)) {
                  BlockState var31;
                  for (var31 = world.getBlockState(mut); var31.is(Blocks.WATER); var31 = world.getBlockState(mut)) {
                     mut.setY(mut.getY() - 1);
                  }

                  if (var31.is(CommonBlockTags.GEN_END_STONES)) {
                     for (int j = 0; j <= dist; j++) {
                        BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.SULPHURIC_ROCK.stone);
                        mut.setY(mut.getY() + 1);
                     }

                     var31 = (BlockState)EndBlocks.HYDROTHERMAL_VENT.defaultBlockState().setValue(HydrothermalVentBlock.ACTIVATED, distRaw < 2.0F);
                     BlocksHelper.setWithoutUpdate(world, mut, var31);
                     mut.setY(mut.getY() + 1);

                     for (BlockState var33 = world.getBlockState(mut); var33.is(Blocks.WATER); var33 = world.getBlockState(mut)) {
                        BlocksHelper.setWithoutUpdate(world, mut, EndBlocks.VENT_BUBBLE_COLUMN.defaultBlockState());
                        mut.setY(mut.getY() + 1);
                     }
                  }
               }
            }

            ((SulphuricLakeFeature)EndFeatures.SULPHURIC_LAKE.getFeature())
               .place(new FeaturePlaceContext(Optional.empty(), world, chunkGenerator, random, pos, null));
            double distance = radius1 * 1.7;
            BlockPos start = pos.offset((int)(-distance), (int)(-halfHeight - 15 - distance), (int)(-distance));
            BlockPos end = pos.offset((int)distance, (int)(-halfHeight - 5 + distance), (int)distance);
            BlockFixer.fixBlocks(world, start, end);
            return true;
         }
      }
   }
}
