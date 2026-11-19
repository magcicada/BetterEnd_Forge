package org.betterx.betterend.world.generator;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFRadialNoiseMap;
import org.betterx.bclib.sdf.operator.SDFScale;
import org.betterx.bclib.sdf.operator.SDFSmoothUnion;
import org.betterx.bclib.sdf.operator.SDFTranslate;
import org.betterx.bclib.sdf.primitive.SDFCappedCone;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;

public class IslandLayer {
   private final SDFRadialNoiseMap noise;
   private final SDF island;
   private final List<BlockPos> positions = new ArrayList<>(9);
   private final Map<BlockPos, SDF> islands = Maps.newHashMap();
   private final OpenSimplexNoise density;
   private final int seed;
   private int lastX = Integer.MIN_VALUE;
   private int lastZ = Integer.MIN_VALUE;
   private final LayerOptions options;

   public IslandLayer(int seed, LayerOptions options) {
      this.density = new OpenSimplexNoise(seed);
      this.options = options;
      this.seed = seed;
      SDF cone1 = makeCone(0.0F, 0.4F, 0.2F, -0.3F);
      SDF cone2 = makeCone(0.4F, 0.5F, 0.1F, -0.1F);
      SDF cone3 = makeCone(0.5F, 0.45F, 0.03F, 0.0F);
      SDF cone4 = makeCone(0.45F, 0.0F, 0.02F, 0.03F);
      SDF coneBottom = new SDFSmoothUnion().setRadius(0.02F).setSourceA(cone1).setSourceB(cone2);
      SDF coneTop = new SDFSmoothUnion().setRadius(0.02F).setSourceA(cone3).setSourceB(cone4);
      this.noise = (SDFRadialNoiseMap)new SDFRadialNoiseMap().setSeed(seed).setRadius(0.5F).setIntensity(0.2F).setSource(coneTop);
      this.island = new SDFSmoothUnion().setRadius(0.01F).setSourceA(this.noise).setSourceB(coneBottom);
   }

   private int getSeed(int x, int z) {
      int h = this.seed + x * 374761393 + z * 668265263;
      h = (h ^ h >> 13) * 1274126177;
      return h ^ h >> 16;
   }

   public void updatePositions(double x, double z, int maxHeight) {
      int ix = MHelper.floor(x / this.options.distance);
      int iz = MHelper.floor(z / this.options.distance);
      if (this.lastX != ix || this.lastZ != iz) {
         this.lastX = ix;
         this.lastZ = iz;
         this.positions.clear();

         for (int pox = -1; pox < 2; pox++) {
            int px = pox + ix;

            for (int poz = -1; poz < 2; poz++) {
               int pz = poz + iz;
               if ((long)px * px + (long)pz * pz > this.options.centerDist) {
                  RandomSource random = new LegacyRandomSource(this.getSeed(px, pz));
                  double posX = (px + random.nextFloat()) * this.options.distance;
                  double posY = MHelper.randRange(this.options.minY, this.options.maxY, random) * maxHeight;
                  double posZ = (pz + random.nextFloat()) * this.options.distance;
                  if (this.density.eval(posX * 0.01, posZ * 0.01) > this.options.coverage) {
                     this.positions.add(new BlockPos((int)posX, (int)posY, (int)posZ));
                  }
               }
            }
         }
      }

      long innerVoidRadiusSqr = VoidRing.terrainRadiusSqr();
      int centerSize = (int)Math.sqrt(innerVoidRadiusSqr) >> 3;
      if (GeneratorOptions.hasCentralIsland() && Math.abs(ix) < centerSize && Math.abs(iz) < centerSize) {
         int count = this.positions.size();

         for (int n = 0; n < count; n++) {
            BlockPos pos = this.positions.get(n);
            long d = (long)pos.getX() * pos.getX() + (long)pos.getZ() * pos.getZ();
            if (d < innerVoidRadiusSqr) {
               this.positions.remove(n);
               count--;
               n--;
            }
         }

         if (this.options.hasCentralIsland) {
            this.positions.add(new BlockPos(0, 64, 0));
         }
      }
   }

   private SDF getIsland(BlockPos pos) {
      SDF island = this.islands.get(pos);
      if (island == null) {
         if (pos.getX() == 0 && pos.getZ() == 0) {
            island = new SDFScale().setScale(1.3F).setSource(this.island);
         } else {
            RandomSource random = new LegacyRandomSource(this.getSeed(pos.getX(), pos.getZ()));
            island = new SDFScale().setScale(random.nextFloat() + 0.5F).setSource(this.island);
         }

         this.islands.put(pos, island);
      }

      this.noise.setOffset(pos.getX(), pos.getZ());
      return island;
   }

   private float getRelativeDistance(SDF sdf, BlockPos center, double px, double py, double pz) {
      float x = (float)(px - center.getX()) / this.options.scale;
      float y = (float)(py - center.getY()) / this.options.scale;
      float z = (float)(pz - center.getZ()) / this.options.scale;
      return sdf.getDistance(x, y, z);
   }

   private float calculateSDF(double x, double y, double z) {
      float distance = 10.0F;

      for (BlockPos pos : this.positions) {
         SDF island = this.getIsland(pos);
         float dist = this.getRelativeDistance(island, pos, x, y, z);
         distance = MHelper.min(distance, dist);
      }

      return distance;
   }

   public float getDensity(double x, double y, double z) {
      return -this.calculateSDF(x, y, z);
   }

   public float getDensity(double x, double y, double z, float height) {
      this.noise.setIntensity(height);
      this.noise.setRadius(0.5F / (1.0F + height));
      return -this.calculateSDF(x, y, z);
   }

   public void clearCache() {
      if (this.islands.size() > 128) {
         this.islands.clear();
      }
   }

   private static SDF makeCone(float radiusBottom, float radiusTop, float height, float minY) {
      float hh = height * 0.5F;
      SDF sdf = new SDFCappedCone().setHeight(hh).setRadius1(radiusBottom).setRadius2(radiusTop);
      return new SDFTranslate().setTranslate(0.0F, minY + hh, 0.0F).setSource(sdf);
   }
}
