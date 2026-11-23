package net.createballistics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createballistics.registry.ModItems;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第三人称举起指示器时，走原版「举望远镜」手臂姿势。
 */
@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel<S> & HeadedModel>
        extends ItemInHandLayer<S, M> {

    public PlayerItemInHandLayerMixin(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void createballistics$spyglassArmPose(S state, ItemStackRenderState item, ItemStack itemStack,
                                                  HumanoidArm arm, PoseStack poseStack,
                                                  SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                  CallbackInfo ci) {
        if (item.isEmpty() || !itemStack.is(ModItems.ARTILLERY_INDICATOR.get())) {
            return;
        }
        InteractionHand currentHand = arm == state.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (state.isUsingItem && state.useItemHand == currentHand && state.attackTime < 1.0E-5F) {
            ci.cancel();
            poseStack.pushPose();
            this.getParentModel().root().translateAndRotate(poseStack);
            ModelPart head = this.getParentModel().getHead();
            float previousXRot = head.xRot;
            head.xRot = Mth.clamp(head.xRot, (float) (-Math.PI / 6), (float) (Math.PI / 2));
            head.translateAndRotate(poseStack);
            head.xRot = previousXRot;
            CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
            boolean isLeftHand = arm == HumanoidArm.LEFT;
            poseStack.translate((isLeftHand ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
