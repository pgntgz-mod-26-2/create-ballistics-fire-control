package net.createballistics.mixin;

import net.createballistics.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 举起指示器时<b>压掉原版那层望远镜圆形遮罩</b>。
 */
@Mixin(Hud.class)
public abstract class GuiSpyglassOverlayMixin {

    @Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void createballistics$noVanillaScopeMask(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
            ci.cancel();
        }
    }
}
