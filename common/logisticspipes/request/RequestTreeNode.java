package logisticspipes.request;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import kotlin.Unit;
import lombok.Getter;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraft;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree.ActiveRequestType;
import logisticspipes.request.RequestTree.workWeightedSorter;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.routing.order.LogisticsOrderManager;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.request.Request;

public class RequestTreeNode {

	protected final RequestTree root;
	@Getter
	private final IResource requestType;
	private final IAdditionalTargetInformation info;
	private final RequestTreeNode parentNode;
	private final Request request;
	private List<RequestTreeNode> subRequests = new ArrayList<>();
	private List<IPromise> promises = new ArrayList<>();
	private List<IExtraPromise> extrapromises = new ArrayList<>();
	private List<IExtraPromise> byproducts = new ArrayList<>();
	private SortedSet<ICraftingTemplate> usedCrafters = new TreeSet<>();
	private Set<LogisticsOrderManager<?, ?>> usedExtrasFromManager = new HashSet<LogisticsOrderManager<?, ?>>();
	private ICraftingTemplate lastCrafterTried = null;
	private int promiseAmount = 0;

	protected RequestTreeNode(IResource requestType, RequestTreeNode parentNode, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info) {
		this(null, requestType, parentNode, requestFlags, info);
	}

	private RequestTreeNode(ICraftingTemplate template, IResource requestType, RequestTreeNode parentNode, EnumSet<ActiveRequestType> requestFlags, IAdditionalTargetInformation info) {
		this.info = info;
		this.parentNode = parentNode;
		this.requestType = requestType;
		this.request = new Request(requestType, parentNode != null ? parentNode.request : null);
		if (parentNode != null) {
			parentNode.subRequests.add(this);
			root = parentNode.root;
		} else {
			root = (RequestTree) this;
		}
		if (template != null) {
			declareCrafterUsed(template);
		}

		if (requestFlags.contains(ActiveRequestType.Provide) && request.checkProvider(this::isDone, (provider, filters) -> {
			provider.canProvide(this, root, filters);
			return Unit.INSTANCE;
		})) {
			return;
		}

		if (requestFlags.contains(ActiveRequestType.Craft) && request.checkExtras(root, this::isDone, this::getMissingAmount, (promise) -> {
			this.addPromise(promise);
			return Unit.INSTANCE;
		})) {
			return;// crafting was able to complete
		}

		if (requestFlags.contains(ActiveRequestType.Craft) && request.checkCrafting(root, this::isDone, this::getMissingAmount, this::isCrafterUsed, this::getSubRequests, (promise) -> {
			this.addPromise(promise);
			return Unit.INSTANCE;
		})) {
			return;// crafting was able to complete
		}

		// crafting is not done!
	}

	protected static List<IResource> shrinkToList(Map<IResource, Integer> items) {
		List<IResource> resources = new ArrayList<>();
		outer:
			for (Entry<IResource, Integer> entry : items.entrySet()) {
				for (IResource resource : resources) {
					if (resource.mergeForDisplay(entry.getKey(), entry.getValue())) {
						continue outer;
					}
				}
				resources.add(entry.getKey().copyForDisplayWith(entry.getValue()));
			}
		return resources;
	}

	private boolean isCrafterUsed(ICraftingTemplate test) {
		if (!usedCrafters.isEmpty() && usedCrafters.contains(test)) {
			return true;
		}
		if (parentNode == null) {
			return false;
		}
		return parentNode.isCrafterUsed(test);
	}

	// returns false if the crafter was already on the list.
	private boolean declareCrafterUsed(ICraftingTemplate test) {
		if (isCrafterUsed(test)) {
			return false;
		}
		usedCrafters.add(test);
		return true;
	}

	public int getPromiseAmount() {
		return promiseAmount;
	}

	public int getMissingAmount() {
		return requestType.getRequestedAmount() - promiseAmount;
	}

