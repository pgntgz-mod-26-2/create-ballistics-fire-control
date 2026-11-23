package net.createballistics.cbc;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;

/**
 * CBC: Advanced Technology (cbc_at) optional compat stub for MC 26.2.
 */
public final class CbcAtCompat {

    public static final String MOD_ID = "cbc_at";

    private CbcAtCompat() {
    }

    public static double barrelVelocity(@Nullable Level level, @Nullable AbstractMountedCannonContraption cannon) {
        return 0.0;
    }
}
