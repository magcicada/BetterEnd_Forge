package org.betterx.betterend.portal;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class EternalPortalLinks extends SavedData {
   private static final String NAME = "betterend_eternal_portal_links";
   private final Map<BlockPos, EternalPortalLinks.Link> links = Maps.newHashMap();

   public static EternalPortalLinks get(ServerLevel level) {
      return (EternalPortalLinks)level.getDataStorage().computeIfAbsent(EternalPortalLinks::load, EternalPortalLinks::new, "betterend_eternal_portal_links");
   }

   @Nullable
   public EternalPortalLinks.Link get(BlockPos center) {
      return this.links.get(center);
   }

   public void put(BlockPos center, ResourceKey<Level> targetDimension, BlockPos targetCenter) {
      EternalPortalLinks.Link link = new EternalPortalLinks.Link(targetDimension, targetCenter.immutable());
      if (!link.equals(this.links.put(center.immutable(), link))) {
         this.setDirty();
      }
   }

   public void remove(BlockPos center) {
      if (this.links.remove(center) != null) {
         this.setDirty();
      }
   }

   public static void link(ServerLevel source, BlockPos sourceCenter, ServerLevel target, BlockPos exitCenter) {
      get(source).put(sourceCenter, target.dimension(), exitCenter);
      EternalPortalLinks targetLinks = get(target);
      if (targetLinks.get(exitCenter) == null) {
         targetLinks.put(exitCenter, source.dimension(), sourceCenter);
      }
   }

   public static void unlink(ServerLevel source, BlockPos sourceCenter, @Nullable ServerLevel target, @Nullable BlockPos exitCenter) {
      get(source).remove(sourceCenter);
      if (target != null && exitCenter != null) {
         EternalPortalLinks targetLinks = get(target);
         EternalPortalLinks.Link back = targetLinks.get(exitCenter);
         if (back != null && back.dimension().equals(source.dimension()) && back.center().equals(sourceCenter)) {
            targetLinks.remove(exitCenter);
         }
      }
   }

   public static EternalPortalLinks load(CompoundTag tag) {
      EternalPortalLinks result = new EternalPortalLinks();
      ListTag list = tag.getList("links", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag entry = list.getCompound(i);
         ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
         if (dimension != null) {
            result.links
               .put(
                  BlockPos.of(entry.getLong("center")),
                  new EternalPortalLinks.Link(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("target")))
               );
         }
      }

      return result;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.links.forEach((center, link) -> {
         CompoundTag entry = new CompoundTag();
         entry.putLong("center", center.asLong());
         entry.putString("dimension", link.dimension().location().toString());
         entry.putLong("target", link.center().asLong());
         list.add(entry);
      });
      tag.put("links", list);
      return tag;
   }

   public record Link(ResourceKey<Level> dimension, BlockPos center) {
   }
}
