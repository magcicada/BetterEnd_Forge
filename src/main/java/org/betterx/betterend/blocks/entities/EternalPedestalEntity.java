package org.betterx.betterend.blocks.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.betterx.betterend.registry.EndBlockEntities;
import org.betterx.betterend.rituals.EternalRitual;

public class EternalPedestalEntity extends PedestalBlockEntity {
   private EternalRitual linkedRitual;

   public EternalPedestalEntity(BlockPos blockPos, BlockState blockState) {
      super(EndBlockEntities.ETERNAL_PEDESTAL, blockPos, blockState);
   }

   public boolean hasRitual() {
      return this.linkedRitual != null;
   }

   public void linkRitual(EternalRitual ritual) {
      this.linkedRitual = ritual;
   }

   public EternalRitual getRitual() {
      return this.linkedRitual;
   }

   public void setLevel(Level level) {
      super.setLevel(level);
      if (this.hasRitual()) {
         this.linkedRitual.setWorld(level);
         if (level instanceof ServerLevel serverLevel && serverLevel.getServer().isSameThread()) {
            this.linkedRitual.registerLinks();
         }
      }
   }

   @Override
   public void saveAdditional(CompoundTag tag) {
      if (this.hasRitual()) {
         CompoundTag ritualTag = this.linkedRitual.toTag(new CompoundTag());
         if (ritualTag != null) {
            tag.put("ritual", ritualTag);
         }
      }

      super.saveAdditional(tag);
   }

   @Override
   protected void fromTag(CompoundTag tag) {
      super.fromTag(tag);
      if (tag.contains("ritual")) {
         this.linkedRitual = new EternalRitual(this.level);
         this.linkedRitual.fromTag(tag.getCompound("ritual"));
      }
   }
}
