/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.item;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import lombok.Data;
import lombok.experimental.Accessors;

import logisticspipes.LPItems;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.SimpleGraphics;
import network.rs485.logisticspipes.util.TextUtil;

@Data
@Accessors(chain = true)
@OnlyIn(Dist.CLIENT)
public class ItemStackRenderer {

	private GameRenderer gameRenderer;
	private ItemRenderer itemRenderer;
	private TextureManager texManager;
	private FontRenderer fontRenderer;
	private ItemStackTileEntityRenderer stackTileRenderer;

	@Nonnull
	private ItemStack itemstack = ItemStack.EMPTY;
	private ItemIdentifierStack itemIdentStack;
	private int posX;
	private int posY;
	private float zLevel;
	private float scaleX;
	private float scaleY;
	private float scaleZ;
	private DisplayAmount displayAmount;
	private boolean renderEffects;
	private boolean ignoreDepth;
	private boolean renderInColor;
	private World world;
	private float partialTickTime;

	public ItemStackRenderer(int posX, int posY, float zLevel, boolean renderEffects, boolean ignoreDepth) {
		this.posX = posX;
		this.posY = posY;
		this.zLevel = zLevel;
		this.renderEffects = renderEffects;
		this.ignoreDepth = ignoreDepth;
		gameRenderer = Minecraft.getInstance().gameRenderer;
		fontRenderer = Minecraft.getInstance().fontRenderer;
		world = Minecraft.getInstance().world;
		texManager = Minecraft.getInstance().textureManager;
		itemRenderer = Minecraft.getInstance().getItemRenderer();
		stackTileRenderer = ItemStackTileEntityRenderer.instance;
		scaleX = 1.0F;
		scaleY = 1.0F;
		scaleZ = 1.0F;
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount) {
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(_allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, zLevel, displayAmount, true, false);
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount, boolean renderEffect, boolean ignoreDepth) {
		ItemStackRenderer itemStackRenderer = new ItemStackRenderer(0, 0, zLevel, renderEffect, ignoreDepth);
		itemStackRenderer.setDisplayAmount(displayAmount);
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(_allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, itemStackRenderer);
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, ItemStackRenderer itemStackRenderer) {
		int ppi = 0;
		int column = 0;
		int row = 0;

		for (ItemIdentifierStack identifierStack : _allItems) {
			if (identifierStack == null) {
				column++;
				if (column >= columns) {
					row++;
					column = 0;
				}
				ppi++;
				continue;
			}
			ItemIdentifier item = identifierStack.getItem();
			if (IItemSearch != null && !IItemSearch.itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= items * page) {
				continue;
			}
			if (ppi > items * (page + 1)) {
				continue;
			}
			ItemStack itemstack = identifierStack.unsafeMakeNormalStack();
			int x = left + xSize * column;
			int y = top + ySize * row + 1;

			if (!itemstack.isEmpty()) {
				itemStackRenderer.setItemstack(itemstack).setPosX(x).setPosY(y);
				itemStackRenderer.renderInGui();
			}

			column++;
			if (column >= columns) {
				row++;
				column = 0;
			}
		}
	}

