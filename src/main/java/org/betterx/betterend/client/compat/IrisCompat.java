package org.betterx.betterend.client.compat;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class IrisCompat {
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");

    private IrisCompat() {
    }

    /**
     * Returns true when Iris/Oculus should render sky itself.
     * This avoids conflicts with custom End sky rendering while shader packs are active.
     */
    public static boolean shouldUseShaderSky() {
        if (!IRIS_LOADED) {
            return false;
        }

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method packInUseMethod = apiClass.getMethod("isShaderPackInUse");
            return (boolean) packInUseMethod.invoke(api);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Method quickCheckMethod = irisClass.getMethod("isPackInUseQuick");
            return (boolean) quickCheckMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return false;
    }
}
