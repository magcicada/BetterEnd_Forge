package org.betterx.betterend.world.features.bushes;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
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
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.operator.SDFSubtraction;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.blocks.basis.FurBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

public class TenaneaBushFeature extends DefaultFeature {
   private static final Function<BlockState, Boolean> REPLACE = BlocksHelper::replaceableOrPlant;
   private static final Direction[] DIRECTIONS = Direction.values();

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      if (!world.getBlockState(pos.below()).is(CommonBlockTags.END_STONES)) {
         return false;
      } else {
         float radius = MHelper.randRange(1.8F, 3.5F, random);
         OpenSimplexNoise noise = new OpenSimplexNoise(random.nextInt());
         BlockState leaves = EndBlocks.TENANEA_LEAVES.defaultBlockState();
         SDF sphere = new SDFSphere().setRadius(radius).setBlock(leaves);
         sphere = new SDFScale3D().setScale(1.0F, 0.75F, 1.0F).setSource(sphere);
         sphere = new SDFDisplacement().setFunction(vec -> (float)noise.eval(vec.x() * 0.2, vec.y() * 0.2, vec.z() * 0.2) * 3.0F).setSource(sphere);
         sphere = new SDFDisplacement().setFunction(vec -> MHelper.randRange(-2.0F, 2.0F, random)).setSource(sphere);
         sphere = new SDFSubtraction().setSourceA(sphere).setSourceB(new SDFTranslate().setTranslate(0.0F, -radius, 0.0F).setSource(sphere));
         sphere.setReplaceFunction(REPLACE);
         List<BlockPos> support = Lists.newArrayList();
         sphere.addPostProcess(info -> {
            if (info.getState().getBlock() instanceof LeavesBlock) {
               int distance = info.getPos().distManhattan(pos);
               if (distance < 7) {
                  if (random.nextInt(4) == 0 && info.getStateDown().isAir()) {
                     BlockPos d = info.getPos().below();
                     support.add(d);
                  }

                  Direction[] shuffled = (Direction[])DIRECTIONS.clone();
                  MHelper.shuffle(shuffled, random);

                  for (Direction d : shuffled) {
                     if (info.getState(d).isAir()) {
                        info.setBlockPos(info.getPos().relative(d), (BlockState)EndBlocks.TENANEA_OUTER_LEAVES.defaultBlockState().setValue(FurBlock.FACING, d));
                     }
                  }

                  return (BlockState)info.getState().setValue(LeavesBlock.DISTANCE, distance);
               } else {
                  return AIR;
               }
            } else {
               return info.getState();
            }
         });
         sphere.fillRecursive(world, pos);
         BlockState stem = EndBlocks.TENANEA.getBark().defaultBlockState();
         BlocksHelper.setWithoutUpdate(world, pos, stem);

         for (Direction d : Direction.values()) {
            BlockPos p = pos.relative(d);
            if (world.isEmptyBlock(p)) {
               BlocksHelper.setWithoutUpdate(world, p, (BlockState)leaves.setValue(LeavesBlock.DISTANCE, 1));
            }
         }

         MutableBlockPos mut = new MutableBlockPos();
         BlockState top = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.TOP);
         BlockState middle = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.MIDDLE);
         BlockState bottom = (BlockState)EndBlocks.TENANEA_FLOWERS.defaultBlockState().setValue(BlockProperties.TRIPLE_SHAPE, TripleShape.BOTTOM);
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
         return true;
      }
   }
}