	public void renderInGui() {
		assert displayAmount != null;
		assert itemRenderer != null;
		assert texManager != null;
		assert fontRenderer != null;
		assert scaleX != 0.0F;
		assert scaleY != 0.0F;
		assert scaleZ != 0.0F;

		GlStateManager.pushMatrix();

		// The only thing that ever sets NORMALIZE are slimes. It never gets disabled and it interferes with our lightning in the HUD.
		GlStateManager.disableNormalize();

		// set up lightning
		GlStateManager.scalef(1.0F / scaleX, 1.0F / scaleY, 1.0F / scaleZ);
		RenderHelper.enableGUIStandardItemLighting();

		// FIXME: lightmap
		// OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		GlStateManager.scalef(scaleX, scaleY, scaleZ);

		if (ignoreDepth) {
			GlStateManager.disableDepthTest();
		} else {
			GlStateManager.enableDepthTest();
		}

		itemRenderer.zLevel += zLevel;

		if (itemIdentStack != null) {
			if (itemIdentStack.getStackSize() < 1) {
				itemstack = itemIdentStack.getItem().unsafeMakeNormalStack(1);
			} else {
				itemstack = itemIdentStack.unsafeMakeNormalStack();
			}
		}

		IBakedModel bakedmodel = itemRenderer
				.getItemModelWithOverrides(itemstack, null, (renderEffects ? Minecraft.getInstance().player : null));

		GlStateManager.pushMatrix();
		this.texManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		this.texManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableAlphaTest();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.setupGuiTransform(posX, posY, bakedmodel.isGui3d());
		bakedmodel.handlePerspective(TransformType.GUI);
		itemRenderer.renderItem(itemstack, bakedmodel);
		GlStateManager.disableAlphaTest();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableLighting();
		GlStateManager.popMatrix();

		this.texManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		this.texManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

		itemRenderer.zLevel -= zLevel;

		// disable lightning
		RenderHelper.disableStandardItemLighting();

		if (ignoreDepth) {
			GlStateManager.disableDepthTest();
		} else {
			GlStateManager.enableDepthTest();
		}
		// 20 should be about the size of a block
		GuiGraphics.drawDurabilityBar(itemstack, posX, posY, zLevel + 20.0F);

		// if we want to render the amount, do that
		if (displayAmount != DisplayAmount.NEVER) {
			if (ignoreDepth) {
				GlStateManager.disableDepthTest();
			} else {
				GlStateManager.enableDepthTest();
			}

			FontRenderer specialFontRenderer = itemstack.getItem().getFontRenderer(itemstack);

			if (specialFontRenderer != null) {
				fontRenderer = specialFontRenderer;
			}

			GlStateManager.disableLighting();
			String amountString = TextUtil.getThreeDigitFormattedNumber(itemIdentStack != null ? itemIdentStack.getStackSize() : itemstack.getCount(), displayAmount == DisplayAmount.ALWAYS);
			GlStateManager.translatef(0.0F, 0.0F, zLevel + 130.0F);

			// using a translated shadow does not hurt and works with the HUD
			SimpleGraphics.drawStringWithTranslatedShadow(fontRenderer, amountString, posX + 17 - fontRenderer.getStringWidth(amountString), posY + 9, Color.getValue(Color.WHITE));

			GlStateManager.translatef(0.0F, 0.0F, -(zLevel + 130.0F));
		}

		GlStateManager.popMatrix();
	}

	private void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
		GlStateManager.translatef((float) xPosition, (float) yPosition, 100.0F + itemRenderer.zLevel);
		GlStateManager.translatef(8.0F, 8.0F, 0.0F);
		GlStateManager.scalef(1.0F, -1.0F, 1.0F);
		GlStateManager.scalef(16.0F, 16.0F, 16.0F);

		if (isGui3d) {
			GlStateManager.enableLighting();
		} else {
			GlStateManager.disableLighting();
		}
	}

	public void renderInWorld() {
		assert gameRenderer != null;
		assert itemRenderer != null;
		assert scaleX != 0.0F;
		assert scaleY != 0.0F;
		assert scaleZ != 0.0F;

		if (itemstack.isEmpty()) {
			// :item_card: 🤷
			itemstack = new ItemStack(LPItems.itemCard);
		}

		Item item = itemstack.getItem();
		if (item instanceof BlockItem) {
			Block block = ((BlockItem) item).getBlock();
			// FIXME: BlockPane
//			if (block instanceof BlockPane) {
//				GlStateManager.scalef(0.5F, 0.5F, 0.5F);
//			}
		} else if (item == LPItems.requestTable) {
			GlStateManager.scalef(0.5F, 0.5F, 0.5F);
		}

		stackTileRenderer.renderByItem(itemstack);
	}

	public void renderItemInGui(float x, float y, Item item, float zLevel, float scale) {
		this.setPosX(0);
		this.setPosY(0);
		this.setScaleX(1f);
		this.setScaleY(1f);
		this.itemstack = new ItemStack(item);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, -100.0);
		GlStateManager.scale(scale, scale, 1f);
		GlStateManager.disableDepth();
		float previousZ = renderItem.zLevel;
		renderItem.zLevel = zLevel;
		this.renderInGui();
		renderItem.zLevel = previousZ;
		GlStateManager.enableDepth();
		GlStateManager.scale(1 / scale, 1 / scale, 1f);
		GlStateManager.translate(-x, -y, 100.0);
		GlStateManager.popMatrix();
	}

	public enum DisplayAmount {
		HIDE_ONE,
		ALWAYS,
		NEVER,
	}

}
