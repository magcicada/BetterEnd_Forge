package org.betterx.betterend.world.features;

import org.betterx.bclib.blocks.BaseAttachedBlock;
import org.betterx.bclib.blocks.BaseWallPlantBlock;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.betterend.BetterEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WallPlantFeature extends WallScatterFeature<WallPlantFeatureConfig> {
    private static final Set<String> WARNED_INVALID_PLANT_STATES = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<BlockState> PLANT_STATE = new ThreadLocal<>();

    public WallPlantFeature() {
        super(WallPlantFeatureConfig.CODEC);
    }

    @Override
    public boolean canGenerate(
            WallPlantFeatureConfig cfg,
            WorldGenLevel world,
            RandomSource random,
            BlockPos pos,
            Direction dir
    ) {
        BlockState plant = resolvePlantState(cfg, world, random, pos, dir);
        if (plant == null) {
            clearCachedPlantState();
            return false;
        }
        cachePlantState(plant);
        Block block = plant.getBlock();
        return block.canSurvive(plant, world, pos);
    }

    @Override
    public void generate(
            WallPlantFeatureConfig cfg,
            WorldGenLevel world,
            RandomSource random,
            BlockPos pos,
            Direction dir
    ) {
        BlockState plant = getCachedPlantState();
        if (plant == null) {
            plant = resolvePlantState(cfg, world, random, pos, dir);
        }
        clearCachedPlantState();
        if (plant == null) {
            return;
        }
        BlocksHelper.setWithoutUpdate(world, pos, plant);
    }

    protected final void clearCachedPlantState() {
        PLANT_STATE.remove();
    }

    protected final BlockState getCachedPlantState() {
        return PLANT_STATE.get();
    }

    private void cachePlantState(BlockState plant) {
        PLANT_STATE.set(plant);
    }

    private BlockState resolvePlantState(
            WallPlantFeatureConfig cfg,
            WorldGenLevel world,
            RandomSource random,
            BlockPos pos,
            Direction dir
    ) {
        BlockState state = cfg.getPlantState(random, pos);
        if (state == null || state.isAir()) {
            return null;
        }
        Block block = state.getBlock();
        if (block instanceof BaseWallPlantBlock) {
            return withDirectionOrFallback(state, block, BaseWallPlantBlock.FACING, dir, pos);
        } else if (block instanceof BaseAttachedBlock) {
            return withDirectionOrFallback(state, block, BlockStateProperties.FACING, dir, pos);
        }
        return state;
    }

    private <T extends Comparable<T>> BlockState withDirectionOrFallback(
            BlockState state,
            Block block,
            Property<T> property,
            T dir,
            BlockPos pos
    ) {
        if (state.hasProperty(property)) {
            return state.setValue(property, dir);
        }

        BlockState fallback = block.defaultBlockState();
        if (fallback.hasProperty(property)) {
            return fallback.setValue(property, dir);
        }

        warnInvalidState(block, pos);
        return null;
    }

    private void warnInvalidState(
            Block block,
            BlockPos pos
    ) {
        final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        final String key = blockId == null ? block.toString() : blockId.toString();
        if (WARNED_INVALID_PLANT_STATES.add(key)) {
            BetterEnd.LOGGER.warning(
                    "[WallPlantFeature] Skipping invalid wall-plant state for block "
                            + key
                            + " at "
                            + pos
            );
        }
    }
}
