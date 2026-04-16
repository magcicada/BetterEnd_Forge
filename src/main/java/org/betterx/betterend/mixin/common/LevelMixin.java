package org.betterx.betterend.mixin.common;

import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.util.BETickChunkContext;
import org.betterx.betterend.world.generator.GeneratorOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

    @Inject(method = "getSharedSpawnPos", at = @At("HEAD"), cancellable = true)
    private void be_getSharedSpawnPos(CallbackInfoReturnable<BlockPos> info) {
        if (GeneratorOptions.changeSpawn()) {
            if ((Object) this instanceof ServerLevel server) {
                if (server.dimension() == Level.END) {
                    BlockPos pos = GeneratorOptions.getSpawn();
                    info.setReturnValue(pos);
                }
            }
        }
    }

    @Inject(method = "setBlockAndUpdate", at = @At("HEAD"), cancellable = true)
    private void be_modifyIceTick(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> info) {
        if (!((Object) this instanceof ServerLevel) || !BETickChunkContext.isInTickChunk() || !state.is(Blocks.ICE)) {
            return;
        }
        ServerLevel server = (ServerLevel) (Object) this;

        ResourceLocation biome = BiomeAPI.getBiomeID(server.getBiome(pos));
        if (BetterEnd.MOD_ID.equals(biome.getNamespace())) {
            info.setReturnValue(server.setBlock(pos, EndBlocks.EMERALD_ICE.defaultBlockState(), 3));
        }
    }
}
