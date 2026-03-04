package org.betterx.betterend.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.world.item.crafting.Ingredient;
import org.betterx.betterend.BetterEnd;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JEIInfusionCategory implements IRecipeCategory<InfusionDisplay> {
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(BetterEnd.MOD_ID, "textures/gui/infusion_16x16_pixel_optimized.png");
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(BetterEnd.MOD_ID, "textures/gui/jei_infusion.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public JEIInfusionCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 0, 0, 150, 104);
        this.icon = guiHelper.drawableBuilder(ICON_TEXTURE, 0, 0, 16, 16)
                .setTextureSize(16, 16)
                .build();
        this.title = Component.translatable("betterend.jei.container.infusion");
    }

    @Override
    public @NotNull RecipeType<InfusionDisplay> getRecipeType() {
        return JEIPlugin.INFUSION_RECIPE_TYPE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionDisplay display, IFocusGroup focuses) {
        List<Ingredient> inputs = display.recipe.getIngredients();

        final int cx = 33;
        final int cy = 38;

        builder.addSlot(RecipeIngredientRole.INPUT, cx + 8, cy + 12).addIngredients(inputs.get(0));
        if (inputs.size() > 1) builder.addSlot(RecipeIngredientRole.INPUT, cx + 8, cy - 16).addIngredients(inputs.get(1));
        if (inputs.size() > 2) builder.addSlot(RecipeIngredientRole.INPUT, cx + 32, cy - 12).addIngredients(inputs.get(2));
        if (inputs.size() > 3) builder.addSlot(RecipeIngredientRole.INPUT, cx + 36, cy + 12).addIngredients(inputs.get(3));
        if (inputs.size() > 4) builder.addSlot(RecipeIngredientRole.INPUT, cx + 32, cy + 36).addIngredients(inputs.get(4));
        if (inputs.size() > 5) builder.addSlot(RecipeIngredientRole.INPUT, cx + 8, cy + 40).addIngredients(inputs.get(5));
        if (inputs.size() > 6) builder.addSlot(RecipeIngredientRole.INPUT, cx - 16, cy + 36).addIngredients(inputs.get(6));
        if (inputs.size() > 7) builder.addSlot(RecipeIngredientRole.INPUT, cx - 20, cy + 12).addIngredients(inputs.get(7));
        if (inputs.size() > 8) builder.addSlot(RecipeIngredientRole.INPUT, cx - 16, cy - 12).addIngredients(inputs.get(8));

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, cx + 88, cy + 12)
                    .addItemStack(display.recipe.getResultItem(minecraft.level.registryAccess()));
        }
    }

    @Override
    public void draw(InfusionDisplay display, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Component timeText = Component.translatable("betterend.jei.infusion.time&val", display.time);
        guiGraphics.drawString(Minecraft.getInstance().font, timeText, 100, 92, 0xFF404040, false);
    }

    @Override
    public @NotNull ResourceLocation getRegistryName(InfusionDisplay recipe) {
        return recipe.recipe.getId();
    }
}