	public void addPromise(IPromise promise) {
		if (!promise.matches(requestType)) {
			throw new IllegalArgumentException("wrong item");
		}
		if (getMissingAmount() == 0) {
			throw new IllegalArgumentException("zero count needed, promises not needed.");
		}
		if (promise.getAmount() > getMissingAmount()) {
			int more = promise.getAmount() - getMissingAmount();
			//promise.numberOfItems = getMissingAmount();
			//Add Extra
			//LogisticsExtraPromise extra = new LogisticsExtraPromise(promise.item, more, promise.sender, false);
			extrapromises.add(promise.split(more));
		}
		if (promise.getAmount() <= 0) {
			throw new IllegalArgumentException("zero count ... again");
		}
		promises.add(promise);
		promiseAmount += promise.getAmount();
		root.promiseAdded(promise);
	}

	public boolean isDone() {
		return getMissingAmount() <= 0;
	}

	protected void remove(RequestTreeNode subNode) {
		subRequests.remove(subNode);
		subNode.removeSubPromisses();
	}

	/* RequestTree helpers */
	protected void removeSubPromisses() {
		promises.forEach(root::promiseRemoved);
		subRequests.forEach(RequestTreeNode::removeSubPromisses);
	}

	protected void checkForExtras(IResource item, HashMap<IProvide, List<IExtraPromise>> extraMap) {
		for (IExtraPromise extra : extrapromises) {
			if (item.matches(extra.getItemType(), IResource.MatchSettings.NORMAL)) {
				List<IExtraPromise> extras = extraMap.get(extra.getProvider());
				if (extras == null) {
					extras = new LinkedList<>();
					extraMap.put(extra.getProvider(), extras);
				}
				extras.add(extra.copy());
			}
		}
		for (RequestTreeNode subNode : subRequests) {
			subNode.checkForExtras(item, extraMap);
		}
	}

	protected void removeUsedExtras(IResource item, HashMap<IProvide, List<IExtraPromise>> extraMap) {
		for (IPromise promise : promises) {
			if (!item.matches(promise.getItemType(), IResource.MatchSettings.NORMAL)) {
				continue;
			}
			if (!(promise instanceof IExtraPromise)) {
				continue;
			}
			IExtraPromise epromise = (IExtraPromise) promise;
			if (epromise.isProvided()) {
				continue;
			}
			int usedcount = epromise.getAmount();
			List<IExtraPromise> extras = extraMap.get(epromise.getProvider());
			if (extras == null) {
				continue;
			}
			for (Iterator<IExtraPromise> it = extras.iterator(); it.hasNext();) {
				IExtraPromise extra = it.next();
				if (extra.getAmount() >= usedcount) {
					extra.lowerAmount(usedcount);
					usedcount = 0;
					break;
				} else {
					usedcount -= extra.getAmount();
					it.remove();
				}
			}
		}
		for (RequestTreeNode subNode : subRequests) {
			subNode.removeUsedExtras(item, extraMap);
		}
	}

	protected LinkedLogisticsOrderList fullFill() {
		LinkedLogisticsOrderList list = new LinkedLogisticsOrderList();
		for (RequestTreeNode subNode : subRequests) {
			list.getSubOrders().add(subNode.fullFill());
		}
		for (IPromise promise : promises) {
			IOrderInfoProvider result = promise.fullFill(requestType, info);
			if (result != null) {
				list.add(result);
			}
		}
		for (IExtraPromise promise : extrapromises) {
			promise.registerExtras(requestType);
		}
		for (IExtraPromise promise : byproducts) {
			promise.registerExtras(requestType);
		}
		return list;
	}

	protected void buildMissingMap(Map<IResource, Integer> missing) {
		if (getMissingAmount() != 0) {
			Integer count = missing.get(getRequestType());
			if (count == null) {
				count = 0;
			}
			count += getMissingAmount();
			missing.put(getRequestType(), count);
		}
		for (RequestTreeNode subNode : subRequests) {
			subNode.buildMissingMap(missing);
		}
	}

	protected void buildUsedMap(Map<IResource, Integer> used, Map<IResource, Integer> missing) {
		int usedcount = 0;
		for (IPromise promise : promises) {
			if (promise.getType() == ResourceType.PROVIDER) {
				usedcount += promise.getAmount();
			}
		}
		if (usedcount != 0) {
			Integer count = used.get(getRequestType());
			if (count == null) {
				count = 0;
			}
			count += usedcount;
			used.put(getRequestType(), count);
		}
		if (getMissingAmount() != 0) {
			Integer count = missing.get(getRequestType());
			if (count == null) {
				count = 0;
			}
			count += getMissingAmount();
			missing.put(getRequestType(), count);
		}
		for (RequestTreeNode subNode : subRequests) {
			subNode.buildUsedMap(used, missing);
		}
	}

