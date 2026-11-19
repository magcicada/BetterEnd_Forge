package org.betterx.betterend.rituals;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.border.WorldBorder;
import org.betterx.bclib.api.v2.dataexchange.DataExchangeAPI;
import org.betterx.bclib.blocks.BlockProperties;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.advancements.BECriteria;
import org.betterx.betterend.blocks.EndPortalBlock;
import org.betterx.betterend.blocks.EternalPedestal;
import org.betterx.betterend.blocks.RunedFlavolite;
import org.betterx.betterend.blocks.basis.PedestalBlock;
import org.betterx.betterend.blocks.entities.EternalPedestalEntity;
import org.betterx.betterend.network.RitualUpdate;
import org.betterx.betterend.portal.EternalPortalLinks;
import org.betterx.betterend.portal.PortalBuilder;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.betterend.registry.EndPortals;
import org.jetbrains.annotations.Nullable;

public class EternalRitual {
   public static final int ACTIVATION_CHARGE_TICKS = 60;
   private static final List<Point> PEDESTAL_POSITIONS = List.of(
      new Point(-4, -5), new Point(-4, 5), new Point(-6, 0), new Point(4, -5), new Point(4, 5), new Point(6, 0)
   );
   private static final Block PEDESTAL = EndBlocks.ETERNAL_PEDESTAL;
   public static final BooleanProperty ACTIVE = BlockProperties.ACTIVE;
   private Level world;
   private Axis axis;
   private ResourceLocation targetWorldId;
   private BlockPos center;
   private BlockPos exit;
   private boolean active = false;
   private boolean willActivate = false;
   private ResourceLocation pendingKeyItemId;
   private BlockPos pendingStartPosition;

   public static List<Point> pedestalOffsets() {
      return PEDESTAL_POSITIONS;
   }

   public EternalRitual(Level world) {
      this.world = world;
   }

   public EternalRitual(Level world, BlockPos initial) {
      this(world);
      this.configure(initial);
   }

   public BlockPos getCenter() {
      return this.center;
   }

   public Axis getAxis() {
      return this.axis;
   }

   public void setWorld(Level world) {
      this.world = world;
   }

   public Level getWorld() {
      return this.world;
   }

   public int getPedestalItemMask() {
      if (this.world != null && this.center != null && this.axis != null) {
         Direction moveX = this.axis == Axis.X ? Direction.EAST : Direction.SOUTH;
         Direction moveY = this.axis == Axis.X ? Direction.NORTH : Direction.EAST;
         int mask = 0;

         for (int i = 0; i < PEDESTAL_POSITIONS.size(); i++) {
            Point offset = PEDESTAL_POSITIONS.get(i);
            BlockPos pedestalPos = this.center.relative(moveX, offset.x).relative(moveY, offset.y);
            if (this.world.getBlockEntity(pedestalPos) instanceof EternalPedestalEntity pedestal && !pedestal.isEmpty()) {
               mask |= 1 << i;
            }
         }

         return mask;
      } else {
         return 0;
      }
   }

   @Nullable
   public ResourceLocation getTargetWorldId() {
      return this.targetWorldId;
   }

   private boolean isInvalid() {
      return this.world == null || this.world.isClientSide() || this.center == null || this.axis == null;
   }

   public void checkStructure(Player player) {
      if (!this.isInvalid()) {
         Direction moveX;
         Direction moveY;
         if (Axis.X == this.axis) {
            moveX = Direction.EAST;
            moveY = Direction.NORTH;
         } else {
            moveX = Direction.SOUTH;
            moveY = Direction.EAST;
         }

         boolean valid = this.checkFrame(this.world, this.center.below());
         Item item = null;

         for (Point pos : PEDESTAL_POSITIONS) {
            MutableBlockPos checkPos = this.center.mutable();
            checkPos.move(moveX, pos.x).move(moveY, pos.y);
            valid &= this.isActive(checkPos);
            if (valid) {
               EternalPedestalEntity pedestal = (EternalPedestalEntity)this.world.getBlockEntity(checkPos);
               if (pedestal != null) {
                  Item pItem = pedestal.getItem(0).getItem();
                  if (item == null) {
                     item = pItem;
                  } else if (!item.equals(pItem)) {
                     valid = false;
                  }
               }
            }
         }

         if (valid && item != null) {
            this.activatePortal(player, item);
            if (player instanceof ServerPlayer sp) {
               BECriteria.PORTAL_ON.trigger(sp);
            }
         }
      }
   }

