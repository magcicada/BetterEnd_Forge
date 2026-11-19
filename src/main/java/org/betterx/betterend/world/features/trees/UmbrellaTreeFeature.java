package org.betterx.betterend.world.features.trees;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFFlatWave;
import org.betterx.bclib.sdf.operator.SDFScale;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.operator.SDFSmoothUnion;
import org.betterx.bclib.sdf.operator.SDFSubtraction;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.operator.SDFUnion;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.bclib.util.SplineHelper;
import org.betterx.betterend.blocks.UmbrellaTreeClusterBlock;
import org.betterx.betterend.blocks.UmbrellaTreeMembraneBlock;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;
import org.joml.Vector3f;

public class UmbrellaTreeFeature extends DefaultFeature {
   private static final Function<BlockState, Boolean> REPLACE = state -> state.is(EndBlocks.UMBRELLA_TREE_MEMBRANE)
      ? true
      : BlocksHelper.replaceableOrPlant(state);
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
   private static final List<Vector3f> ROOT = Lists.newArrayList(
      new Vector3f[]{new Vector3f(0.1F, 0.7F, 0.0F), new Vector3f(0.3F, 0.3F, 0.0F), new Vector3f(0.7F, 0.05F, 0.0F), new Vector3f(0.8F, -0.2F, 0.0F)}
   );

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      NoneFeatureConfiguration config = (NoneFeatureConfiguration)featureConfig.config();
      if (!world.getBlockState(pos.below()).is(BlockTags.NYLIUM)) {
         return false;
      } else {
         BlockState wood = EndBlocks.UMBRELLA_TREE.getBark().defaultBlockState();
         BlockState membrane = (BlockState)EndBlocks.UMBRELLA_TREE_MEMBRANE.defaultBlockState().setValue(UmbrellaTreeMembraneBlock.COLOR, 1);
         BlockState center = (BlockState)EndBlocks.UMBRELLA_TREE_MEMBRANE.defaultBlockState().setValue(UmbrellaTreeMembraneBlock.COLOR, 0);
         BlockState fruit = (BlockState)EndBlocks.UMBRELLA_TREE_CLUSTER.defaultBlockState().setValue(UmbrellaTreeClusterBlock.NATURAL, true);
         float size = MHelper.randRange(10, 20, random);
         int count = (int)(size * 0.15F);
         float var = (float) (Math.PI * 2) / (count * 3);
         float start = MHelper.randRange(0.0F, (float) (Math.PI * 2), random);
         SDF sdf = null;
         List<UmbrellaTreeFeature.Center> centers = Lists.newArrayList();
         float scale = 1.0F;
         if (config != null) {
            scale = MHelper.randRange(1.0F, 1.7F, random);
         }

         for (int i = 0; i < count; i++) {
            float angle = (float)i / count * (float) (Math.PI * 2) + MHelper.randRange(0.0F, var, random) + start;
            List<Vector3f> spline = SplineHelper.copySpline(SPLINE);
            float sizeXZ = (size + MHelper.randRange(0.0F, size * 0.5F, random)) * 0.7F;
            SplineHelper.scale(spline, sizeXZ, sizeXZ * MHelper.randRange(1.0F, 2.0F, random), sizeXZ);
            SplineHelper.rotateSpline(spline, angle);
            SplineHelper.offsetParts(spline, random, 0.5F, 0.0F, 0.5F);
            if (SplineHelper.canGenerate(spline, pos, world, REPLACE)) {
               float rScale = (scale - 1.0F) * 0.4F + 1.0F;
               SDF branch = SplineHelper.buildSDF(spline, 1.2F * rScale, 0.8F * rScale, bpos -> wood);
               Vector3f vec = spline.get(spline.size() - 1);
               float radius = (size + MHelper.randRange(0.0F, size * 0.5F, random)) * 0.4F;
               sdf = (SDF)(sdf == null ? branch : new SDFUnion().setSourceA(sdf).setSourceB(branch));
               SDF mem = this.makeMembrane(radius, random, membrane, center);
               float px = MHelper.floor(vec.x()) + 0.5F;
               float py = MHelper.floor(vec.y()) + 0.5F;
               float pz = MHelper.floor(vec.z()) + 0.5F;
               SDF var38 = new SDFTranslate().setTranslate(px, py, pz).setSource(mem);
               sdf = new SDFSmoothUnion().setRadius(2.0F).setSourceA(sdf).setSourceB(var38);
               centers.add(
                  new UmbrellaTreeFeature.Center(
                     (double)pos.getX() + px * scale, (double)pos.getY() + py * scale, (double)pos.getZ() + pz * scale, radius * scale
                  )
               );
            }
         }

         if (sdf == null) {
            return false;
         } else {
            if (scale > 1.0F) {
               sdf = new SDFScale().setScale(scale).setSource(sdf);
            }

            sdf.setReplaceFunction(REPLACE).addPostProcess(info -> {
               if (EndBlocks.UMBRELLA_TREE.isTreeLog(info.getStateUp()) && EndBlocks.UMBRELLA_TREE.isTreeLog(info.getStateDown())) {
                  return EndBlocks.UMBRELLA_TREE.getBark().defaultBlockState();
               } else if (info.getState().equals(membrane)) {
                  UmbrellaTreeFeature.Center min = centers.get(0);
                  double d = Double.MAX_VALUE;
                  BlockPos bpos = info.getPos();

                  for (UmbrellaTreeFeature.Center cx : centers) {
                     double d2 = cx.distance(bpos.getX(), bpos.getZ());
                     if (d2 < d) {
                        d = d2;
                        min = cx;
                     }
                  }

                  int color = MHelper.floor(d / min.radius * 7.0);
                  color = Mth.clamp(color, 1, 7);
                  return (BlockState)info.getState().setValue(UmbrellaTreeMembraneBlock.COLOR, color);
               } else {
                  return info.getState();
               }
            }).fillRecursive(world, pos);
            this.makeRoots(world, pos, (size * 0.5F + 3.0F) * scale, random, wood);

            for (UmbrellaTreeFeature.Center c : centers) {
               if (!GenerationArea.getBlockState(world, new BlockPos((int)c.px, (int)c.py, (int)c.pz)).isAir()) {
                  count = MHelper.floor(MHelper.randRange(5.0F, 10.0F, random) * scale);
                  float startAngle = random.nextFloat() * (float) (Math.PI * 2);

                  for (int ix = 0; ix < count; ix++) {
                     float angle = (float)ix / count * (float) (Math.PI * 2) + startAngle;
                     float dist = MHelper.randRange(1.5F, 2.5F, random) * scale;
                     double px = c.px + Math.sin(angle) * dist;
                     double pz = c.pz + Math.cos(angle) * dist;
                     this.makeFruits(world, px, c.py - 1.0, pz, fruit);
                  }
               }
            }

            return true;
         }
      }
   }

   private void makeRoots(WorldGenLevel world, BlockPos pos, float radius, RandomSource random, BlockState wood) {
      int count = (int)(radius * 1.5F);

      for (int i = 0; i < count; i++) {
         float angle = (float)i / count * (float) (Math.PI * 2);
         float scale = radius * MHelper.randRange(0.85F, 1.15F, random);
         List<Vector3f> branch = SplineHelper.copySpline(ROOT);
         SplineHelper.rotateSpline(branch, angle);
         SplineHelper.scale(branch, scale);
         Vector3f last = branch.get(branch.size() - 1);
         if (GenerationArea.getBlockState(world, pos.offset((int)last.x(), (int)last.y(), (int)last.z())).is(CommonBlockTags.GEN_END_STONES)) {
            SplineHelper.fillSplineForce(branch, world, wood, pos, REPLACE);
         }
      }
   }

   private SDF makeMembrane(float radius, RandomSource random, BlockState membrane, BlockState center) {
      SDF sphere = new SDFSphere().setRadius(radius).setBlock(membrane);
      SDF sub = new SDFTranslate().setTranslate(0.0F, -4.0F, 0.0F).setSource(sphere);
      sphere = new SDFSubtraction().setSourceA(sphere).setSourceB(sub);
      sphere = new SDFScale3D().setScale(1.0F, 0.5F, 1.0F).setSource(sphere);
      sphere = new SDFTranslate().setTranslate(0.0F, 1.0F - radius * 0.5F, 0.0F).setSource(sphere);
      float angle = random.nextFloat() * (float) (Math.PI * 2);
      int count = (int)MHelper.randRange(radius, radius * 2.0F, random);
      if (count < 5) {
         count = 5;
      }

      sphere = new SDFFlatWave().setAngle(angle).setRaysCount(count).setIntensity(0.6F).setSource(sphere);
      SDF cent = new SDFSphere().setRadius(2.5F).setBlock(center);
      return new SDFUnion().setSourceA(sphere).setSourceB(cent);
   }

   private void makeFruits(WorldGenLevel world, double px, double py, double pz, BlockState fruit) {
      MutableBlockPos mut = new MutableBlockPos().set(px, py, pz);
      if (GenerationArea.contains(world, mut)) {
         for (int i = 0; i < 8; i++) {
            mut.move(Direction.DOWN);
            if (world.isEmptyBlock(mut)) {
               BlockState state = world.getBlockState(mut.above());
               if (state.is(EndBlocks.UMBRELLA_TREE_MEMBRANE) && (Integer)state.getValue(UmbrellaTreeMembraneBlock.COLOR) < 2) {
                  BlocksHelper.setWithoutUpdate(world, mut, fruit);
               }
               break;
            }
         }
      }
   }

   static {
      SplineHelper.offset(ROOT, new Vector3f(0.0F, -0.45F, 0.0F));
   }

   private static class Center {
      final double px;
      final double py;
      final double pz;
      final float radius;

      Center(double x, double y, double z, float radius) {
         this.px = x;
         this.py = y;
         this.pz = z;
         this.radius = radius;
      }

      double distance(float x, float z) {
         return MHelper.length(this.px - x, this.pz - z);
      }
   }
}
