package org.betterx.betterend.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndBlocks;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class JEIAlloyingCategory implements IRecipeCategory<AlloyingDisplay> {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(BetterEnd.MOD_ID, "textures/gui/smelter_gui.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final IDrawableAnimated fire;
    private final Component title;
    private static final DecimalFormat DF = new DecimalFormat("###.##");

    public JEIAlloyingCategory(IGuiHelper guiHelper) {
        // 背景切片：保持核心区域 (26, 12, 124, 62)
        this.background = guiHelper.createDrawable(GUI_TEXTURE, 26, 12, 124, 62);

        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(EndBlocks.END_STONE_SMELTER));
        this.title = Component.translatable("betterend.jei.container.alloying");

        // 动画贴图资产
        this.fire = guiHelper.drawableBuilder(GUI_TEXTURE, 177, 0, 14, 14)
                .buildAnimated(200, IDrawableAnimated.StartDirection.TOP, true);

        this.arrow = guiHelper.drawableBuilder(GUI_TEXTURE, 176, 15, 24, 17)
                .buildAnimated(200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public @NotNull RecipeType<AlloyingDisplay> getRecipeType() {
        return (RecipeType) JEIPlugin.ALLOYING_RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyingDisplay display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 5)
                .addIngredients(display.recipe.getIngredients().get(0));

        if (display.recipe.getIngredients().size() > 1) {
            builder.addSlot(RecipeIngredientRole.INPUT, 41, 5)
                    .addIngredients(display.recipe.getIngredients().get(1));
        }

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 30, 41)
                .addItemStacks(JEIPlugin.ALLOYING_FUELS);

        if (Minecraft.getInstance().level != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 103, 23)
                    .addItemStack(display.recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
        }
    }

    @Override
    public void draw(AlloyingDisplay display, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        fire.draw(guiGraphics, 32, 25);
        arrow.draw(guiGraphics, 64, 23);

        Component text = Component.translatable(
                "betterend.jei.alloying.experience_time",
                DF.format(display.experience),
                DF.format(display.time / 20.0D)
        );

        guiGraphics.drawString(Minecraft.getInstance().font, text, 55, 49, 0xFF404040, false);
    }

    @Override
    public @NotNull ResourceLocation getRegistryName(AlloyingDisplay recipe) {
        return recipe.recipe.getId();
    }
}