   public void updateActiveStateOnPedestals() {
      if (this.world != null) {
         updateActiveStateOnPedestals(this.center, this.axis, this.active, this.willActivate, this.world, this);
         DataExchangeAPI.send(new RitualUpdate(this));
      }
   }

   public static void updateActiveStateOnPedestals(BlockPos center, Axis axis, boolean active, boolean willActivate, Level world, EternalRitual fallback) {
      updateActiveStateOnPedestals(center, axis, active, willActivate, world, fallback, null, -1);
   }

   public static void updateActiveStateOnPedestals(
      BlockPos center, Axis axis, boolean active, boolean willActivate, Level world, EternalRitual fallback, Item clientKeyItem, int pedestalItemMask
   ) {
      Direction moveX;
      Direction moveY;
      if (Axis.X == axis) {
         moveX = Direction.EAST;
         moveY = Direction.NORTH;
      } else {
         moveX = Direction.SOUTH;
         moveY = Direction.EAST;
      }

      for (int pedestalIndex = 0; pedestalIndex < PEDESTAL_POSITIONS.size(); pedestalIndex++) {
         Point pos = PEDESTAL_POSITIONS.get(pedestalIndex);
         MutableBlockPos checkPos = center.mutable();
         checkPos.move(moveX, pos.x).move(moveY, pos.y);
         if (world.getBlockEntity(checkPos) instanceof EternalPedestalEntity pedestal) {
            if (world.isClientSide() && pedestalItemMask >= 0) {
               boolean hasItem = (pedestalItemMask & 1 << pedestalIndex) != 0;
               pedestal.setClientItem(hasItem && clientKeyItem != null ? new ItemStack(clientKeyItem) : ItemStack.EMPTY);
               BlockState state = world.getBlockState(checkPos);
               if (state.is(PEDESTAL)) {
                  state = (BlockState)state.setValue(PedestalBlock.HAS_ITEM, hasItem);
                  if (!hasItem) {
                     state = (BlockState)((BlockState)state.setValue(PedestalBlock.HAS_LIGHT, false)).setValue(EternalPedestal.ACTIVATED, false);
                  } else if (active || willActivate) {
                     state = (BlockState)((BlockState)state.setValue(PedestalBlock.HAS_LIGHT, true)).setValue(EternalPedestal.ACTIVATED, true);
                  }

                  world.setBlockAndUpdate(checkPos, state);
               }
            }

            if (fallback == null) {
               fallback = pedestal.hasRitual() ? pedestal.getRitual() : new EternalRitual(world);
            }

            fallback.world = world;
            fallback.center = center;
            fallback.axis = axis;
            fallback.active = active;
            fallback.willActivate = willActivate;
            pedestal.linkRitual(fallback);
         }
      }
   }

