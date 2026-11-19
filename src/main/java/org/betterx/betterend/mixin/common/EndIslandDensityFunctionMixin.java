package org.betterx.betterend.mixin.common;

import org.betterx.betterend.world.generator.VoidRing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(
   targets = {"net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction"}
)
public class EndIslandDensityFunctionMixin {
   @ModifyConstant(
      method = {"getHeightValue"},
      constant = {@Constant(
         longValue = 4096L
      )}
   )
   private static long be_voidRingLimit(long original) {
      return VoidRing.vanillaIslandLimit();
   }
}
