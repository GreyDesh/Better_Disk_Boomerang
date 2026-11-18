package greydesh.tactical.discs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import greydesh.tactical.discs.entity.DiscEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DiscRenderer extends EntityRenderer<DiscEntity> {
    private final ItemRenderer itemRenderer;

    public DiscRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DiscEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0.0D, 0.05D, 0.0D);

        // Lay flat horizontally like a frisbee / cutting disc
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // Rapidly spin around its center
        float spin = (entity.tickCount + partialTick) * 45.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        // Visual scale of the flying disc (doubled size for clear visibility)
        poseStack.scale(1.85F, 1.85F, 1.85F);

        ItemStack itemStack = entity.getItem();
        this.itemRenderer.renderStatic(
                itemStack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DiscEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