	public boolean hasBeenQueried(LogisticsOrderManager<?, ?> orderManager) {
		return usedExtrasFromManager.contains(orderManager);
	}

	public void setQueried(LogisticsOrderManager<?, ?> orderManager) {
		usedExtrasFromManager.add(orderManager);
	}

	private int getSubRequests(int nCraftingSets, ICraftingTemplate template) {
		boolean failed = false;
		List<Pair<IResource, IAdditionalTargetInformation>> stacks = template.getComponents(nCraftingSets);
		int workSetsAvailable = nCraftingSets;
		ArrayList<RequestTreeNode> lastNodes = new ArrayList<>(stacks.size());
		for (Pair<IResource, IAdditionalTargetInformation> stack : stacks) {
			RequestTreeNode node = new RequestTreeNode(template, stack.getValue1(), this, RequestTree.defaultRequestFlags, stack.getValue2());
			lastNodes.add(node);
			if (!node.isDone()) {
				failed = true;
			}
		}
		if (failed) {
			// drop the failed requests.
			lastNodes.forEach(RequestTreeNode::destroy);
			//save last tried template for filling out the tree
			lastCrafterTried = template;
			//figure out how many we can actually get
			for (int i = 0; i < stacks.size(); i++) {
				workSetsAvailable = Math.min(workSetsAvailable, lastNodes.get(i).getPromiseAmount() / (stacks.get(i).getValue1().getRequestedAmount() / nCraftingSets));
			}

			return generateRequestTreeFor(workSetsAvailable, template);
		}
		byproducts.addAll(template.getByproducts(workSetsAvailable).stream().collect(Collectors.toList()));
		return workSetsAvailable;
	}

	private int generateRequestTreeFor(int workSets, ICraftingTemplate template) {
		//and try it
		ArrayList<RequestTreeNode> newChildren = new ArrayList<>();
		if (workSets > 0) {
			//now set the amounts
			List<Pair<IResource, IAdditionalTargetInformation>> stacks = template.getComponents(workSets);
			boolean failed = false;
			for (Pair<IResource, IAdditionalTargetInformation> stack : stacks) {
				RequestTreeNode node = new RequestTreeNode(template, stack.getValue1(), this, RequestTree.defaultRequestFlags, stack.getValue2());
				newChildren.add(node);
				if (!node.isDone()) {
					failed = true;
				}
			}
			if (failed) {
				newChildren.forEach(RequestTreeNode::destroy);
				return 0;
			}
		}
		byproducts.addAll(template.getByproducts(workSets).stream().collect(Collectors.toList()));
		return workSets;
	}

	void recurseFailedRequestTree() {
		if (isDone()) {
			return;
		}
		if (lastCrafterTried == null) {
			return;
		}

		ICraftingTemplate template = lastCrafterTried;

		int nCraftingSetsNeeded = (getMissingAmount() + template.getResultStackSize() - 1) / template.getResultStackSize();

		List<Pair<IResource, IAdditionalTargetInformation>> stacks = template.getComponents(nCraftingSetsNeeded);

		for (Pair<IResource, IAdditionalTargetInformation> stack : stacks) {
			new RequestTreeNode(template, stack.getValue1(), this, RequestTree.defaultRequestFlags, stack.getValue2());
		}

		addPromise(template.generatePromise(nCraftingSetsNeeded));

		subRequests.forEach(RequestTreeNode::recurseFailedRequestTree);
	}

	protected void logFailedRequestTree(RequestLog log) {
		Map<IResource, Integer> missing = new HashMap<>();
		subRequests.stream().filter(node -> node instanceof RequestTree).filter(node -> !node.isDone())
				.forEach(node -> {
					node.recurseFailedRequestTree();
					node.buildMissingMap(missing);
				});
		log.handleMissingItems(RequestTreeNode.shrinkToList(missing));
	}

	private void destroy() {
		parentNode.remove(this);
	}
}
