package net.createballistics.cbc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Sable optional compat stub for MC 26.2 (Sable is not present in this environment).
 */
public final class SableCompat {

    public static final String MOD_ID = "sable";

    private SableCompat() {
    }

    public static boolean available() {
        return false;
    }

    public static boolean isOnPhysicsStructure(@Nullable Level level, @Nullable BlockPos pos) {
        return false;
    }

    public static Vec3 structureDisplacementAt(@Nullable Level level, @Nullable Vec3 worldPos) {
        return Vec3.ZERO;
    }

    public static boolean scaleIsUsable(@Nullable Level level, @Nullable Vec3 pos) {
        return true;
    }

    public static Vec3 toWorld(@Nullable Level level, Vec3 plotPos) {
        return plotPos;
    }

    @Nullable
    public static float[] worldAimToLocal(@Nullable Level level, @Nullable Vec3 plotPos,
                                          double worldYawDeg, double worldPitchDeg) {
        return null;
    }

    public static Vec3 velocityAt(@Nullable Level level, Vec3 plotPos) {
        return Vec3.ZERO;
    }

    public static BlockPos displayPos(@Nullable Level level, BlockPos levelPos) {
        return levelPos;
    }

    @Nullable
    public static Vec3[] eyeAndLookToWorld(@Nullable Level level, Vec3 eye, Vec3 look) {
        return null;
    }

    @Nullable
    public static double[] orientationAt(@Nullable Level level, @Nullable Vec3 plotPos) {
        return null;
    }
}
