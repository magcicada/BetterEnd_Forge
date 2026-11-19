package org.betterx.betterend.item;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.betterx.bclib.items.BaseArmorItem;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.interfaces.BetterEndElytra;
import org.betterx.betterend.interfaces.MultiModelItem;
import org.betterx.betterend.registry.EndItems;
import org.jetbrains.annotations.Nullable;

public class ArmoredElytra extends BaseArmorItem implements MultiModelItem, BetterEndElytra {
   private final ResourceLocation wingTexture;
   private final Item repairItem;
   private final double movementFactor;
   private final float toughness;
   private final int defense;

   public ArmoredElytra(String name, ArmorMaterial material, Item repairItem, int durability, double movementFactor, boolean fireproof) {
      this(name, material, repairItem, durability, movementFactor, 1.15, 1.15F, fireproof);
   }

   public ArmoredElytra(
      String name,
      ArmorMaterial material,
      Item repairItem,
      int durability,
      double movementFactor,
      double defenseDivider,
      float toughnessDivider,
      boolean fireproof
   ) {
      super(
         material,
         Type.CHESTPLATE,
         fireproof
            ? EndItems.makeEndItemSettings().durability(durability).rarity(Rarity.EPIC).fireResistant()
            : EndItems.makeEndItemSettings().durability(durability).rarity(Rarity.EPIC)
      );
      this.wingTexture = BetterEnd.makeID("textures/entity/" + name + ".png");
      this.repairItem = repairItem;
      this.movementFactor = movementFactor;
      this.defense = (int)(material.getDefenseForType(Type.CHESTPLATE) / defenseDivider);
      this.toughness = material.getToughness() / toughnessDivider;
      this.addAttributeModifier(Attributes.ARMOR, new AttributeModifier(ARMOR_MODIFIER_UUID_PER_SLOT[2], "Armor modifier", this.defense, Operation.ADDITION));
      this.addAttributeModifier(
         Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ARMOR_MODIFIER_UUID_PER_SLOT[2], "Armor toughness", this.toughness, Operation.ADDITION)
      );
   }

   @Override
   public double getMovementFactor() {
      return this.movementFactor;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public ResourceLocation getModelTexture() {
      return this.wingTexture;
   }

   public boolean isValidRepairItem(ItemStack itemStack, ItemStack itemStack2) {
      return super.isValidRepairItem(itemStack, itemStack2) || itemStack2.getItem() == this.repairItem;
   }

   public int getDefense() {
      return this.defense;
   }

   public float getToughness() {
      return this.toughness;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModelPredicate() {
      ItemProperties.register(this, new ResourceLocation("broken"), new ClampedItemPropertyFunction() {
         public float unclampedCall(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed) {
            return ElytraItem.isFlyEnabled(itemStack) ? 0.0F : 1.0F;
         }
      });
   }
}
