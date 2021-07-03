package logisticspipes.pipefxhandlers;

import java.util.Random;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

public class EntitySparkleFX extends Particle {

	public int multiplier;
	public boolean shrink;
	public int particle;
	public int blendmode;

	public EntitySparkleFX(World world, double x, double y, double z, float scalemult, float red, float green, float blue, int var12) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		shrink = false;
		particle = 0;
		blendmode = 1;

		particleRed = red;
		particleGreen = green;
		particleBlue = blue;
		particleGravity = 0.07F;
		motionX = motionY = motionZ = 0.0D;
		multipleParticleScaleBy(scalemult);
		maxAge = 3 * var12 - 1;
		multiplier = var12;
		canCollide = false;
	}

	private static final ResourceLocation TEXTURE = new ResourceLocation("logisticspipes", "textures/particles/particles.png");
	private static final ResourceLocation field_110737_b = new ResourceLocation("textures/particle/particles.png");

	@Override
	public void renderParticle(BufferBuilder buffer, @Nonnull ActiveRenderInfo entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		Tessellator.getInstance().draw();
		GL11.glPushMatrix();
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, blendmode);
		Minecraft.getInstance().textureManager.bindTexture(EntitySparkleFX.TEXTURE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F);
		float var8 = particle + partialTicks / multiplier;
		float var9 = var8 % 8 / 8.0F;
		float var10 = var9 + 0.124875F;
		float var11 = var8 / 8 / 8.0F;
		float var12 = var11 + 0.124875F;
		float var13 = 0.1F * width * ((maxAge - partialTicks + 1) / (float) maxAge);
		float var14 = (float) (prevPosX + (posX - prevPosX) * partialTicks - Particle.interpPosX);
		float var15 = (float) (prevPosY + (posY - prevPosY) * partialTicks - Particle.interpPosY);
		float var16 = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - Particle.interpPosZ);
		float var17 = 1.0F;
		//BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		//tesselator.setBrightness(240);
		buffer.pos(var14 - rotationX * var13 - rotationXY * var13, var15 - rotationZ * var13, var16 - rotationYZ * var13 - rotationXZ * var13).tex(var10, var12).color(particleRed * var17, particleGreen * var17, particleBlue * var17, 1.0F).endVertex();
		buffer.pos(var14 - rotationX * var13 + rotationXY * var13, var15 + rotationZ * var13, var16 - rotationYZ * var13 + rotationXZ * var13).tex(var10, var11).color(particleRed * var17, particleGreen * var17, particleBlue * var17, 1.0F).endVertex();
		buffer.pos(var14 + rotationX * var13 + rotationXY * var13, var15 + rotationZ * var13, var16 + rotationYZ * var13 + rotationXZ * var13).tex(var9, var11).color(particleRed * var17, particleGreen * var17, particleBlue * var17, 1.0F).endVertex();
		buffer.pos(var14 + rotationX * var13 - rotationXY * var13, var15 - rotationZ * var13, var16 + rotationYZ * var13 - rotationXZ * var13).tex(var9, var12).color(particleRed * var17, particleGreen * var17, particleBlue * var17, 1.0F).endVertex();
		Tessellator.getInstance().draw();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPopMatrix();
		Minecraft.getInstance().textureManager.bindTexture(EntitySparkleFX.field_110737_b);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void tick() {
		// TODO: Replace with super.tick() ?
		try {
			ClientPlayerEntity var1 = Minecraft.getInstance().player;

			if (var1.getDistanceSq(posX, posY, posZ) > 2500 || age++ >= maxAge) {
				setExpired();
				return;
			}

			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;

			motionX -= 0.05D * particleGravity - 0.1D * particleGravity * new Random().nextDouble();
			motionY -= 0.05D * particleGravity - 0.1D * particleGravity * new Random().nextDouble();
			motionZ -= 0.05D * particleGravity - 0.1D * particleGravity * new Random().nextDouble();

			move(motionX, motionY, motionZ);
			motionX *= 0.9800000190734863D;
			motionY *= 0.9800000190734863D;
			motionZ *= 0.9800000190734863D;

			if (onGround) {
				motionX *= 0.699999988079071D;
				motionZ *= 0.699999988079071D;
			}
		} catch (Exception ignored) {
		}
	}

	@Nonnull
	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.CUSTOM;
	}
}
