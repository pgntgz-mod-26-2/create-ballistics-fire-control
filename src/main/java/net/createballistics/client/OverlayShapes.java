package net.createballistics.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createballistics.mixin.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * HUD 上少数几个<b>原版 {@code GuiGraphicsExtractor} 画不出来</b>的几何图形。
 *
 * <p>目前只有圆环：准星那种"一圈细线"没法用 {@code fill()}（那是矩形）或
 * {@code blit()}（那要贴图）表达，只能通过 {@link GuiElementRenderState} 自己拼。
 * 本类刻意做得极小、参数极少 —— 不学的图形不要提前加进来。
 */
public final class OverlayShapes {

    /** 蓄力圆环的两个半径（屏幕较短边的比例）—— 与卓越前线一致。 */
    public static final float CHARGE_OUTER = 0.07F;
    public static final float CHARGE_INNER = 0.052F;

    private OverlayShapes() {
    }

    /**
     * 蓄力圆环：深色底环（整圈）+ 白色进度弧，弧满一圈就是开火。
     *
     * <p><b>唯一实现</b> —— 火炮指示器的迭加层与<b>无人机图传 HUD</b> 都调它
     * （2026-09-23 用户要求：图传里也要看得到这圈读条，不然飞着的时候根本不知道
     * "按住左键到没到开火那一下"）。
     *
     * <p>⚠️ 它<b>不是</b>常显的准星 —— 2026-09-22 我第一版做成了"举镜时常显一个白圆环"，
     * 用户拿对比图指出那是<b>长按左键的蓄力进度</b>，而且常显的粗圆环正好糊在准星上挡视线。
     * 所以只有真的在蓄力（{@code progress > 0}）时才画。
     */
    public static void chargeRing(GuiGraphicsExtractor guiGraphics, float centerX, float centerY, float progress) {
        if (progress <= 0.0F) {
            return;
        }
        ring(guiGraphics, centerX, centerY, CHARGE_OUTER, CHARGE_INNER, 1.0F, 0.0F, 0.0F, 0.0F, 0.4F);
        ring(guiGraphics, centerX, centerY, CHARGE_OUTER, CHARGE_INNER, progress, 1.0F, 1.0F, 1.0F, 0.8F);
    }

    /**
     * 以 {@code (centerX, centerY)} 为心画一段圆环弧。
     *
     * @param progress 画多少：1.0 = 整圈，0.5 = 半圈（蓄力进度就用它）
     * @param color    依次是 r / g / b / a，取值 0~1
     */
    public static void ring(GuiGraphicsExtractor guiGraphics, float centerX, float centerY,
                            float outerRatio, float innerRatio, float progress, float... color) {
        if (progress <= 0.0F) {
            return;
        }
        float scale = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        float outer = outerRatio * scale;
        float inner = innerRatio * scale;
        float maxAngle = (float) (Math.PI * 2.0) * Mth.clamp(progress, 0.0F, 1.0F);
        int argb = ARGB.colorFromFloat(color[3], color[0], color[1], color[2]);

        Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose()).rotateAbout(-Mth.HALF_PI, centerX, centerY);
        int minX = (int) Math.floor(centerX - outer - 1.0F);
        int minY = (int) Math.floor(centerY - outer - 1.0F);
        int size = (int) Math.ceil((outer + 1.0F) * 2.0F);
        ScreenRectangle bounds = new ScreenRectangle(minX, minY, size, size).transformMaxBounds(guiGraphics.pose());

        ((GuiGraphicsExtractorAccessor) guiGraphics).createBallistics$guiRenderState().addGuiElement(
                new RingRenderState(RenderPipelines.GUI, TextureSetup.noTexture(), pose,
                        centerX, centerY, outer, inner, maxAngle, argb, bounds));
    }

    private record RingRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            float centerX,
            float centerY,
            float outer,
            float inner,
            float maxAngle,
            int color,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            final float step = Mth.PI / 90.0F;
            int steps = Math.max(1, (int) Math.ceil(maxAngle / step));
            for (int i = 0; i < steps; i++) {
                float a0 = Math.min(i * step, maxAngle);
                float a1 = Math.min((i + 1) * step, maxAngle);
                if (a1 <= a0) {
                    continue;
                }
                float cos0 = Mth.cos(a0);
                float sin0 = Mth.sin(a0);
                float cos1 = Mth.cos(a1);
                float sin1 = Mth.sin(a1);
                vertexConsumer.addVertexWith2DPose(pose, centerX + outer * cos0, centerY + outer * sin0).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, centerX + inner * cos0, centerY + inner * sin0).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, centerX + inner * cos1, centerY + inner * sin1).setColor(color);
                vertexConsumer.addVertexWith2DPose(pose, centerX + outer * cos1, centerY + outer * sin1).setColor(color);
            }
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return null;
        }
    }
}
