package net.createballistics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 三个火控方块共用的传动轴渲染器 —— 把 Create 标准的半截轴插进「应力接入面」并按转速旋转。
 */
public class FireControlShaftRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

    private final EnumProperty<Direction> facingProperty;
    private final boolean shaftOnFacing;
    private List<BlockStateModelPart> shaftHalfModel;

    public FireControlShaftRenderer(BlockEntityRendererProvider.Context context,
                                    EnumProperty<Direction> facingProperty, boolean shaftOnFacing) {
        super(context);
        this.facingProperty = facingProperty;
        this.shaftOnFacing = shaftOnFacing;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(BlockEntityRenderState renderState, PoseStack ms, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        if (!(renderState instanceof KineticBlockEntityRenderer.KineticRenderState state)
                || state.blockEntity == null) {
            return;
        }
        T be = (T) state.blockEntity;
        if (isInvalid(be) || be.getLevel() == null) {
            return;
        }
        BlockState blockState = be.getBlockState();
        if (!blockState.hasProperty(facingProperty)) {
            return;
        }
        List<BlockStateModelPart> model = getShaftHalfModel();
        if (model.isEmpty()) {
            return;
        }

        Direction facing = blockState.getValue(facingProperty);
        Direction shaftFace = shaftOnFacing ? facing : facing.getOpposite();
        Direction.Axis axis = facing.getAxis();
        BlockPos pos = be.getBlockPos();

        float time = KineticBlockEntityRenderer.getRenderTime(be, state.partialTicks);
        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, pos, axis);
        float angle = time * be.getSpeed() * 3.0F / 10.0F % 360.0F;

        if (be.getSpeed() != 0.0F && be.hasSource()) {
            BlockPos source = be.source.subtract(pos);
            Direction sourceFacing = Direction.getApproximateNearest(source.getX(), source.getY(), source.getZ());
            if (sourceFacing.getAxis() == axis) {
                angle *= sourceFacing == shaftFace ? 1.0F : -1.0F;
            } else if (sourceFacing.getAxisDirection() == shaftFace.getAxisDirection()) {
                angle *= -1.0F;
            }
        }

        angle += offset;
        angle = angle / 180.0F * (float) Math.PI;

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(rotation(axis, angle));
        rotateHalfShaftTo(ms, shaftFace);
        ms.translate(-0.5, -0.5, -0.5);
        collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), model,
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ms.popPose();
    }

    private List<BlockStateModelPart> getShaftHalfModel() {
        if (shaftHalfModel == null) {
            BlockStateModelPart part = Minecraft.getInstance().getModelManager()
                    .getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
            shaftHalfModel = part == null ? List.of() : List.of(part);
        }
        return shaftHalfModel;
    }

    private static void rotateHalfShaftTo(PoseStack ms, Direction dir) {
        switch (dir) {
            case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case UP -> ms.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> {
            }
        }
    }
}
