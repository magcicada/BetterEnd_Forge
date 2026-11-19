package org.betterx.betterend.blocks.basis;

import java.util.Map;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.betterx.bclib.blocks.BaseBlockNotFull;
import org.betterx.bclib.blocks.BlockProperties;
import org.betterx.bclib.client.models.ModelsHelper;

public class EndLanternBlock extends BaseBlockNotFull implements SimpleWaterloggedBlock, LiquidBlockContainer {
   public static final BooleanProperty IS_FLOOR = BlockProperties.IS_FLOOR;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public EndLanternBlock(Block source) {
      this(Properties.copy(source).lightLevel(bs -> 15).noOcclusion());
   }

   public EndLanternBlock(Properties settings) {
      super(settings.noOcclusion());
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> stateManager) {
      stateManager.add(new Property[]{IS_FLOOR, WATERLOGGED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      LevelReader worldView = ctx.getLevel();
      BlockPos blockPos = ctx.getClickedPos();
      Direction dir = ctx.getClickedFace();
      boolean water = worldView.getFluidState(blockPos).getType() == Fluids.WATER;
      if (dir != Direction.DOWN && dir != Direction.UP) {
         if (canSupportCenter(worldView, blockPos.above(), Direction.DOWN)) {
            return (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, false)).setValue(WATERLOGGED, water);
         } else {
            return canSupportCenter(worldView, blockPos.below(), Direction.UP)
               ? (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, true)).setValue(WATERLOGGED, water)
               : null;
         }
      } else if (dir == Direction.DOWN) {
         if (canSupportCenter(worldView, blockPos.above(), Direction.DOWN)) {
            return (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, false)).setValue(WATERLOGGED, water);
         } else {
            return canSupportCenter(worldView, blockPos.below(), Direction.UP)
               ? (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, true)).setValue(WATERLOGGED, water)
               : null;
         }
      } else if (canSupportCenter(worldView, blockPos.below(), Direction.UP)) {
         return (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, true)).setValue(WATERLOGGED, water);
      } else {
         return canSupportCenter(worldView, blockPos.above(), Direction.DOWN)
            ? (BlockState)((BlockState)this.defaultBlockState().setValue(IS_FLOOR, false)).setValue(WATERLOGGED, water)
            : null;
      }
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return state.getValue(IS_FLOOR) ? canSupportCenter(world, pos.below(), Direction.UP) : canSupportCenter(world, pos.above(), Direction.DOWN);
   }

   public BlockState updateShape(BlockState state, Direction facing, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
      Boolean water = (Boolean)state.getValue(WATERLOGGED);
      if (water) {
         world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      if (!this.canSurvive(state, world, pos)) {
         return water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
      } else {
         return state;
      }
   }

   public boolean canPlaceLiquid(BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
      return false;
   }

   public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState) {
      return false;
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public UnbakedModel getModelVariant(ResourceLocation stateId, BlockState blockState, Map<ResourceLocation, UnbakedModel> modelCache) {
      String floor = blockState.getValue(IS_FLOOR) ? "_floor" : "";
      ResourceLocation modelId = new ResourceLocation(stateId.getNamespace(), "block/" + stateId.getPath() + floor);
      this.registerBlockModel(stateId, modelId, blockState, modelCache);
      return ModelsHelper.createBlockSimple(modelId);
   }
}
