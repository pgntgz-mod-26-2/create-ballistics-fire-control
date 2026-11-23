package net.createballistics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createballistics.client.model.DroneModel;
import net.createballistics.entity.DroneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Native 26.2 drone renderer reading Bedrock geometry directly from {@link DroneModel}.
 */
public class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderer.DroneRenderState> {

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2F;
    }

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }

    @Override
    public void extractRenderState(DroneEntity entity, DroneRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getViewYRot(partialTick);
        state.spinRad = (float) (System.currentTimeMillis() % 36000000L) / 12.0F;
    }

    @Override
    public void submit(DroneRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        final int light = state.lightCoords;
        final float spinRad = state.spinRad;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(DroneModel.TEXTURE),
                (pose, buffer) -> DroneModel.render(pose, buffer, light, spinRad));
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static class DroneRenderState extends EntityRenderState {
        public float yRot;
        public float spinRad;
    }
}
