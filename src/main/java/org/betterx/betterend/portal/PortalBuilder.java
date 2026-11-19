package org.betterx.betterend.portal;

import com.google.common.collect.Sets;
import java.awt.Point;
import java.util.Optional;
import java.util.Set;
import net.minecraft.BlockUtil;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.betterend.blocks.EndPortalBlock;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndPoiTypes;
import org.betterx.betterend.rituals.EternalRitual;

public class PortalBuilder {
   public static final Set<Point> FRAME_POSITIONS = Sets.newHashSet(
      new Point[]{
         new Point(0, 0),
         new Point(0, 6),
         new Point(1, 0),
         new Point(1, 6),
         new Point(2, 1),
         new Point(2, 5),
         new Point(3, 2),
         new Point(3, 3),
         new Point(3, 4)
      }
   );
   public static final Set<Point> PORTAL_POSITIONS = Sets.newHashSet(
      new Point[]{
         new Point(0, 0),
         new Point(0, 1),
         new Point(0, 2),
         new Point(0, 3),
         new Point(0, 4),
         new Point(1, 0),
         new Point(1, 1),
         new Point(1, 2),
         new Point(1, 3),
         new Point(1, 4),
         new Point(2, 1),
         new Point(2, 2),
         new Point(2, 3)
      }
   );
   private static final Set<Point> BASE_POSITIONS = Sets.newHashSet(
      new Point[]{new Point(3, 0), new Point(2, 0), new Point(2, 1), new Point(1, 1), new Point(1, 2), new Point(0, 1), new Point(0, 2)}
   );
   private static final Block BASE = EndBlocks.FLAVOLITE.tiles;
   public static final Block FRAME = EndBlocks.FLAVOLITE_RUNED_ETERNAL;
   public static final Block PORTAL = EndBlocks.END_PORTAL_BLOCK;
   public static int SPIRAL_SEARCH_RADIUS = 128;
   public static int GENERATE_SEARCH_RADIUS = 16;
   private static final int PROBE_FOOTPRINT = 4;
   private final ServerLevel targetLevel;
   private final Level sourceLevel;

   public PortalBuilder(Level sourceLevel, ServerLevel targetLevel) {
      this.targetLevel = targetLevel;
      this.sourceLevel = sourceLevel;
   }

   public static void generatePortal(Level world, BlockPos center, Axis axis, int portalId) {
      BlockPos framePos = center.below();
      Direction moveDir = Axis.X == axis ? Direction.EAST : Direction.NORTH;
      BlockState frame = (BlockState)FRAME.defaultBlockState().setValue(EternalRitual.ACTIVE, true);
      FRAME_POSITIONS.forEach(point -> {
         BlockPos pos = framePos.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         world.setBlockAndUpdate(pos, frame);
         pos = framePos.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         world.setBlockAndUpdate(pos, frame);
      });
      BlockState portal = (BlockState)((BlockState)PORTAL.defaultBlockState().setValue(EndPortalBlock.AXIS, axis)).setValue(EndPortalBlock.PORTAL, portalId);
      PORTAL_POSITIONS.forEach(point -> {
         BlockPos pos = center.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         world.setBlockAndUpdate(pos, portal);
         pos = center.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         world.setBlockAndUpdate(pos, portal);
      });
      generateBase(world, framePos, moveDir);
   }

   private static void generateBase(Level world, BlockPos center, Direction moveX) {
      BlockState base = BASE.defaultBlockState();
      Direction moveY = moveX.getClockWise();
      BASE_POSITIONS.forEach(point -> {
         BlockPos pos = center.mutable().move(moveX, point.x).move(moveY, point.y);
         world.setBlockAndUpdate(pos, base);
         pos = center.mutable().move(moveX, -point.x).move(moveY, point.y);
         world.setBlockAndUpdate(pos, base);
         pos = center.mutable().move(moveX, point.x).move(moveY, -point.y);
         world.setBlockAndUpdate(pos, base);
         pos = center.mutable().move(moveX, -point.x).move(moveY, -point.y);
         world.setBlockAndUpdate(pos, base);
      });
   }

   public static boolean checkIsAreaValid(Level world, BlockPos pos, Axis axis) {
      if (pos.getY() >= world.getHeight() - 1) {
         return false;
      } else {
         return !isBaseValid(world, pos, axis) ? false : checkArea(world, pos, axis);
      }
   }

   private static boolean isBaseValid(Level world, BlockPos pos, Axis axis) {
      boolean solid = true;
      if (axis.equals(Axis.X)) {
         pos = pos.below().offset(0, 0, -3);

         for (int i = 0; i < 7; i++) {
            BlockPos checkPos = pos.offset(0, 0, i);
            BlockState state = world.getBlockState(checkPos);
            solid &= validBlock(world, checkPos, state);
         }
      } else {
         pos = pos.below().offset(-3, 0, 0);

         for (int i = 0; i < 7; i++) {
            BlockPos checkPos = pos.offset(i, 0, 0);
            BlockState state = world.getBlockState(checkPos);
            solid &= validBlock(world, checkPos, state);
         }
      }

      return solid;
   }

