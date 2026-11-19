package org.betterx.betterend.portal;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.advancements.BECriteria;
import org.betterx.betterend.blocks.EndPortalBlock;
import org.betterx.betterend.mixin.common.portal.EntityAccessor;
import org.betterx.betterend.mixin.common.portal.ServerPlayerAccessor;
import org.betterx.betterend.registry.EndPortals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class TravelerState {
   public final Entity entity;
   private BlockPos portalEntrancePos;
   private boolean isInsidePortal;
   private int portalTime;
   @Nullable
   private EternalPortalLinks.Link arrivedAt;
   @Nullable
   private EternalPortalLinks.Link cameFrom;
   @Nullable
   private EternalPortalLinks.Link pendingArrival;
   @Nullable
   private EternalPortalLinks.Link pendingDeparture;
   private int lastPortalTick = Integer.MIN_VALUE;

   public TravelerState(Entity entity) {
      this.entity = entity;
      this.portalEntrancePos = null;
      this.isInsidePortal = false;
   }

   public static TravelerState init(Entity e) {
      return new TravelerState(e);
   }

   public void handleInsidePortal(BlockPos blockPos) {
      if (this.entity.isOnPortalCooldown()) {
         this.entity.setPortalCooldown();
      } else {
         if (!this.level().isClientSide && !blockPos.equals(this.portalEntrancePos)) {
            this.portalEntrancePos = blockPos.immutable();
         }

         if (this.level().isClientSide && this.entity instanceof LocalPlayer) {
            ((EntityAccessor)this.entity).be_setInsidePortal(true);
         }

         this.isInsidePortal = true;
      }
   }

   public Level level() {
      return this.entity.level();
   }

   @Internal
   public void portalTick() {
      if (this.level() instanceof ServerLevel) {
         if (this.entity.tickCount != this.lastPortalTick) {
            this.lastPortalTick = this.entity.tickCount;
            if (this.isInsidePortal) {
               int waitTimer = this.entity.getPortalWaitTime();
               ServerLevel sourceDimension = (ServerLevel)this.level();
               ServerLevel targetDimension = this.getTargetDimension(sourceDimension);
               if (targetDimension != null && targetDimension != sourceDimension && !this.entity.isPassenger() && this.portalTime++ >= waitTimer) {
                  this.level().getProfiler().push("end_portal");
                  this.portalTime = waitTimer;
                  this.entity.setPortalCooldown();
                  this.changeDimension(targetDimension);
                  this.level().getProfiler().pop();
               }

               this.isInsidePortal = false;
            } else {
               if (this.portalTime > 0) {
                  this.portalTime -= 4;
               }

               if (this.portalTime < 0) {
                  this.portalTime = 0;
               }
            }
         }
      }
   }

   @Nullable
   private ServerLevel getTargetDimension(ServerLevel sourceDimension) {
      MinecraftServer server = sourceDimension.getServer();
      if (sourceDimension.dimension() != Level.END) {
         return server.getLevel(Level.END);
      } else {
         int portalId = -1;
         if (this.portalEntrancePos != null) {
            BlockState state = sourceDimension.getBlockState(this.portalEntrancePos);
            if (state.hasProperty(EndPortalBlock.PORTAL)) {
               portalId = (Integer)state.getValue(EndPortalBlock.PORTAL);
            }
         }

         return EndPortals.getWorld(server, portalId);
      }
   }

   public void changeDimension(ServerLevel targetLevel) {
      if (this.level() instanceof ServerLevel && !this.entity.isRemoved()) {
         this.level().getProfiler().push("be_findEntry");
         this.entity.unRide();
         PortalInfo portalInfo = this.findDimensionEntryPoint(targetLevel);
         if (portalInfo != null) {
            this.level().getProfiler().push("be_reposition");
            this.teleportEntity(targetLevel, portalInfo);
            this.level().getProfiler().pop();
            this.level().getProfiler().pop();
         }
      }
   }

   private void teleportEntity(ServerLevel serverLevel, PortalInfo portalInfo) {
      EternalPortalLinks.Link arrival = this.pendingArrival;
      EternalPortalLinks.Link departure = this.pendingDeparture;
      this.pendingArrival = null;
      this.pendingDeparture = null;
      Entity arrived = this.entity;
      if (this.entity instanceof ServerPlayer sp && sp.isCreative()) {
         sp.teleportTo(
            serverLevel,
            portalInfo.pos.x + 0.5,
            portalInfo.pos.y,
            portalInfo.pos.z + 0.5,
            this.entity.getYRot() + 180.0F,
            this.entity.getXRot()
         );
         BECriteria.PORTAL_TRAVEL.trigger(sp);
      } else {
         if (this.entity instanceof ServerPlayer sp) {
            BECriteria.PORTAL_TRAVEL.trigger(sp);
            travelToDimension(sp, serverLevel, portalInfo);
         } else {
            arrived = sendToDimension(this.entity, serverLevel, portalInfo);
         }

         this.entity.setPortalCooldown();
      }

      if (arrived instanceof TravelingEntity te && te.be_getTravelerState() != null) {
         te.be_getTravelerState().arrivedAt = arrival;
         te.be_getTravelerState().cameFrom = departure;
      }
   }

   public static Entity sendToDimension(Entity traveler, ServerLevel serverLevel, @NotNull PortalInfo portalInfo) {
      if (traveler.level() instanceof ServerLevel sourceDimension && !traveler.isRemoved()) {
         traveler.unRide();
         Entity copy = traveler.getType().create(serverLevel);
         if (copy != null) {
            copy.restoreFrom(traveler);
            copy.moveTo(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, copy.getXRot());
            copy.setDeltaMovement(portalInfo.speed);
            serverLevel.addDuringTeleport(copy);
         }

         ((EntityAccessor)traveler).be_removeAfterChangingDimensions();
         sourceDimension.resetEmptyTime();
         serverLevel.resetEmptyTime();
         return copy;
      } else {
         return null;
      }
   }

   public static Entity travelToDimension(ServerPlayer traveler, ServerLevel serverLevel, @NotNull PortalInfo portalInfo) {
      LevelData levelData = serverLevel.getLevelData();
      ServerLevel sourceDimension = traveler.serverLevel();
      PlayerList playerList = traveler.server.getPlayerList();
      ((ServerPlayerAccessor)traveler).be_setChangingDimension(true);
      traveler.unRide();
      traveler.connection
         .send(
            new ClientboundRespawnPacket(
               serverLevel.dimensionTypeId(),
               serverLevel.dimension(),
               BiomeManager.obfuscateSeed(serverLevel.getSeed()),
               traveler.gameMode.getGameModeForPlayer(),
               traveler.gameMode.getPreviousGameModeForPlayer(),
               serverLevel.isDebug(),
               serverLevel.isFlat(),
               (byte)3,
               traveler.getLastDeathLocation(),
               traveler.getPortalCooldown()
            )
         );
      traveler.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
      playerList.sendPlayerPermissionLevel(traveler);
      sourceDimension.removePlayerImmediately(traveler, RemovalReason.CHANGED_DIMENSION);
      ((EntityAccessor)traveler).be_unsetRemoved();
      traveler.setServerLevel(serverLevel);
      traveler.connection.resetPosition();
      serverLevel.addDuringPortalTeleport(traveler);
      ((ServerPlayerAccessor)traveler).be_triggerDimensionChangeTriggers(sourceDimension);
      traveler.connection
         .teleport(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, portalInfo.xRot);
      playerList.sendLevelInfo(traveler, serverLevel);
      playerList.sendAllPlayerInfo(traveler);
      traveler.connection.send(new ClientboundPlayerAbilitiesPacket(traveler.getAbilities()));
      sendPlayerEffects(traveler);
      traveler.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
      ((ServerPlayerAccessor)traveler).be_setLastSentExp(-1);
      ((ServerPlayerAccessor)traveler).be_setLastSentHealth(-1.0F);
      ((ServerPlayerAccessor)traveler).be_setLastSentFood(-1);
      sourceDimension.resetEmptyTime();
      serverLevel.resetEmptyTime();
      return traveler;
   }

   private static void sendPlayerEffects(ServerPlayer traveler) {
      for (MobEffectInstance mobEffectInstance : traveler.getActiveEffects()) {
         traveler.connection.send(new ClientboundUpdateMobEffectPacket(traveler.getId(), mobEffectInstance));
      }
   }

   @Nullable
   private PortalInfo findDimensionEntryPoint(ServerLevel targetLevel) {
      boolean toEnd = targetLevel.dimension() == Level.END;
      if (this.level().dimension() != Level.END && !toEnd) {
         return null;
      } else if (this.portalEntrancePos == null) {
         return null;
      } else {
         WorldBorder worldBorder = targetLevel.getWorldBorder();
         BlockPos startingPos = PortalBuilder.getStartingPos(this.level(), targetLevel, this.entity, worldBorder);
         BlockPos entranceCenter = this.getEntranceCenter();
         return this.findLinkedPortal(targetLevel, entranceCenter)
            .or(() -> this.getExitPortal(targetLevel, startingPos, toEnd, worldBorder))
            .map(
               foundRectangle -> {
                  this.pendingArrival = new EternalPortalLinks.Link(targetLevel.dimension(), PortalBuilder.getPortalCenter(targetLevel, foundRectangle));
                  this.pendingDeparture = entranceCenter == null ? null : new EternalPortalLinks.Link(this.level().dimension(), entranceCenter);
                  BlockState blockState = this.level().getBlockState(this.portalEntrancePos);
                  Vec3 vec3;
                  Axis axis;
                  if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                     axis = (Axis)blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                     FoundRectangle foundRectangle2 = BlockUtil.getLargestRectangleAround(
                        this.portalEntrancePos, axis, 21, Axis.Y, 21, blockPos -> this.level().getBlockState(blockPos) == blockState
                     );
                     vec3 = this.getRelativePortalPosition(axis, foundRectangle2);
                  } else {
                     axis = Axis.X;
                     vec3 = new Vec3(0.5, 0.0, 0.0);
                  }

                  return PortalShape.createPortalInfo(
                     targetLevel, foundRectangle, axis, vec3, this.entity, this.entity.getDeltaMovement(), this.entity.getYRot(), this.entity.getXRot()
                  );
               }
            )
            .orElse(null);
      }
   }

   private Optional<FoundRectangle> findLinkedPortal(ServerLevel targetLevel, @Nullable BlockPos sourceCenter) {
      if (sourceCenter != null && this.level() instanceof ServerLevel sourceLevel) {
         if (this.cameFrom != null && this.arrivedAt != null && this.arrivedAt.equals(new EternalPortalLinks.Link(sourceLevel.dimension(), sourceCenter))) {
            Optional<FoundRectangle> back = this.getLinkedPortal(targetLevel, sourceCenter, this.cameFrom);
            if (back.isPresent()) {
               return back;
            }
         }

         return this.getLinkedPortal(targetLevel, sourceCenter, EternalPortalLinks.get(sourceLevel).get(sourceCenter));
      } else {
         return Optional.empty();
      }
   }

   private Optional<FoundRectangle> getLinkedPortal(ServerLevel targetLevel, BlockPos sourceCenter, @Nullable EternalPortalLinks.Link link) {
      if (link != null && link.dimension().equals(targetLevel.dimension())) {
         BlockPos targetCenter = link.center();
         targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(targetCenter), 3, targetCenter);
         if (!targetLevel.getBlockState(targetCenter).is(PortalBuilder.PORTAL)) {
            BetterEnd.LOGGER
               .warning(
                  "The Eternal Portal at {} linked to {} in {} no longer exists, searching for another one",
                  sourceCenter,
                  targetCenter,
                  targetLevel.dimension().location()
               );
            return Optional.empty();
         } else {
            return Optional.of(PortalBuilder.getPortalRectangle(targetLevel, targetCenter));
         }
      } else {
         return Optional.empty();
      }
   }

   @Nullable
   private BlockPos getEntranceCenter() {
      return !this.level().getBlockState(this.portalEntrancePos).is(PortalBuilder.PORTAL)
         ? null
         : PortalBuilder.getPortalCenter(this.level(), PortalBuilder.getPortalRectangle(this.level(), this.portalEntrancePos));
   }

   protected Optional<FoundRectangle> getExitPortal(ServerLevel targetLevel, BlockPos startingPos, boolean bl, WorldBorder worldBorder) {
      PortalBuilder builder = new PortalBuilder(this.level(), targetLevel);
      Optional<FoundRectangle> portalRectangle = builder.findPortalAround(startingPos, worldBorder);
      if (portalRectangle.isPresent()) {
         return portalRectangle;
      } else {
         BetterEnd.LOGGER.error("Unable to locate an active portal");
         return portalRectangle;
      }
   }

   protected Vec3 getRelativePortalPosition(Axis axis, FoundRectangle foundRectangle) {
      return PortalShape.getRelativePosition(foundRectangle, axis, this.entity.position(), this.entity.getDimensions(this.entity.getPose()));
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         TravelerState that = (TravelerState)obj;
         return Objects.equals(this.entity, that.entity)
            && Objects.equals(this.portalEntrancePos, that.portalEntrancePos)
            && this.isInsidePortal == that.isInsidePortal;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.entity, this.portalEntrancePos, this.isInsidePortal);
   }

   @Override
   public String toString() {
      return "TravelerState[entity=" + this.entity + ", portalEntrancePos=" + this.portalEntrancePos + ", isInsidePortal=" + this.isInsidePortal + "]";
   }
}
