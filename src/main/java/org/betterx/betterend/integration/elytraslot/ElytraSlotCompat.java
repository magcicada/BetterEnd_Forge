package org.betterx.betterend.integration.elytraslot;

import org.betterx.bclib.items.elytra.BCLElytraItem;
import org.betterx.bclib.items.elytra.BCLElytraUtils;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.interfaces.BetterEndElytra;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class ElytraSlotCompat {
    private static final ResourceLocation VANILLA_WINGS = new ResourceLocation(
            "minecraft",
            "textures/entity/elytra.png"
    );
    private static boolean bclibSlotProviderInstalled;
    private static boolean curiosReflectionInitialized;
    private static boolean curiosReflectionReady;
    private static Method curiosGetCuriosHelper;
    private static Method curiosFindFirstCurio;
    private static Method slotResultStack;

    private ElytraSlotCompat() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("elytraslot")) {
            return;
        }

        try {
            registerProvider();
            installBCLibSlotProvider();
        } catch (Throwable throwable) {
            BetterEnd.LOGGER.warning("[elytraslot] failed to register BetterEnd provider: " + throwable.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerProvider() throws ReflectiveOperationException {
        Class<?> commonClass = Class.forName("com.illusivesoulworks.elytraslot.ElytraSlotCommonMod");
        Class<?> providerInterface = Class.forName("com.illusivesoulworks.elytraslot.common.IElytraProvider");
        Field providersField = commonClass.getDeclaredField("PROVIDERS");
        providersField.setAccessible(true);

        Object providersObject = providersField.get(null);
        if (!(providersObject instanceof List<?> providers)) {
            return;
        }

        for (Object provider : providers) {
            if (!Proxy.isProxyClass(provider.getClass())) {
                continue;
            }
            InvocationHandler handler = Proxy.getInvocationHandler(provider);
            if (handler instanceof BetterEndProviderHandler) {
                return;
            }
        }

        Object proxy = Proxy.newProxyInstance(
                providerInterface.getClassLoader(),
                new Class[]{providerInterface},
                new BetterEndProviderHandler()
        );
        ((List<Object>) providers).add(proxy);
    }

    private static boolean isBetterEndElytra(ItemStack stack) {
        return stack.getItem() instanceof BetterEndElytra;
    }

    private static boolean isElytra(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof ElytraItem || item instanceof BCLElytraItem;
    }

    private static ResourceLocation getTexture(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BetterEndElytra betterEndElytra) {
            return betterEndElytra.getModelTexture();
        }
        return VANILLA_WINGS;
    }

    private static void installBCLibSlotProvider() {
        if (bclibSlotProviderInstalled) {
            return;
        }

        BCLElytraUtils.SlotProvider previous = BCLElytraUtils.slotProvider;
        BCLElytraUtils.slotProvider = (entity, slotGetter) -> {
            ItemStack previousStack = previous == null ? null : previous.getElytra(entity, slotGetter);
            if (isElytra(previousStack)) {
                return previousStack;
            }

            ItemStack chestStack = slotGetter.apply(EquipmentSlot.CHEST);
            if (isElytra(chestStack)) {
                return chestStack;
            }

            ItemStack curiosStack = findCuriosElytra(entity);
            return curiosStack.isEmpty() ? previousStack : curiosStack;
        };

        bclibSlotProviderInstalled = true;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack findCuriosElytra(LivingEntity entity) {
        try {
            Object helper = getCuriosHelper();
            if (helper == null) {
                return ItemStack.EMPTY;
            }

            Object found = curiosFindFirstCurio.invoke(
                    helper,
                    entity,
                    (Predicate<ItemStack>) ElytraSlotCompat::isElytra
            );
            if (!(found instanceof Optional<?> optional) || optional.isEmpty()) {
                return ItemStack.EMPTY;
            }

            Object stack = slotResultStack.invoke(optional.get());
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static Object getCuriosHelper() throws ReflectiveOperationException {
        if (!ensureCuriosReflection()) {
            return null;
        }
        return curiosGetCuriosHelper.invoke(null);
    }

    private static boolean ensureCuriosReflection() {
        if (curiosReflectionInitialized) {
            return curiosReflectionReady;
        }
        curiosReflectionInitialized = true;

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Class<?> curiosHelper = Class.forName("top.theillusivec4.curios.api.type.util.ICuriosHelper");
            Class<?> slotResult = Class.forName("top.theillusivec4.curios.api.SlotResult");

            curiosGetCuriosHelper = curiosApi.getMethod("getCuriosHelper");
            curiosFindFirstCurio = curiosHelper.getMethod("findFirstCurio", LivingEntity.class, Predicate.class);
            slotResultStack = slotResult.getMethod("stack");
            curiosReflectionReady = true;
        } catch (ReflectiveOperationException exception) {
            curiosReflectionReady = false;
            BetterEnd.LOGGER.warning("[elytraslot] failed to init Curios bridge: " + exception.getMessage());
        }

        return curiosReflectionReady;
    }

    private static final class BetterEndProviderHandler implements InvocationHandler {
        private Constructor<?> colorCtor;
        private Constructor<?> renderCtor;

        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] args
        ) throws Throwable {
            String name = method.getName();
            if ("matches".equals(name)) {
                return args != null && args.length > 0 && args[0] instanceof ItemStack stack && isBetterEndElytra(stack);
            }
            if ("canFly".equals(name)) {
                return args != null && args.length > 1
                        && args[0] instanceof ItemStack stack
                        && args[1] instanceof LivingEntity livingEntity
                        && stack.canElytraFly(livingEntity);
            }
            if ("getRender".equals(name)) {
                if (args == null || args.length == 0 || !(args[0] instanceof ItemStack stack)) {
                    return null;
                }
                return buildRender(stack);
            }
            if ("hasCapeTexture".equals(name)) {
                return true;
            }
            if ("toString".equals(name)) {
                return "BetterEndElytraSlotProvider";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(this);
            }
            if ("equals".equals(name)) {
                return proxy == (args == null || args.length == 0 ? null : args[0]);
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == char.class) {
                return '\0';
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0.0F;
            }
            if (returnType == double.class) {
                return 0.0D;
            }
            return null;
        }

        private Object buildRender(ItemStack stack) throws ReflectiveOperationException {
            ensureRenderApi();
            Object color = colorCtor.newInstance(1.0F, 1.0F, 1.0F, 1.0F);
            return renderCtor.newInstance(color, getTexture(stack), stack.hasFoil(), stack, true);
        }

        private void ensureRenderApi() throws ReflectiveOperationException {
            if (colorCtor != null && renderCtor != null) {
                return;
            }

            Class<?> colorClass = Class.forName("com.illusivesoulworks.elytraslot.client.ElytraColor");
            Class<?> renderClass = Class.forName("com.illusivesoulworks.elytraslot.client.ElytraRenderResult");
            this.colorCtor = colorClass.getDeclaredConstructor(float.class, float.class, float.class, float.class);
            this.renderCtor = renderClass.getDeclaredConstructor(
                    colorClass,
                    ResourceLocation.class,
                    boolean.class,
                    ItemStack.class,
                    boolean.class
            );
            this.colorCtor.setAccessible(true);
            this.renderCtor.setAccessible(true);
        }
    }
}