   private static boolean validBlock(Level world, BlockPos pos, BlockState state) {
      return state.isRedstoneConductor(world, pos) && state.isCollisionShapeFullBlock(world, pos);
   }

   public static boolean checkArea(Level world, BlockPos center, Axis axis) {
      Direction moveDir = Axis.X == axis ? Direction.NORTH : Direction.EAST;

      for (BlockPos checkPos : BlockPos.betweenClosed(center.relative(moveDir.getClockWise()), center.relative(moveDir.getCounterClockWise()))) {
         for (Point point : PORTAL_POSITIONS) {
            BlockPos pos = checkPos.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
            BlockState state = world.getBlockState(pos);
            if (isStateInvalid(state)) {
               return false;
            }

            pos = checkPos.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
            state = world.getBlockState(pos);
            if (isStateInvalid(state)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean isStateInvalid(BlockState state) {
      return !state.getFluidState().isEmpty() ? true : !BlocksHelper.replaceableOrPlant(state);
   }

   public Optional<FoundRectangle> findPortalAround(BlockPos blockPos, WorldBorder worldBorder) {
      Optional<BlockPos> oPos = Optional.empty();
      if (EndPoiTypes.ETERNAL_PORTAL != null) {
         PoiManager poiManager = this.targetLevel.getPoiManager();
         poiManager.ensureLoadedAndValid(this.targetLevel, blockPos, SPIRAL_SEARCH_RADIUS);
         oPos = EndPoiTypes.ETERNAL_PORTAL.findPoiAround(this.targetLevel, blockPos, SPIRAL_SEARCH_RADIUS, worldBorder);
      }

      return oPos.map(
         poiPos -> {
            this.targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(poiPos), 3, poiPos);
            BlockState blockState = this.targetLevel.getBlockState(poiPos);
            return BlockUtil.getLargestRectangleAround(
               poiPos, (Axis)blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Axis.Y, 21, bp -> this.targetLevel.getBlockState(bp) == blockState
            );
         }
      );
   }

   public BlockPos getPortalCenter(FoundRectangle rectangle) {
      return getPortalCenter(this.targetLevel, rectangle);
   }

   public static BlockPos getPortalCenter(Level level, FoundRectangle rectangle) {
      BlockState portalState = level.getBlockState(rectangle.minCorner);
      Axis portalAxis = portalState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS) ? (Axis)portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS) : Axis.X;
      MutableBlockPos center = rectangle.minCorner.relative(Direction.get(AxisDirection.POSITIVE, portalAxis), rectangle.axis1Size / 2).mutable();

      while (center.getY() > level.getMinBuildHeight() && level.getBlockState(center.below()).is(PORTAL)) {
         center.move(Direction.DOWN);
      }

      return center.immutable();
   }

   public static FoundRectangle getPortalRectangle(Level level, BlockPos portalPos) {
      BlockState blockState = level.getBlockState(portalPos);
      return BlockUtil.getLargestRectangleAround(portalPos, (Axis)blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Axis.Y, 21, bp -> level.getBlockState(bp) == blockState);
   }

   public static BlockPos getStartingPos(Level sourceLevel, Level targetLevel, Entity entity, WorldBorder worldBorder) {
      double dimensionScale = DimensionType.getTeleportationScale(sourceLevel.dimensionType(), targetLevel.dimensionType());
      return worldBorder.clampToBounds(entity.getX() * dimensionScale, entity.getY(), entity.getZ() * dimensionScale);
   }

   public Optional<BlockPos> createPortal(BlockPos startPosition, Axis axis, int portalID) {
      Direction portalDirection = Direction.get(AxisDirection.POSITIVE, axis);
      double d = -1.0;
      BlockPos centerPos = null;
      double e = -1.0;
      BlockPos blockPos3 = null;
      WorldBorder worldBorder = this.targetLevel.getWorldBorder();
      int maxHeight = Math.min(this.targetLevel.getMaxBuildHeight(), this.targetLevel.getMinBuildHeight() + this.targetLevel.getLogicalHeight()) - 1;
      int genMinChunkX = SectionPos.blockToSectionCoord(startPosition.getX() - GENERATE_SEARCH_RADIUS - 4);
      int genMaxChunkX = SectionPos.blockToSectionCoord(startPosition.getX() + GENERATE_SEARCH_RADIUS + 4);
      int genMinChunkZ = SectionPos.blockToSectionCoord(startPosition.getZ() - GENERATE_SEARCH_RADIUS - 4);
      int genMaxChunkZ = SectionPos.blockToSectionCoord(startPosition.getZ() + GENERATE_SEARCH_RADIUS + 4);
      MutableBlockPos currentPos = startPosition.mutable();

      for (MutableBlockPos testPosition : BlockPos.spiralAround(startPosition, SPIRAL_SEARCH_RADIUS, Direction.EAST, Direction.SOUTH)) {
         int ring = Math.max(Math.abs(testPosition.getX() - startPosition.getX()), Math.abs(testPosition.getZ() - startPosition.getZ()));
         if (d != -1.0 && (double)ring * ring >= d) {
            break;
         }

         if (ring <= GENERATE_SEARCH_RADIUS
            || this.isColumnLoaded(testPosition.getX(), testPosition.getZ(), genMinChunkX, genMaxChunkX, genMinChunkZ, genMaxChunkZ)) {
            int levelHeight = Math.min(maxHeight, this.targetLevel.getHeight(Types.MOTION_BLOCKING, testPosition.getX(), testPosition.getZ()));
            if (worldBorder.isWithinBounds(testPosition) && worldBorder.isWithinBounds(testPosition.move(portalDirection, 1))) {
               testPosition.move(portalDirection.getOpposite(), 1);

               for (int yy = levelHeight; yy >= this.targetLevel.getMinBuildHeight(); yy--) {
                  testPosition.setY(yy);
                  if (this.canPortalReplaceBlock(testPosition)) {
                     int startY = yy;

                     while (yy > this.targetLevel.getMinBuildHeight() && this.canPortalReplaceBlock(testPosition.move(Direction.DOWN))) {
                        yy--;
                     }

                     int n;
                     if (yy + 4 <= maxHeight && ((n = startY - yy) <= 0 || n >= 3)) {
                        testPosition.setY(yy);
                        if (this.canHostFrame(testPosition, currentPos, portalDirection, 0)) {
                           double f = startPosition.distSqr(testPosition);
                           if (this.canHostFrame(testPosition, currentPos, portalDirection, -1)
                              && this.canHostFrame(testPosition, currentPos, portalDirection, 1)
                              && (d == -1.0 || d > f)) {
                              d = f;
                              centerPos = testPosition.immutable();
                           }

                           if (d == -1.0 && (e == -1.0 || e > f)) {
                              e = f;
                              blockPos3 = testPosition.immutable();
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (d == -1.0 && e != -1.0) {
         centerPos = blockPos3;
         d = e;
      }

      if (d == -1.0) {
         int p = maxHeight - 9;
         int o = Math.max(this.targetLevel.getMinBuildHeight() - -1, 70);
         if (p < o) {
            return Optional.empty();
         }

         centerPos = new BlockPos(startPosition.getX(), Mth.clamp(startPosition.getY(), o, p), startPosition.getZ()).immutable();
         if (!worldBorder.isWithinBounds(centerPos)) {
            return Optional.empty();
         }
      }

      this.buildPortal(portalDirection, centerPos, portalID);
      return Optional.of(centerPos.immutable());
   }

   private void buildPortal(Direction portalDirection, BlockPos centerPos, int portalID) {
      Axis portalAxis = Axis.X == portalDirection.getAxis() ? Axis.Z : Axis.X;
      generatePortal(this.targetLevel, centerPos, portalAxis, portalID);
   }

   private int getPortalID(BlockPos portalEntrancePos) {
      BlockState currentState = this.sourceLevel.getBlockState(portalEntrancePos);
      return currentState.hasProperty(EndPortalBlock.PORTAL) ? (Integer)currentState.getValue(EndPortalBlock.PORTAL) : -1;
   }

   private boolean isColumnLoaded(int x, int z, int genMinChunkX, int genMaxChunkX, int genMinChunkZ, int genMaxChunkZ) {
      for (int dx = -4; dx <= 4; dx += 8) {
         for (int dz = -4; dz <= 4; dz += 8) {
            int chunkX = SectionPos.blockToSectionCoord(x + dx);
            int chunkZ = SectionPos.blockToSectionCoord(z + dz);
            if ((chunkX < genMinChunkX || chunkX > genMaxChunkX || chunkZ < genMinChunkZ || chunkZ > genMaxChunkZ)
               && this.targetLevel.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean canPortalReplaceBlock(BlockPos currentPos) {
      BlockState blockState = this.targetLevel.getBlockState(currentPos);
      return blockState.canBeReplaced() && blockState.getFluidState().isEmpty();
   }

   private boolean canHostFrame(BlockPos pos, MutableBlockPos currentPos, Direction direction, int widthScale) {
      Direction orthogonalDir = direction.getClockWise();

      for (int x = -1; x < 3; x++) {
         for (int y = -1; y < 4; y++) {
            currentPos.setWithOffset(
               pos, direction.getStepX() * x + orthogonalDir.getStepX() * widthScale, y, direction.getStepZ() * x + orthogonalDir.getStepZ() * widthScale
            );
            if (y < 0 && !this.targetLevel.getBlockState(currentPos).isSolid()) {
               return false;
            }

            if (y >= 0 && !this.canPortalReplaceBlock(currentPos)) {
               return false;
            }
         }
      }

      return true;
   }
}
