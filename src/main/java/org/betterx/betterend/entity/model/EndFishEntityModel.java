package org.betterx.betterend.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import org.betterx.betterend.entity.EndFishEntity;

public class EndFishEntityModel extends EntityModel<EndFishEntity> {
   private final ModelPart model;
   private final ModelPart fin_top;
   private final ModelPart fin_bottom;
   private final ModelPart flipper;
   private final ModelPart fin_right;
   private final ModelPart fin_left;

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition modelData = new MeshDefinition();
      PartDefinition modelPartData = modelData.getRoot();
      PartDefinition bodyPart = modelPartData.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -4.0F, 2.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      bodyPart.addOrReplaceChild(
         "top_fin",
         CubeListBuilder.create().texOffs(0, 6).addBox(0.0F, -8.0F, 0.0F, 0.0F, 8.0F, 6.0F),
         PartPose.offsetAndRotation(0.0F, -2.0F, -4.0F, -0.6981F, 0.0F, 0.0F)
      );
      bodyPart.addOrReplaceChild(
         "bottom_fin",
         CubeListBuilder.create().texOffs(0, 6).addBox(0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 6.0F),
         PartPose.offsetAndRotation(0.0F, 2.0F, -4.0F, 0.6981F, 0.0F, 0.0F)
      );
      bodyPart.addOrReplaceChild(
         "tail_fin",
         CubeListBuilder.create().texOffs(0, 15).addBox(0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 5.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, -0.7854F, 0.0F, 0.0F)
      );
      bodyPart.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(0, 25).addBox(-3.7071F, 0.7071F, -1.5F, 3.0F, 0.0F, 3.0F),
         PartPose.offsetAndRotation(-1.0F, 0.0F, -1.0F, 1.5708F, 0.7854F, 0.0F)
      );
      bodyPart.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().mirror().texOffs(0, 25).addBox(0.7071F, 0.7071F, -1.5F, 3.0F, 0.0F, 3.0F),
         PartPose.offsetAndRotation(1.0F, 0.0F, -1.0F, 1.5708F, -0.7854F, 0.0F)
      );
      return LayerDefinition.create(modelData, 32, 32);
   }

   public EndFishEntityModel(ModelPart modelPart) {
      super(RenderType::entityCutout);
      this.model = modelPart.getChild("body");
      this.fin_top = this.model.getChild("top_fin");
      this.fin_bottom = this.model.getChild("bottom_fin");
      this.flipper = this.model.getChild("tail_fin");
      this.fin_right = this.model.getChild("right_fin");
      this.fin_left = this.model.getChild("left_fin");
   }

   public void setupAnim(EndFishEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
      float s1 = (float)Math.sin(animationProgress * 0.1);
      float s2 = (float)Math.sin(animationProgress * 0.05);
      this.flipper.yRot = s1 * 0.3F;
      this.fin_top.xRot = s2 * 0.02F - 0.6981F;
      this.fin_bottom.xRot = 0.6981F - s2 * 0.02F;
      this.fin_left.yRot = s1 * 0.3F - 0.7854F;
      this.fin_right.yRot = 0.7854F - s1 * 0.3F;
   }

   public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
      this.model.render(matrices, vertices, light, overlay);
   }
}
