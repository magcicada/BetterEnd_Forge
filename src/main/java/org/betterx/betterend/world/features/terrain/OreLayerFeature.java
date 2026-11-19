package org.betterx.betterend.world.features.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFCoordModify;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.MHelper;

public class OreLayerFeature extends Feature<OreLayerFeatureConfig> {
   public OreLayerFeature() {
      super(OreLayerFeatureConfig.CODEC);
   }

   public boolean place(FeaturePlaceContext<OreLayerFeatureConfig> featureConfig) {
      OreLayerFeatureConfig cfg = (OreLayerFeatureConfig)featureConfig.config();
      RandomSource random = featureConfig.random();
      BlockPos pos = featureConfig.origin();
      WorldGenLevel world = featureConfig.level();
      float radius = cfg.radius * 0.5F;
      int r = MHelper.floor(radius + 1.0F);
      int posX = MHelper.randRange(Math.max(r - 16, 0), Math.min(31 - r, 15), random) + pos.getX();
      int posZ = MHelper.randRange(Math.max(r - 16, 0), Math.min(31 - r, 15), random) + pos.getZ();
      int posY = MHelper.randRange(cfg.minY, cfg.maxY, random);
      SDF body = new SDFSphere().setRadius(radius).setBlock(cfg.state);
      body = new SDFScale3D().setScale(1.0F, 0.2F, 1.0F).setSource(body);
      body = new SDFCoordModify().setFunction(vec -> {
         double x = (vec.x() + pos.getX()) * 0.1;
         double z = (vec.z() + pos.getZ()) * 0.1;
         double offset = cfg.getNoise(world.getSeed()).eval(x, z);
         vec.set(vec.x(), vec.y() + (float)offset * 8.0F, vec.z());
      }).setSource(body);
      body.setReplaceFunction(state -> state.is(Blocks.END_STONE));
      body.fillRecursive(world, new BlockPos(posX, posY, posZ));
      return true;
   }
}
