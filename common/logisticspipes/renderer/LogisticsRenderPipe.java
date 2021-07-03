package logisticspipes.renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.model.SignModel;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.minecraftforge.client.ForgeHooksClient;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.newpipe.LogisticsNewPipeItemBoxRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsRenderPipe extends TileEntityRenderer<LogisticsTileGenericPipe> {

	private static final int LIQUID_STAGES = 40;
	private static final int MAX_ITEMS_TO_RENDER = 10;
	private static final ResourceLocation SIGN = new ResourceLocation("textures/entity/sign.png");
	public static LogisticsNewRenderPipe secondRenderer = new LogisticsNewRenderPipe();
	public static LogisticsNewPipeItemBoxRenderer boxRenderer = new LogisticsNewPipeItemBoxRenderer();
	public static ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
	private static final ItemStackRenderer itemRenderer = new ItemStackRenderer(0, 0, 0, false, false);
	private final SignModel signModel;

	public LogisticsRenderPipe() {
		super();
		signModel = new SignModel();
		signModel.getSignStick().showModel = false;
	}

	@Override
	public void render(LogisticsTileGenericPipe tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage) {
		boolean inHand = false;
		if (tile == null && x == 0 && y == 0 && z == 0) {
			inHand = true;
		} else if (tile == null || tile.pipe == null) {
			return;
		}

		GlStateManager.enableDepthTest();
		GlStateManager.depthFunc(515);
		GlStateManager.depthMask(true);

		if (destroyStage >= 0) {
			this.bindTexture(DESTROY_STAGES[destroyStage]);
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.pushMatrix();
			GlStateManager.scalef(4.0F, 4.0F, 1.0F);
			//GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		}

		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();

		if (destroyStage < 0) {
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}

		if (!inHand) {
			if (tile.pipe instanceof CoreRoutedPipe) {
				renderPipeSigns((CoreRoutedPipe) tile.pipe, x, y, z, partialTicks);
			}
		}

		double distance = !inHand ? new DoubleCoordinates((TileEntity) tile).distanceTo(new DoubleCoordinates(Minecraft.getInstance().player)) : 0;

		LogisticsRenderPipe.secondRenderer.renderTileEntityAt(tile, x, y, z, partialTicks, distance);

		if (!inHand && !tile.isOpaque()) {
			if (tile.pipe.transport instanceof PipeFluidTransportLogistics) {
				//renderFluids(pipe.pipe, x, y, z);
			}
			if (tile.pipe.transport != null) {
				renderSolids(tile.pipe, x, y, z, partialTicks);
			}
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		if (destroyStage >= 0) {
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		}
	}

	private void renderSolids(CoreUnroutedPipe pipe, double x, double y, double z, float partialTickTime) {
		GL11.glPushMatrix();

		final World world = Objects.requireNonNull(pipe.container.getWorld(), "world may not be null");
		float light = world.getBrightness(pipe.container.getPos());

		int count = 0;
		for (LPTravelingItem item : pipe.transport.items) {
			CoreUnroutedPipe lPipe = pipe;
			double lX = x;
			double lY = y;
			double lZ = z;
			float lItemYaw = item.getYaw();
			if (count >= LogisticsRenderPipe.MAX_ITEMS_TO_RENDER) {
				break;
			}

			if (item.getItemIdentifierStack() == null) {
				continue;
			}
			if (!item.getContainer().getPos().equals(lPipe.container.getPos())) {
				continue;
			}

			if (item.getPosition() > lPipe.transport.getPipeLength() || item.getPosition() < 0) {
				continue;
			}

			float fPos = item.getPosition() + item.getSpeed() * partialTickTime;
			if (fPos > lPipe.transport.getPipeLength() && item.output != null) {
				CoreUnroutedPipe nPipe = lPipe.transport.getNextPipe(item.output);
				if (nPipe != null) {
					fPos -= lPipe.transport.getPipeLength();
					lX -= lPipe.getX() - nPipe.getX();
					lY -= lPipe.getY() - nPipe.getY();
					lZ -= lPipe.getZ() - nPipe.getZ();
					lItemYaw += lPipe.transport.getYawDiff(item);
					lPipe = nPipe;
					item = item.renderCopy();
					item.input = item.output;
					item.output = null;
				} else {
					continue;
				}
			}

			DoubleCoordinates pos = lPipe.getItemRenderPos(fPos, item);
			if (pos == null) {
				continue;
			}
			double boxScale = lPipe.getBoxRenderScale(fPos, item);
			double itemYaw = (lPipe.getItemRenderYaw(fPos, item) - lPipe.getItemRenderYaw(0, item) + lItemYaw) % 360;
			double itemPitch = lPipe.getItemRenderPitch(fPos, item);
			double itemYawForPitch = lPipe.getItemRenderYaw(fPos, item);

			ItemStack stack = item.getItemIdentifierStack().makeNormalStack();
			doRenderItem(stack, world, lX + pos.getXCoord(), lY + pos.getYCoord(), lZ + pos.getZCoord(), light, 0.75F, boxScale, itemYaw, itemPitch, itemYawForPitch, partialTickTime);
			count++;
		}

		count = 0;
		double dist = 0.135;
		DoubleCoordinates pos = new DoubleCoordinates(0.5, 0.5, 0.5);
		CoordinateUtils.add(pos, Direction.SOUTH, dist);
		CoordinateUtils.add(pos, Direction.EAST, dist);
		CoordinateUtils.add(pos, Direction.UP, dist);
		for (Pair<ItemIdentifierStack, Pair<Integer, Integer>> item : pipe.transport._itemBuffer) {
			if (item == null || item.getValue1() == null) {
				continue;
			}
			ItemStack stack = item.getValue1().makeNormalStack();
			doRenderItem(stack, world, x + pos.getXCoord(), y + pos.getYCoord(), z + pos.getZCoord(), light, 0.25F, 0, 0, 0, 0, partialTickTime);
			count++;
			if (count >= 27) {
				break;
			} else if (count % 9 == 0) {
				CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
				CoordinateUtils.add(pos, Direction.EAST, dist * 2.0);
				CoordinateUtils.add(pos, Direction.DOWN, dist);
			} else if (count % 3 == 0) {
				CoordinateUtils.add(pos, Direction.SOUTH, dist * 2.0);
				CoordinateUtils.add(pos, Direction.WEST, dist);
			} else {
				CoordinateUtils.add(pos, Direction.NORTH, dist);
			}
		}

		GL11.glPopMatrix();
	}

	public void doRenderItem(@Nonnull ItemStack itemstack, World world, double x, double y, double z, float light, float renderScale, double boxScale, double yaw, double pitch, double yawForPitch, float partialTickTime) {
		LogisticsRenderPipe.boxRenderer.doRenderItem(itemstack, light, x, y, z, boxScale, yaw, pitch, yawForPitch);

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glScalef(renderScale, renderScale, renderScale);
		GL11.glRotated(yawForPitch, 0, 1, 0);
		GL11.glRotated(pitch, 1, 0, 0);
		GL11.glRotated(-yawForPitch, 0, 1, 0);
		GL11.glRotated(yaw, 0, 1, 0);
		GL11.glTranslatef(0.0F, -0.35F, 0.0F);
		itemRenderer.setItemstack(itemstack).setWorld(world).setPartialTickTime(partialTickTime);
		itemRenderer.renderInWorld();
		GL11.glPopMatrix();
	}

	private boolean needDistance(List<Pair<Direction, IPipeSign>> list) {
		List<Pair<Direction, IPipeSign>> copy = new ArrayList<>(list);
		Iterator<Pair<Direction, IPipeSign>> iter = copy.iterator();
		boolean north = false, south = false, east = false, west = false;
		while (iter.hasNext()) {
			Pair<Direction, IPipeSign> pair = iter.next();
			if (pair.getValue1() == Direction.UP || pair.getValue1() == Direction.DOWN || pair.getValue1() == null) {
				iter.remove();
			}
			if (pair.getValue1() == Direction.NORTH) {
				north = true;
			}
			if (pair.getValue1() == Direction.SOUTH) {
				south = true;
			}
			if (pair.getValue1() == Direction.EAST) {
				east = true;
			}
			if (pair.getValue1() == Direction.WEST) {
				west = true;
			}
		}
		boolean result = copy.size() > 1;
		if (copy.size() == 2) {
			if (north && south) {
				result = false;
			}
			if (east && west) {
				result = false;
			}
		}
		return result;
	}

	private void renderPipeSigns(CoreRoutedPipe pipe, double x, double y, double z, float partialTickTime) {
		if (!pipe.getPipeSigns().isEmpty()) {
			List<Pair<Direction, IPipeSign>> list = pipe.getPipeSigns();
			for (Pair<Direction, IPipeSign> pair : list) {
				if (pipe.container.renderState.pipeConnectionMatrix.isConnected(pair.getValue1())) {
					continue;
				}
				GL11.glPushMatrix();
				GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
				switch (pair.getValue1()) {
					case UP:
						GL11.glRotatef(90, 1.0F, 0.0F, 0.0F);
						break;
					case DOWN:
						GL11.glRotatef(-90, 1.0F, 0.0F, 0.0F);
						break;
					case NORTH:
						GL11.glRotatef(0, 0.0F, 1.0F, 0.0F);
						if (needDistance(list)) {
							GL11.glTranslatef(0.0F, 0.0F, -0.15F);
						}
						break;
					case SOUTH:
						GL11.glRotatef(-180, 0.0F, 1.0F, 0.0F);
						if (needDistance(list)) {
							GL11.glTranslatef(0.0F, 0.0F, -0.15F);
						}
						break;
					case EAST:
						GL11.glRotatef(-90, 0.0F, 1.0F, 0.0F);
						if (needDistance(list)) {
							GL11.glTranslatef(0.0F, 0.0F, -0.15F);
						}
						break;
					case WEST:
						GL11.glRotatef(90, 0.0F, 1.0F, 0.0F);
						if (needDistance(list)) {
							GL11.glTranslatef(0.0F, 0.0F, -0.15F);
						}
						break;
					default:
				}
				renderSign(pipe, pair.getValue2(), partialTickTime);
				GL11.glPopMatrix();
			}
		}
	}

	private void renderSign(CoreRoutedPipe pipe, IPipeSign type, float partialTickTime) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		GL11.glDisable(GL12.GL_RESCALE_NORMAL);

		float signScale = 2 / 3.0F;
		GL11.glTranslatef(0.0F, -0.3125F, -0.36F);
		GL11.glRotatef(180, 0.0f, 1.0f, 0.0f);
		Minecraft.getInstance().textureManager.bindTexture(LogisticsRenderPipe.SIGN);

		GL11.glPushMatrix();
		GL11.glScalef(signScale, -signScale, -signScale);
		signModel.renderSign();
		GL11.glPopMatrix();

		GL11.glTranslatef(-0.32F, 0.5F * signScale + 0.08F, 0.07F * signScale);

		type.render(pipe, this);
	}

	public void renderItemStackOnSign(@Nonnull ItemStack itemstack) {
		if (itemstack.isEmpty()) {
			return; // Only happens on false configuration
		}

		Minecraft mc = Minecraft.getInstance();
		final ItemRenderer itemRenderer = mc.getItemRenderer();

		GlStateManager.disableLighting();
		GlStateManager.color3f(1F, 1F, 1F); //Forge: Reset color in case Items change it.
		GlStateManager.enableBlend(); //Forge: Make sure blend is enabled else tabs show a white border.
		itemRenderer.zLevel = 100.0F;
		GlStateManager.enableRescaleNormal();

		// itemRenderer.renderItemAndEffectIntoGUI(itemstack, 0, 0);
		// item render code
		GlStateManager.pushMatrix();
		mc.textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		mc.textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableAlphaTest();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// mezz.jei.render.ItemStackFastRenderer#getBakedModel
		ItemModelMesher itemModelMesher = itemRenderer.getItemModelMesher();
		IBakedModel bakedModel = itemModelMesher.getItemModel(itemstack);
		bakedModel = bakedModel.getOverrides().getModelWithOverrides(bakedModel, itemstack, null, null);

		// make item/block flat and position it
		GlStateManager.translatef(0.05F, 0F, 0F);
		GlStateManager.scalef(0.8F, 0.8F, 0.001F);

		// model rotation
		if (bakedModel != null) {
			bakedModel.handlePerspective(TransformType.GUI);

			// model scaling to fit on sign
			GlStateManager.scalef(0.4F, 0.4F, 0.4F);

			itemRenderer.renderItem(itemstack, bakedModel);
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.disableAlphaTest();
		GlStateManager.popMatrix();
		mc.textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		mc.textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
		// item render code end

		// not needed?
		//itemRenderer.renderItemOverlays(mc.fontRenderer, itemstack, 0, 0);
		itemRenderer.zLevel = 0.0F;
	}

	public String cut(String name, FontRenderer renderer) {
		if (renderer.getStringWidth(name) < 90) {
			return name;
		}
		StringBuilder sum = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			if (renderer.getStringWidth(sum.toString() + name.charAt(i) + "...") < 90) {
				sum.append(name.charAt(i));
			} else {
				return sum.toString() + "...";
			}
		}
		return sum.toString();
	}
}