   @Nullable
   public Item getKeyItem() {
      if (this.world != null && this.center != null && this.axis != null) {
         Direction moveX = this.axis == Axis.X ? Direction.EAST : Direction.SOUTH;
         Direction moveY = this.axis == Axis.X ? Direction.NORTH : Direction.EAST;

         for (Point pos : PEDESTAL_POSITIONS) {
            BlockPos checkPos = this.center.relative(moveX, pos.x).relative(moveY, pos.y);
            if (this.world.getBlockEntity(checkPos) instanceof EternalPedestalEntity pedestal && !pedestal.isEmpty()) {
               return pedestal.getItem(0).getItem();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean checkFrame(Level world, BlockPos framePos) {
      Direction moveDir = Axis.X == this.axis ? Direction.NORTH : Direction.EAST;
      boolean valid = true;

      for (Point point : PortalBuilder.FRAME_POSITIONS) {
         BlockPos pos = framePos.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         BlockState state = world.getBlockState(pos);
         valid &= state.getBlock() instanceof RunedFlavolite;
         pos = framePos.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         state = world.getBlockState(pos);
         valid &= state.getBlock() instanceof RunedFlavolite;
      }

      return valid;
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean willActivate() {
      return this.willActivate;
   }

   private void activatePortal(Player player, Item keyItem) {
      if (!this.active && !this.willActivate) {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(keyItem);
         int portalId = EndPortals.getPortalIdByItem(itemId);
         Level targetWorld = this.getTargetWorld(portalId);
         this.pendingKeyItemId = itemId;
         this.pendingStartPosition = PortalBuilder.getStartingPos(this.world, targetWorld, player, targetWorld.getWorldBorder());
         this.willActivate = true;
         this.updateActiveStateOnPedestals();
         this.world.scheduleTick(this.getActivationPedestalPos(), PEDESTAL, 60);
      }
   }

   public void finishActivation() {
      if (this.willActivate && !this.active && !this.isInvalid() && this.pendingKeyItemId != null && this.pendingStartPosition != null) {
         Item keyItem = this.getKeyItem();
         if (keyItem != null && this.pendingKeyItemId.equals(BuiltInRegistries.ITEM.getKey(keyItem)) && this.hasValidStructureFor(keyItem)) {
            int portalId = EndPortals.getPortalIdByItem(this.pendingKeyItemId);
            Level targetWorld = this.getTargetWorld(portalId);
            ResourceLocation worldId = targetWorld.dimension().location();

            try {
               if (this.exit == null) {
                  this.initPortal(this.pendingStartPosition, worldId, portalId);
               } else {
                  if (!worldId.equals(this.targetWorldId)) {
                     this.initPortal(this.pendingStartPosition, worldId, portalId);
                  } else if (!this.checkFrame(targetWorld, this.exit.below())) {
                     Axis portalAxis = Axis.X == this.axis ? Axis.Z : Axis.X;
                     PortalBuilder.generatePortal(targetWorld, this.exit, portalAxis, portalId);
                  }

                  this.activatePortal(targetWorld, this.exit, portalId);
               }

               this.activatePortal(this.world, this.center, portalId);
               if (this.world instanceof ServerLevel serverLevel) {
                  this.doEffects(serverLevel, this.center);
               }

               this.willActivate = false;
               this.active = true;
               this.clearPendingActivation();
               this.registerLinks();
               this.updateActiveStateOnPedestals();
            } catch (Exception var7) {
               BetterEnd.LOGGER.error("Create End portals error.", var7);
               this.removePortal(targetWorld, this.exit);
               this.removePortal(this.world, this.center);
               this.willActivate = false;
               this.active = false;
               this.clearPendingActivation();
               this.updateActiveStateOnPedestals();
            }
         } else {
            this.cancelActivation();
         }
      }
   }

   public void cancelActivation() {
      if (this.willActivate) {
         this.willActivate = false;
         this.clearPendingActivation();
         this.updateActiveStateOnPedestals();
      }
   }

   private void clearPendingActivation() {
      this.pendingKeyItemId = null;
      this.pendingStartPosition = null;
   }

   private BlockPos getActivationPedestalPos() {
      Direction moveX = this.axis == Axis.X ? Direction.EAST : Direction.SOUTH;
      Direction moveY = this.axis == Axis.X ? Direction.NORTH : Direction.EAST;
      Point offset = PEDESTAL_POSITIONS.get(0);
      return this.center.relative(moveX, offset.x).relative(moveY, offset.y);
   }

   private boolean hasValidStructureFor(Item keyItem) {
      if (!this.checkFrame(this.world, this.center.below())) {
         return false;
      } else {
         Direction moveX = this.axis == Axis.X ? Direction.EAST : Direction.SOUTH;
         Direction moveY = this.axis == Axis.X ? Direction.NORTH : Direction.EAST;

         for (Point offset : PEDESTAL_POSITIONS) {
            BlockPos pedestalPos = this.center.relative(moveX, offset.x).relative(moveY, offset.y);
            if (!this.isActive(pedestalPos)
               || !(this.world.getBlockEntity(pedestalPos) instanceof EternalPedestalEntity pedestal)
               || pedestal.isEmpty()
               || pedestal.getItem(0).getItem() != keyItem) {
               return false;
            }
         }

         return true;
      }
   }

   private void initPortal(BlockPos startPosition, ResourceLocation worldId, int portalId) {
      this.targetWorldId = worldId;
      if (this.world instanceof ServerLevel sourceWorld) {
         ServerLevel targetLevel = (ServerLevel)this.getTargetWorld(portalId);
         PortalBuilder builder = new PortalBuilder(this.world, targetLevel);
         WorldBorder worldBorder = targetLevel.getWorldBorder();
         Optional<FoundRectangle> foundRectangle = builder.findPortalAround(startPosition, worldBorder);
         if (foundRectangle.isPresent()) {
            this.exit = builder.getPortalCenter(foundRectangle.get());
         } else {
            Optional<BlockPos> centerPos = builder.createPortal(startPosition, this.axis, portalId);
            centerPos.ifPresent(blockPos -> this.exit = blockPos);
         }
      }
   }

   private void doEffects(ServerLevel serverWorld, BlockPos center) {
      Direction moveX;
      Direction moveY;
      if (Axis.X == this.axis) {
         moveX = Direction.EAST;
         moveY = Direction.NORTH;
      } else {
         moveX = Direction.SOUTH;
         moveY = Direction.EAST;
      }

      for (Point pos : PEDESTAL_POSITIONS) {
         MutableBlockPos p = center.mutable();
         p.move(moveX, pos.x).move(moveY, pos.y);
         serverWorld.sendParticles(ParticleTypes.PORTAL, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 20, 0.0, 0.0, 0.0, 1.0);
         serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 20, 0.0, 0.0, 0.0, 0.3);
      }

      serverWorld.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.NEUTRAL, 16.0F, 1.0F);
   }

   private void activatePortal(Level world, BlockPos center, int portalId) {
      BlockPos framePos = center.below();
      Direction moveDir = Axis.X == this.axis ? Direction.NORTH : Direction.EAST;
      BlockState frame = (BlockState)PortalBuilder.FRAME.defaultBlockState().setValue(ACTIVE, true);
      PortalBuilder.FRAME_POSITIONS.forEach(point -> {
         BlockPos pos = framePos.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         BlockState state = world.getBlockState(pos);
         if (state.hasProperty(ACTIVE) && !(Boolean)state.getValue(ACTIVE)) {
            world.setBlockAndUpdate(pos, frame);
         }

         pos = framePos.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         state = world.getBlockState(pos);
         if (state.hasProperty(ACTIVE) && !(Boolean)state.getValue(ACTIVE)) {
            world.setBlockAndUpdate(pos, frame);
         }
      });
      Axis portalAxis = Axis.X == this.axis ? Axis.Z : Axis.X;
      BlockState portal = (BlockState)((BlockState)PortalBuilder.PORTAL.defaultBlockState().setValue(EndPortalBlock.AXIS, portalAxis))
         .setValue(EndPortalBlock.PORTAL, portalId);
      ParticleOptions effect = new BlockParticleOption(ParticleTypes.BLOCK, portal);
      if (world instanceof ServerLevel serverWorld) {
         PortalBuilder.PORTAL_POSITIONS.forEach(point -> {
            BlockPos pos = center.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
            if (!world.getBlockState(pos).is(PortalBuilder.PORTAL)) {
               world.setBlockAndUpdate(pos, portal);
               serverWorld.sendParticles(effect, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
               serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.3);
            }

            pos = center.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
            if (!world.getBlockState(pos).is(PortalBuilder.PORTAL)) {
               world.setBlockAndUpdate(pos, portal);
               serverWorld.sendParticles(effect, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
               serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.3);
            }
         });
      }
   }

   public void disablePortal(int state) {
      if (this.active && !this.isInvalid()) {
         if (!this.world.isClientSide()) {
            Level targetWorld = this.getTargetWorld(state);
            if (this.world instanceof ServerLevel sourceLevel) {
               EternalPortalLinks.unlink(sourceLevel, this.center, targetWorld instanceof ServerLevel targetLevel ? targetLevel : null, this.exit);
            }

            this.removePortal(targetWorld, this.exit);
         }

         this.removePortal(this.world, this.center);
      }
   }

   private void removePortal(Level world, BlockPos center) {
      BlockPos framePos = center.below();
      Direction moveDir = Axis.X == this.axis ? Direction.NORTH : Direction.EAST;
      PortalBuilder.FRAME_POSITIONS.forEach(point -> {
         BlockPos pos = framePos.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         BlockState state = world.getBlockState(pos);
         if (state.is(PortalBuilder.FRAME) && (Boolean)state.getValue(ACTIVE)) {
            world.setBlockAndUpdate(pos, (BlockState)state.setValue(ACTIVE, false));
         }

         pos = framePos.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         state = world.getBlockState(pos);
         if (state.is(PortalBuilder.FRAME) && (Boolean)state.getValue(ACTIVE)) {
            world.setBlockAndUpdate(pos, (BlockState)state.setValue(ACTIVE, false));
         }
      });
      PortalBuilder.PORTAL_POSITIONS.forEach(point -> {
         BlockPos pos = center.mutable().move(moveDir, point.x).move(Direction.UP, point.y);
         if (world.getBlockState(pos).is(PortalBuilder.PORTAL)) {
            world.removeBlock(pos, false);
         }

         pos = center.mutable().move(moveDir, -point.x).move(Direction.UP, point.y);
         if (world.getBlockState(pos).is(PortalBuilder.PORTAL)) {
            world.removeBlock(pos, false);
         }
      });
      this.active = false;
      this.updateActiveStateOnPedestals();
   }

   @Nullable
   public EternalPortalLinks.Link registerLinks() {
      if (this.active && this.center != null && this.exit != null && this.targetWorldId != null) {
         if (this.world instanceof ServerLevel sourceLevel) {
            ServerLevel targetLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.targetWorldId));
            if (targetLevel != null && targetLevel != sourceLevel) {
               EternalPortalLinks.link(sourceLevel, this.center, targetLevel, this.exit);
               return new EternalPortalLinks.Link(targetLevel.dimension(), this.exit);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private Level getTargetWorld(int state) {
      return this.world.dimension() == Level.END
         ? EndPortals.getWorld(this.world.getServer(), state)
         : Objects.requireNonNull(this.world.getServer()).getLevel(Level.END);
   }

   public void configure(BlockPos initial) {
      BlockPos checkPos = initial.east(12);
      if (this.hasPedestal(checkPos)) {
         this.axis = Axis.X;
         this.center = initial.east(6);
      } else {
         checkPos = initial.west(12);
         if (this.hasPedestal(checkPos)) {
            this.axis = Axis.X;
            this.center = initial.west(6);
         } else {
            checkPos = initial.south(12);
            if (this.hasPedestal(checkPos)) {
               this.axis = Axis.Z;
               this.center = initial.south(6);
            } else {
               checkPos = initial.north(12);
               if (this.hasPedestal(checkPos)) {
                  this.axis = Axis.Z;
                  this.center = initial.north(6);
               } else {
                  checkPos = initial.north(10);
                  if (this.hasPedestal(checkPos)) {
                     this.axis = Axis.X;
                     checkPos = checkPos.east(8);
                     if (this.hasPedestal(checkPos)) {
                        this.center = initial.north(5).east(4);
                     } else {
                        this.center = initial.north(5).west(4);
                     }
                  } else {
                     checkPos = initial.south(10);
                     if (this.hasPedestal(checkPos)) {
                        this.axis = Axis.X;
                        checkPos = checkPos.east(8);
                        if (this.hasPedestal(checkPos)) {
                           this.center = initial.south(5).east(4);
                        } else {
                           this.center = initial.south(5).west(4);
                        }
                     } else {
                        checkPos = initial.east(10);
                        if (this.hasPedestal(checkPos)) {
                           this.axis = Axis.Z;
                           checkPos = checkPos.south(8);
                           if (this.hasPedestal(checkPos)) {
                              this.center = initial.east(5).south(4);
                           } else {
                              this.center = initial.east(5).north(4);
                           }
                        } else {
                           checkPos = initial.west(10);
                           if (this.hasPedestal(checkPos)) {
                              this.axis = Axis.Z;
                              checkPos = checkPos.south(8);
                              if (this.hasPedestal(checkPos)) {
                                 this.center = initial.west(5).south(4);
                              } else {
                                 this.center = initial.west(5).north(4);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean hasPedestal(BlockPos pos) {
      return this.world.getBlockState(pos).is(PEDESTAL);
   }

   private boolean isActive(BlockPos pos) {
      BlockState state = this.world.getBlockState(pos);
      if (state.is(PEDESTAL)) {
         EternalPedestalEntity pedestal = (EternalPedestalEntity)this.world.getBlockEntity(pos);
         if (pedestal != null) {
            if (!pedestal.hasRitual()) {
               pedestal.linkRitual(this);
            } else {
               EternalRitual ritual = pedestal.getRitual();
               if (!ritual.equals(this)) {
                  pedestal.linkRitual(this);
               }
            }
         }

         return (Boolean)state.getValue(ACTIVE);
      } else {
         return false;
      }
   }

   public CompoundTag toTag(CompoundTag tag) {
      if (this.center != null && this.axis != null) {
         tag.put("center", NbtUtils.writeBlockPos(this.center));
         tag.putString("axis", this.axis.getName());
         tag.putBoolean("active", this.active);
         tag.putBoolean("will_activate", this.willActivate);
         if (this.pendingKeyItemId != null) {
            tag.putString("pending_key_item", this.pendingKeyItemId.toString());
         }

         if (this.pendingStartPosition != null) {
            tag.put("pending_start", NbtUtils.writeBlockPos(this.pendingStartPosition));
         }

         if (this.targetWorldId != null) {
            tag.putString("key_item", this.targetWorldId.toString());
         }

         if (this.exit != null) {
            tag.put("exit", NbtUtils.writeBlockPos(this.exit));
         }

         return tag;
      } else {
         return null;
      }
   }

   public void fromTag(CompoundTag tag) {
      this.axis = Axis.byName(tag.getString("axis"));
      this.center = NbtUtils.readBlockPos(tag.getCompound("center"));
      this.active = tag.getBoolean("active");
      this.willActivate = tag.getBoolean("will_activate");
      if (tag.contains("pending_key_item")) {
         this.pendingKeyItemId = new ResourceLocation(tag.getString("pending_key_item"));
      }

      if (tag.contains("pending_start")) {
         this.pendingStartPosition = NbtUtils.readBlockPos(tag.getCompound("pending_start"));
      }

      if (tag.contains("exit")) {
         this.exit = NbtUtils.readBlockPos(tag.getCompound("exit"));
      }

      if (tag.contains("key_item")) {
         this.targetWorldId = new ResourceLocation(tag.getString("key_item"));
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         EternalRitual ritual = (EternalRitual)o;
         return this.world.equals(ritual.world) && Objects.equals(this.center, ritual.center) && Objects.equals(this.exit, ritual.exit);
      } else {
         return false;
      }
   }

   public static EternalRitual findRitualForActivePortal(Level world, BlockPos startPos) {
      BlockState state = world.getBlockState(startPos);
      if (!isActivePortalBlock(state)) {
         return null;
      } else {
         Direction direction = state.getValue(NetherPortalBlock.AXIS) == Axis.X ? Direction.EAST : Direction.NORTH;

         while (world.getBlockState(startPos.below()).is(EndBlocks.END_PORTAL_BLOCK)) {
            startPos = startPos.below();
         }

         if (world.getBlockState(startPos.relative(direction, -1)).is(EndBlocks.FLAVOLITE_RUNED_ETERNAL)) {
            startPos = startPos.relative(direction, 1);
         } else if (world.getBlockState(startPos.relative(direction, 1)).is(EndBlocks.FLAVOLITE_RUNED_ETERNAL)) {
            startPos = startPos.relative(direction, -1);
         }

         if (!world.getBlockState(startPos.below()).is(EndBlocks.FLAVOLITE_RUNED_ETERNAL)) {
            startPos = startPos.below();
         }

         if (world.getBlockState(startPos.relative(direction, -1)).is(EndBlocks.FLAVOLITE_RUNED_ETERNAL)) {
            startPos = startPos.relative(direction, 1);
         } else if (world.getBlockState(startPos.relative(direction, 1)).is(EndBlocks.FLAVOLITE_RUNED_ETERNAL)) {
            startPos = startPos.relative(direction, -1);
         }

         startPos = startPos.relative(direction.getClockWise(), 6);
         state = world.getBlockState(startPos);
         return state.is(EndBlocks.ETERNAL_PEDESTAL) && world.getBlockEntity(startPos) instanceof EternalPedestalEntity pedestal ? pedestal.getRitual() : null;
      }
   }

   private static boolean isActivePortalBlock(BlockState state) {
      return state.is(EndBlocks.END_PORTAL_BLOCK) && state.hasProperty(NetherPortalBlock.AXIS);
   }
}
