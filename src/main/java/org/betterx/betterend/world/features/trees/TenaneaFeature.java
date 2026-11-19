package org.betterx.betterend.world.features.trees;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.blocks.BlockProperties;
import org.betterx.bclib.blocks.BlockProperties.TripleShape;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFDisplacement;
import org.betterx.bclib.sdf.operator.SDFScale;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.operator.SDFSubtraction;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.bclib.util.SplineHelper;
import org.betterx.betterend.blocks.basis.FurBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.joml.Vector3f;

public class TenaneaFeature extends DefaultFeature {
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final Function<BlockState, Boolean> REPLACE = state -> state.getBlock() == EndBlocks.TENANEA_LEAVES
      ? true
      : BlocksHelper.replaceableOrPlant(state);
   private static final Function<BlockState, Boolean> IGNORE = EndBlocks.TENANEA::isTreeLog;
   private static final List<Vector3f> SPLINE = Lists.newArrayList(
      new Vector3f[]{
         new Vector3f(0.0F, 0.0F, 0.0F),
         new Vector3f(0.1F, 0.35F, 0.0F),
         new Vector3f(0.2F, 0.5F, 0.0F),
         new Vector3f(0.3F, 0.55F, 0.0F),
         new Vector3f(0.42F, 0.7F, 0.0F),
         new Vector3f(0.5F, 1.0F, 0.0F)
      }
   );

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      if (!world.getBlockState(pos.below()).is(BlockTags.NYLIUM)) {
         return false;
      } else {
         float size = MHelper.randRange(7, 10, random);
         int count = (int)(size * 0.45F);
         float var = (float) (Math.PI * 2) / (count * 3);
         float start = MHelper.randRange(0.0F, (float) (Math.PI * 2), random);

         for (int i = 0; i < count; i++) {
            float angle = (float)i / count * (float) (Math.PI * 2) + MHelper.randRange(0.0F, var, random) + start;
            List<Vector3f> spline = SplineHelper.copySpline(SPLINE);
            SplineHelper.rotateSpline(spline, angle);
            SplineHelper.scale(spline, size + MHelper.randRange(0.0F, size * 0.5F, random));
            SplineHelper.offsetParts(spline, random, 1.0F, 0.0F, 1.0F);
            SplineHelper.fillSpline(spline, world, EndBlocks.TENANEA.getBark().defaultBlockState(), pos, REPLACE);
            Vector3f last = spline.get(spline.size() - 1);
            float leavesRadius = (size * 0.3F + MHelper.randRange(0.8F, 1.5F, random)) * 1.4F;
            OpenSimplexNoise noise = new OpenSimplexNoise(random.nextLong());
            this.leavesBall(world, pos.offset((int)last.x(), (int)last.y(), (int)last.z()), leavesRadius, random, noise);
         }

         return true;
      }
   }

   private void leavesBall(WorldGenLevel world, BlockPos pos, float radius, RandomSource random, OpenSimplexNoise noise) {
      SDF sphere = new SDFSphere().setRadius(radius).setBlock((BlockState)EndBlocks.TENANEA_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 6));
      SDF sub = new SDFScale().setScale(5.0F).setSource(sphere);
      sub = new SDFTranslate().setTranslate(0.0F, -radius * 5.0F, 0.0F).setSource(sub);
      sphere = new SDFSubtraction().setSourceA(sphere).setSourceB(sub);
      sphere = new SDFScale3D().setScale(1.0F, 0.75F, 1.0F).setSource(sphere);
      sphere = new SDFDisplacement().setFunction(vec -> (float)noise.eval(vec.x() * 0.2, vec.y() * 0.2, vec.z() * 0.2) * 2.0F).setSource(sphere);
      sphere = new SDFDisplacement().setFunction(vec -> MHelper.randRange(-1.5F, 1.5F, random)).setSource(sphere);
      MutableBlockPos mut = new MutableBlockPos();

      for (Direction d1 : BlocksHelper.HORIZONTAL) {
         BlockPos p = mut.set(pos).move(Direction.UP).move(d1).immutable();
         BlocksHelper.setWithoutUpdate(world, p, EndBlocks.TENANEA.getBark().defaultBlockState());

         for (Direction d2 : BlocksHelper.HORIZONTAL) {
            mut.set(p).move(Direction.UP).move(d2);
            BlocksHelper.setWithoutUpdate(world, p, EndBlocks.TENANEA.getBark().defaultBlockState());
         }
      }

      BlockState top = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.TOP);
      BlockState middle = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.MIDDLE);
      BlockState bottom = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.BOTTOM);
      BlockState outer = EndBlocks.TENANEA_OUTER_LEAVES.defaultBlockState();
      List<BlockPos> support = Lists.newArrayList();
      sphere.addPostProcess(info -> {
         if (random.nextInt(6) == 0 && info.getStateDown().isAir()) {
            BlockPos d = info.getPos().below();
            support.add(d);
         }

         if (random.nextInt(5) == 0) {
            for (Direction dir : Direction.values()) {
               BlockState state = info.getState(dir, 2);
               if (state.isAir()) {
                  return info.getState();
               }
            }

            info.setState(EndBlocks.TENANEA.getBark().defaultBlockState());
         }

         Direction[] shuffled = (Direction[])DIRECTIONS.clone();
         MHelper.shuffle(shuffled, random);

         for (Direction d : shuffled) {
            if (info.getState(d).isAir()) {
               info.setBlockPos(info.getPos().relative(d), (BlockState)outer.setValue(FurBlock.FACING, d));
            }
         }

         if (EndBlocks.TENANEA.isTreeLog(info.getState())) {
            for (int x = -6; x < 7; x++) {
               int ax = Math.abs(x);
               mut.setX(x + info.getPos().getX());

               for (int z = -6; z < 7; z++) {
                  int az = Math.abs(z);
                  mut.setZ(z + info.getPos().getZ());

                  for (int y = -6; y < 7; y++) {
                     int ay = Math.abs(y);
                     int dx = ax + ay + az;
                     if (dx < 7) {
                        mut.setY(y + info.getPos().getY());
                        BlockState state = info.getState(mut);
                        if (state.getBlock() instanceof LeavesBlock) {
                           int distance = (Integer)state.getValue(LeavesBlock.DISTANCE);
                           if (dx < distance) {
                              info.setState(mut, (BlockState)state.setValue(LeavesBlock.DISTANCE, dx));
                           }
                        }
                     }
                  }
               }
            }
         }

         return info.getState();
      });
      sphere.fillRecursiveIgnore(world, pos, IGNORE);
      BlocksHelper.setWithoutUpdate(world, pos, EndBlocks.TENANEA.getBark());
      support.forEach(bpos -> {
         if (GenerationArea.contains(world, bpos)) {
            BlockState state = world.getBlockState(bpos);
            if (state.isAir() || state.is(EndBlocks.TENANEA_OUTER_LEAVES)) {
               int count = MHelper.randRange(3, 8, random);
               mut.set(bpos);
               if (world.getBlockState(mut.above()).is(EndBlocks.TENANEA_LEAVES)) {
                  BlocksHelper.setWithoutUpdate(world, mut, top);

                  for (int i = 1; i < count; i++) {
                     mut.setY(mut.getY() - 1);
                     if (!world.isEmptyBlock(mut.below())) {
                        break;
                     }

                     BlocksHelper.setWithoutUpdate(world, mut, middle);
                  }

                  BlocksHelper.setWithoutUpdate(world, mut, bottom);
               }
            }
         }
      });
   }
}
