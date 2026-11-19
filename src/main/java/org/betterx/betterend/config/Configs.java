package org.betterx.betterend.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.resources.ResourceLocation;
import org.betterx.bclib.BCLib;
import org.betterx.bclib.api.v3.levelgen.features.BCLFeature;
import org.betterx.bclib.api.v3.levelgen.features.FeatureConfigAPI;
import org.betterx.bclib.config.IgnoredConfigEntries;
import org.betterx.bclib.config.PathConfig;
import org.betterx.betterend.BetterEnd;

public class Configs {
   public static final PathConfig ENTITY_CONFIG = new PathConfig("betterend", "entities");
   public static final PathConfig BLOCK_CONFIG = new PathConfig("betterend", "blocks");
   public static final PathConfig GENERATOR_CONFIG = new PathConfig("betterend", "generator", false);
   public static final PathConfig STRUCTURE_CONFIG = new PathConfig("betterend", "structures");
   public static final PathConfig FEATURE_CONFIG = new PathConfig("betterend", "features");
   public static final PathConfig ENCHANTMENT_CONFIG = new PathConfig("betterend", "enchantments");
   public static final PathConfig CLENT_CONFIG = new PathConfig("betterend", "client", false);

   public static void saveConfigs() {
      ENTITY_CONFIG.saveChanges();
      BLOCK_CONFIG.saveChanges();
      GENERATOR_CONFIG.saveChanges();
      STRUCTURE_CONFIG.saveChanges();
      FEATURE_CONFIG.saveChanges();
      ENCHANTMENT_CONFIG.saveChanges();
      if (BCLib.isClient()) {
         CLENT_CONFIG.saveChanges();
      }
   }

   public static boolean isStructureEnabled(ResourceLocation id) {
      return !"betterend".equals(id.getNamespace()) || STRUCTURE_CONFIG.getBooleanRoot(id.getPath(), true);
   }

   public static boolean isFeatureEnabled(ResourceLocation id) {
      return !"betterend".equals(id.getNamespace()) || FEATURE_CONFIG.getBooleanRoot(id.getPath(), true);
   }

   public static void registerFeatureConfigEntries(Class<?>... sources) {
      for (Class<?> source : sources) {
         for (Field field : source.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && BCLFeature.class.isAssignableFrom(field.getType())) {
               registerFeatureConfigEntry(field);
            }
         }
      }

      FEATURE_CONFIG.saveChanges();
   }

   private static void registerFeatureConfigEntry(Field field) {
      try {
         field.setAccessible(true);
         if (field.get(null) instanceof BCLFeature<?, ?> feature) {
            FeatureConfigAPI.registerFeature(feature);
            feature.getPlacedFeature().unwrapKey().ifPresent(key -> isFeatureEnabled(key.location()));
         }
      } catch (IllegalAccessException var3) {
         BetterEnd.LOGGER.warning("Failed to register feature config entry for " + field.getName());
      }
   }

   static {
      IgnoredConfigEntries.removeRootSwitches("betterend", "blocks");
      IgnoredConfigEntries.warnIfDisabled("betterend", "items");
      IgnoredConfigEntries.warnIfDisabled("betterend", "biomes", "biomes and their caves");
      IgnoredConfigEntries.warnIfDisabled("betterend", "recipes", "recipes in this file");
   }
}
