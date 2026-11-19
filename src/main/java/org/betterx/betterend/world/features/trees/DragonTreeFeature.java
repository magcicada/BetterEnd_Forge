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
import org.betterx.bclib.sdf.PosInfo;
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
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;
import org.joml.Vector3f;

public class DragonTreeFeature extends DefaultFeature {
   private static final Function<BlockState, Boolean> REPLACE = state -> state.getBlock() == EndBlocks.DRAGON_TREE_LEAVES
      ? true
      : BlocksHelper.replaceableOrPlant(state);
   private static final Function<BlockState, Boolean> IGNORE = EndBlocks.DRAGON_TREE::isTreeLog;
   private static final Function<PosInfo, BlockState> POST = info -> EndBlocks.DRAGON_TREE.isTreeLog(info.getStateUp())
         && EndBlocks.DRAGON_TREE.isTreeLog(info.getStateDown())
      ? EndBlocks.DRAGON_TREE.getBark().defaultBlockState()
      : info.getState();
   private static final List<Vector3f> BRANCH = Lists.newArrayList(
      new Vector3f[]{
         new Vector3f(0.0F, 0.0F, 0.0F),
         new Vector3f(0.1F, 0.3F, 0.0F),
         new Vector3f(0.4F, 0.6F, 0.0F),
         new Vector3f(0.8F, 0.8F, 0.0F),
         new Vector3f(1.0F, 1.0F, 0.0F)
      }
   );
   private static final List<Vector3f> SIDE1 = Lists.newArrayList(
      new Vector3f[]{new Vector3f(0.4F, 0.6F, 0.0F), new Vector3f(0.8F, 0.8F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F)}
   );
   private static final List<Vector3f> SIDE2 = SplineHelper.copySpline(SIDE1);
   private static final List<Vector3f> ROOT = Lists.newArrayList(
      new Vector3f[]{
         new Vector3f(0.0F, 1.0F, 0.0F),
         new Vector3f(0.1F, 0.7F, 0.0F),
         new Vector3f(0.3F, 0.3F, 0.0F),
         new Vector3f(0.7F, 0.05F, 0.0F),
         new Vector3f(0.8F, -0.2F, 0.0F)
      }
   );

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      if (!world.getBlockState(pos.below()).is(BlockTags.NYLIUM)) {
         return false;
      } else {
         float size = MHelper.randRange(10, 25, random);
         List<Vector3f> spline = SplineHelper.makeSpline(0.0F, 0.0F, 0.0F, 0.0F, size, 0.0F, 6);
         SplineHelper.offsetParts(spline, random, 1.0F, 0.0F, 1.0F);
         if (!SplineHelper.canGenerate(spline, pos, world, REPLACE)) {
            return false;
         } else {
            BlocksHelper.setWithoutUpdate(world, pos, AIR);
            Vector3f last = SplineHelper.getPos(spline, 3.5F);
            OpenSimplexNoise noise = new OpenSimplexNoise(random.nextLong());
            float radius = size * MHelper.randRange(0.5F, 0.7F, random);
            this.makeCap(world, pos.offset((int)last.x(), (int)last.y(), (int)last.z()), radius, random, noise);
            last = spline.get(0);
            this.makeRoots(world, pos.offset((int)last.x(), (int)last.y(), (int)last.z()), radius, random);
            radius = MHelper.randRange(1.2F, 2.3F, random);
            SDF function = SplineHelper.buildSDF(spline, radius, 1.2F, bpos -> EndBlocks.DRAGON_TREE.getBark().defaultBlockState());
            function.setReplaceFunction(REPLACE);
            function.addPostProcess(POST);
            function.fillRecursiveIgnore(world, pos, IGNORE);
            return true;
         }
      }
   }

   private void makeCap(WorldGenLevel world, BlockPos pos, float radius, RandomSource random, OpenSimplexNoise noise) {
      int count = (int)radius;
      int offset = (int)(BRANCH.get(BRANCH.size() - 1).y() * radius);

      for (int i = 0; i < count; i++) {
         float angle = (float)i / count * (float) (Math.PI * 2);
         float scale = radius * MHelper.randRange(0.85F, 1.15F, random);
         List<Vector3f> branch = SplineHelper.copySpline(BRANCH);
         SplineHelper.rotateSpline(branch, angle);
         SplineHelper.scale(branch, scale);
         SplineHelper.fillSpline(branch, world, EndBlocks.DRAGON_TREE.getBark().defaultBlockState(), pos, REPLACE);
         branch = SplineHelper.copySpline(SIDE1);
         SplineHelper.rotateSpline(branch, angle);
         SplineHelper.scale(branch, scale);
         SplineHelper.fillSpline(branch, world, EndBlocks.DRAGON_TREE.getBark().defaultBlockState(), pos, REPLACE);
         branch = SplineHelper.copySpline(SIDE2);
         SplineHelper.rotateSpline(branch, angle);
         SplineHelper.scale(branch, scale);
         SplineHelper.fillSpline(branch, world, EndBlocks.DRAGON_TREE.getBark().defaultBlockState(), pos, REPLACE);
      }

      this.leavesBall(world, pos.above(offset), radius * 1.15F + 2.0F, random, noise);
   }

   private void makeRoots(WorldGenLevel world, BlockPos pos, float radius, RandomSource random) {
      int count = (int)(radius * 1.5F);

      for (int i = 0; i < count; i++) {
         float angle = (float)i / count * (float) (Math.PI * 2);
         float scale = radius * MHelper.randRange(0.85F, 1.15F, random);
         List<Vector3f> branch = SplineHelper.copySpline(ROOT);
         SplineHelper.rotateSpline(branch, angle);
         SplineHelper.scale(branch, scale);
         Vector3f last = branch.get(branch.size() - 1);
         if (GenerationArea.getBlockState(world, pos.offset((int)last.x(), (int)last.y(), (int)last.z())).is(CommonBlockTags.GEN_END_STONES)) {
            SplineHelper.fillSpline(branch, world, EndBlocks.DRAGON_TREE.getBark().defaultBlockState(), pos, REPLACE);
         }
      }
   }

   private void leavesBall(WorldGenLevel world, BlockPos pos, float radius, RandomSource random, OpenSimplexNoise noise) {
      SDF sphere = new SDFSphere().setRadius(radius).setBlock((BlockState)EndBlocks.DRAGON_TREE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 6));
      SDF sub = new SDFScale().setScale(5.0F).setSource(sphere);
      sub = new SDFTranslate().setTranslate(0.0F, -radius * 5.0F, 0.0F).setSource(sub);
      sphere = new SDFSubtraction().setSourceA(sphere).setSourceB(sub);
      sphere = new SDFScale3D().setScale(1.0F, 0.5F, 1.0F).setSource(sphere);
      sphere = new SDFDisplacement().setFunction(vec -> (float)noise.eval(vec.x() * 0.2, vec.y() * 0.2, vec.z() * 0.2) * 1.5F).setSource(sphere);
      sphere = new SDFDisplacement().setFunction(vec -> random.nextFloat() * 3.0F - 1.5F).setSource(sphere);
      MutableBlockPos mut = new MutableBlockPos();
      sphere.addPostProcess(info -> {
         if (random.nextInt(5) == 0) {
            for (Direction dir : Direction.values()) {
               BlockState statexx = info.getState(dir, 2);
               if (statexx.isAir()) {
                  return info.getState();
               }
            }

            info.setState(EndBlocks.DRAGON_TREE.getBark().defaultBlockState());

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
                        BlockState statex = info.getState(mut);
                        if (statex.getBlock() instanceof LeavesBlock) {
                           int distance = (Integer)statex.getValue(LeavesBlock.DISTANCE);
                           if (dx < distance) {
                              info.setState(mut, (BlockState)statex.setValue(LeavesBlock.DISTANCE, dx));
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
      if (radius > 5.0F) {
         int count = (int)(radius * 2.5F);

         for (int i = 0; i < count; i++) {
            BlockPos p = pos.offset((int)(random.nextGaussian() * 1.0), (int)(random.nextGaussian() * 1.0), (int)(random.nextGaussian() * 1.0));
            boolean place = true;

            for (Direction d : Direction.values()) {
               BlockState state = GenerationArea.getBlockState(world, p.relative(d));
               if (!EndBlocks.DRAGON_TREE.isTreeLog(state) && !state.is(EndBlocks.DRAGON_TREE_LEAVES)) {
                  place = false;
                  break;
               }
            }

            if (place) {
               BlocksHelper.setWithoutUpdate(world, p, EndBlocks.DRAGON_TREE.getBark());
            }
         }
      }

      BlocksHelper.setWithoutUpdate(world, pos, EndBlocks.DRAGON_TREE.getBark());
   }

   static {
      Vector3f offset1 = new Vector3f(-0.4F, -0.6F, 0.0F);
      Vector3f offset2 = new Vector3f(0.4F, 0.6F, 0.0F);
      SplineHelper.offset(SIDE1, offset1);
      SplineHelper.offset(SIDE2, offset1);
      SplineHelper.rotateSpline(SIDE1, 0.5F);
      SplineHelper.rotateSpline(SIDE2, -0.5F);
      SplineHelper.offset(SIDE1, offset2);
      SplineHelper.offset(SIDE2, offset2);
      SplineHelper.offset(ROOT, new Vector3f(0.0F, -0.45F, 0.0F));
   }
}
