package org.betterx.betterend.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.betterx.bclib.client.render.HumanoidArmorRenderer.CopyExtraState;
import org.betterx.bclib.items.BaseArmorItem;
import org.betterx.betterend.effects.EndStatusEffects;
import org.betterx.betterend.item.material.EndArmorMaterial;
import org.betterx.betterend.item.model.CrystaliteArmorRenderer;

public class CrystaliteArmor extends BaseArmorItem {
   public static final MutableComponent CHEST_DESC = Component.translatable("tooltip.armor.crystalite_chest");
   public static final MutableComponent BOOTS_DESC = Component.translatable("tooltip.armor.crystalite_boots");
   private boolean clientInitDone;
   private static final EquipmentSlot[] SET_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   public CrystaliteArmor(Type type, Properties settings) {
      super(EndArmorMaterial.CRYSTALITE, type, settings);
   }

   public static boolean hasFullSet(LivingEntity owner) {
      for (EquipmentSlot slot : SET_SLOTS) {
         ItemStack armorStack = owner.getItemBySlot(slot);
         if (!(armorStack.getItem() instanceof CrystaliteArmor)) {
            return false;
         }
      }

      return true;
   }

   public static void applySetEffect(LivingEntity owner) {
      if ((owner.tickCount & 63) == 0) {
         owner.addEffect(new MobEffectInstance(EndStatusEffects.CRYSTALITE_HEALTH_REGEN));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      if (!this.clientInitDone) {
         this.clientInitDone = true;
         consumer.accept(new IClientItemExtensions() {
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
               CrystaliteArmorRenderer renderer = CrystaliteArmorRenderer.getInstance();
               HumanoidModel<LivingEntity> model = renderer.modelFor(livingEntity, slot);
               if (model == null) {
                  return original;
               } else {
                  @SuppressWarnings("unchecked")
                  HumanoidModel<LivingEntity> originalTyped = (HumanoidModel<LivingEntity>) original;
                  originalTyped.copyPropertiesTo(model);
                  if (model instanceof CopyExtraState copy) {
                     copy.copyPropertiesFrom(originalTyped);
                  }

                  return model;
               }
            }
         });
      }
   }

   static {
      Style descStyle = Style.EMPTY.applyFormats(new ChatFormatting[]{ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC});
      CHEST_DESC.setStyle(descStyle);
      BOOTS_DESC.setStyle(descStyle);
   }
}
