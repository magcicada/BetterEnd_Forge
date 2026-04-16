package org.betterx.betterend.util;

public final class BETickChunkContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private BETickChunkContext() {}

    public static void push() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void pop() {
        int depth = DEPTH.get() - 1;
        DEPTH.set(Math.max(depth, 0));
    }

    public static boolean isInTickChunk() {
        return DEPTH.get() > 0;
    }
}
