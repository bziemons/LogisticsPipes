/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.IRequestAPI;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCQueued;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.request.resources.IResource;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.logistic.IDestination;
import network.rs485.logisticspipes.logistic.Interests;

@CCType(name = "LogisticsPipes:Request")
public class PipeItemsRequestLogistics extends CoreRoutedPipe implements IRequestAPI {

	public PipeItemsRequestLogistics(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_REQUESTER_TEXTURE;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	public void openGui(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_Normal_Orderer_ID, getWorld(), getX(), getY(), getZ());
	}

	@Override
	public boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings) {
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openRequest) {
				openGui(entityplayer);
			} else {
				entityplayer.sendMessage(new TextComponentString("Permission denied"));
			}
		}
		return true;
	}

	@Override
	protected Stream<Interests> streamPipeInterests() {
		return Stream.empty();
	}

	@Override
	public IDestination handleItem(IRoutedItem item) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	/* IRequestAPI */

	@Override
	public List<ItemStack> getProvidedItems() {
		if (stillNeedReplace()) {
			return new ArrayList<>();
		}
		// TODO PROVIDE REFACTOR
		Map<ItemIdentifier, Integer> items = Collections.emptyMap(); // SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost());
		List<ItemStack> list = new ArrayList<>(items.size());
		for (Entry<ItemIdentifier, Integer> item : items.entrySet()) {
			ItemStack is = item.getKey().unsafeMakeNormalStack(item.getValue());
			list.add(is);
		}
		return list;
	}

	@Override
	public List<ItemStack> getCraftedItems() {
		if (stillNeedReplace()) {
			return new ArrayList<>();
		}
		// TODO PROVIDE REFACTOR
		List<ItemIdentifier> items = Collections.emptyList(); // SimpleServiceLocator.logisticsManager.getCraftableItems(getRouter().getIRoutersByCost());
		List<ItemStack> list = new ArrayList<>(items.size());
		for (ItemIdentifier item : items) {
			ItemStack is = item.unsafeMakeNormalStack(1);
			list.add(is);
		}
		return list;
	}

	@Override
	public SimulationResult simulateRequest(ItemStack wanted) {
		final List<IResource> used = Collections.emptyList();
		final List<IResource> missing = Collections.emptyList();
		// TODO PROVIDE REFACTOR
//		RequestTree.simulate(ItemIdentifier.get(wanted).makeStack(wanted.getCount()), this, new RequestLog() {
//
//			@Override
//			public void handleMissingItems(List<IResource> items) {
//				missing.addAll(items);
//			}
//
//			@Override
//			public void handleSucessfullRequestOf(IResource item, TempOrders parts) {}
//
//			@Override
//			public void handleSucessfullRequestOfList(List<IResource> items, TempOrders parts) {
//				used.addAll(items);
//			}
//		});

		SimulationResult r = new SimulationResult();
//		r.used = resourcesToItemStacks(used.stream()).collect(Collectors.toList());
//		r.missing = resourcesToItemStacks(missing.stream()).collect(Collectors.toList());
		r.used = Collections.emptyList();
		r.missing = Collections.emptyList();
		return r;
	}

	@Override
	public List<ItemStack> performRequest(ItemStack wanted) {
		final List<IResource> missing = Collections.emptyList();
		// TODO PROVIDE REFACTOR
//		RequestTree.request(ItemIdentifier.get(wanted).makeStack(wanted.getCount()), this, new RequestLog() {
//
//			@Override
//			public void handleMissingItems(List<IResource> items) {
//				missing.addAll(items);
//			}
//
//			@Override
//			public void handleSucessfullRequestOf(IResource item, TempOrders parts) {}
//
//			@Override
//			public void handleSucessfullRequestOfList(List<IResource> items, TempOrders parts) {}
//		}, null);
//		return resourcesToItemStacks(missing.stream()).collect(Collectors.toList());
		return Collections.emptyList();
	}

//	private static Stream<ItemStack> resourcesToItemStacks(Stream<IResource> resourceStream) {
//		return resourceStream
//				.filter(resource -> resource instanceof ItemResource)
//				.map(resource -> ((ItemResource) resource).getItem().unsafeMakeNormalStack(resource.getAmount()));
//	}

	/* CC */
	@CCCommand(description = "Requests the given ItemIdentifierStack")
	@CCQueued
	public Object[] makeRequest(ItemIdentifierStack stack) throws Exception {
		return makeRequest(stack.getItem(), Double.valueOf(stack.getStackSize()), false);
	}

	@CCCommand(description = "Requests the given ItemIdentifierStack")
	@CCQueued
	public Object[] makeRequest(ItemIdentifierStack stack, Boolean forceCrafting) throws Exception {
		return makeRequest(stack.getItem(), Double.valueOf(stack.getStackSize()), forceCrafting);
	}

	@CCCommand(description = "Requests the given ItemIdentifier with the given amount")
	@CCQueued
	public Object[] makeRequest(ItemIdentifier item, Double amount) throws Exception {
		return makeRequest(item, amount, false);
	}

	@CCCommand(description = "Requests the given ItemIdentifier with the given amount")
	@CCQueued
	public Object[] makeRequest(ItemIdentifier item, Double amount, Boolean forceCrafting) throws Exception {
		if (forceCrafting == null) {
			forceCrafting = false;
		}
		if (item == null) {
			throw new Exception("Invalid ItemIdentifier");
		}
		// TODO PROVIDE REFACTOR
		//return RequestHandler.computerRequest(item.makeStack((int) Math.floor(amount)), this, forceCrafting);
		return new Object[0];
	}

	@CCCommand(description = "Asks for all available ItemIdentifier inside the Logistics Network")
	@CCQueued
	public List<Pair<ItemIdentifier, Integer>> getAvailableItems() {
		// TODO PROVIDE REFACTOR
		Map<ItemIdentifier, Integer> items = Collections.emptyMap(); // SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost());
		List<Pair<ItemIdentifier, Integer>> list = new LinkedList<>();
		for (Entry<ItemIdentifier, Integer> item : items.entrySet()) {
			int amount = item.getValue();
			list.add(new Pair<>(item.getKey(), amount));
		}
		return list;
	}

	@CCCommand(description = "Asks for all craftable ItemIdentifier inside the Logistics Network")
	@CCQueued
	public List<ItemIdentifier> getCraftableItems() {
		// TODO PROVIDE REFACTOR
		List<ItemIdentifier> items = Collections.emptyList(); // SimpleServiceLocator.logisticsManager.getCraftableItems(getRouter().getIRoutersByCost());
		return items;
	}

	@CCCommand(description = "Asks for the amount of an ItemIdentifier Id inside the Logistics Network")
	@CCQueued
	public int getItemAmount(ItemIdentifier item) throws Exception {
		// TODO PROVIDE REFACTOR
		Map<ItemIdentifier, Integer> items = Collections.emptyMap(); // SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost());
		if (item == null) {
			throw new Exception("Invalid ItemIdentifierID");
		}
		if (items.containsKey(item)) {
			return items.get(item);
		}
		return 0;
	}
}
