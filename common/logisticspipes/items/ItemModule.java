package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.registries.IForgeRegistry;

import org.lwjgl.input.Keyboard;

import logisticspipes.LPConstants;
import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.AbstractModule;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleEnchantmentSink;
import logisticspipes.modules.ModuleEnchantmentSinkMK2;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.StringUtils;
import network.rs485.logisticspipes.api.LogisticsModule;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.module.AsyncExtractorModule;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;
import network.rs485.logisticspipes.module.Gui;

public class ItemModule extends LogisticsItem {

	private static class ModuleFactory<T extends LogisticsModule> {

		private final Supplier<T> moduleConstructor;

		private ModuleFactory(@Nonnull Supplier<T> moduleConstructor) {
			this.moduleConstructor = Objects.requireNonNull(moduleConstructor);
		}

		private LogisticsModule construct() {
			return moduleConstructor.get();
		}

	}

	private final ModuleFactory<?> moduleFactory;

	public ItemModule(ModuleFactory<?> moduleType) {
		super();
		this.moduleFactory = moduleType;
		setHasSubtypes(false);
	}

	public static void loadModules(IForgeRegistry<Item> registry) {
		registerModule(registry, ModuleItemSink.getName(), ModuleItemSink::new);
		registerModule(registry, ModulePassiveSupplier.getName(), ModulePassiveSupplier::new);
		registerModule(registry, AsyncExtractorModule.getName(), AsyncExtractorModule::new);
		registerModule(registry, ModulePolymorphicItemSink.getName(), ModulePolymorphicItemSink::new);
		registerModule(registry, AsyncQuicksortModule.getName(), AsyncQuicksortModule::new);
		registerModule(registry, ModuleTerminus.getName(), ModuleTerminus::new);
		registerModule(registry, AsyncAdvancedExtractor.getName(), AsyncAdvancedExtractor::new);
		registerModule(registry, ModuleProvider.getName(), ModuleProvider::new);
		registerModule(registry, ModuleModBasedItemSink.getName(), ModuleModBasedItemSink::new);
		registerModule(registry, ModuleOreDictItemSink.getName(), ModuleOreDictItemSink::new);
		registerModule(registry, ModuleEnchantmentSink.getName(), ModuleEnchantmentSink::new);
		registerModule(registry, ModuleEnchantmentSinkMK2.getName(), ModuleEnchantmentSinkMK2::new);
		//registerModule(registry, "quick_sort_cc", ModuleCCBasedQuickSort::new);
		//registerModule(registry, "item_sink_cc", ModuleCCBasedItemSink::new);
		registerModule(registry, ModuleCrafter.getName(), ModuleCrafter::new);
		registerModule(registry, ModuleActiveSupplier.getName(), ModuleActiveSupplier::new);
		registerModule(registry, ModuleCreativeTabBasedItemSink.getName(), ModuleCreativeTabBasedItemSink::new);
	}

	public static void registerModule(IForgeRegistry<Item> registry, String name, @Nonnull Supplier<? extends LogisticsModule> moduleConstructor) {
		registerModule(registry, name, moduleConstructor, LPConstants.LP_MOD_ID);
	}

	public static void registerModule(IForgeRegistry<Item> registry, String name, @Nonnull Supplier<? extends LogisticsModule> moduleConstructor, String modID) {
		ItemModule module = LogisticsPipes.setName(new ItemModule(new ModuleFactory<>(moduleConstructor)), String.format("module_%s", name), modID);
		LPItems.modules.put(name, module.getRegistryName());
		registry.register(module);
	}

	private void openConfigGui(@Nonnull ItemStack stack, EntityPlayer player, World world) {
		LogisticsModule module = getModuleForItem(stack, null, null, null);
		if (module instanceof Gui && !stack.isEmpty()) {
			ItemModuleInformationManager.readInformation(stack, module);
			//module.registerPosition(ModulePositionType.IN_HAND, player.inventory.currentItem);
			Gui.getInHandGuiProvider((Gui) module).open(player);
		}
	}

	@Override
	public boolean hasEffect(@Nonnull ItemStack stack) {
		if (stack.isEmpty()) return false;
		LogisticsModule module = getModuleForItem(stack, null, null, null);
		if (module instanceof AbstractModule) {
			return ((AbstractModule) module).hasEffect();
		}
		return false;
	}

	@Override
	@Nonnull
	public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, @Nonnull final EnumHand hand) {
		if (MainProxy.isServer(player.world)) {
			openConfigGui(player.getHeldItem(hand), player, world);
		}
		return super.onItemRightClick(world, player, hand);
	}

	@Override
	@Nonnull
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (MainProxy.isServer(player.world)) {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof LogisticsTileGenericPipe) {
				if (player.getDisplayName().getUnformattedText().equals("ComputerCraft")) { // Allow turtle to place modules in pipes.
					CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(world, pos);
					if (LogisticsBlockGenericPipe.isValid(pipe)) {
						pipe.blockActivated(player);
					}
				}
				return EnumActionResult.PASS;
			}
			openConfigGui(player.inventory.getCurrentItem(), player, world);
		}
		return EnumActionResult.PASS;
	}

	@Nullable
	public LogisticsModule getModuleForItem(@Nonnull ItemStack itemStack, LogisticsModule currentModule, IWorldProvider world, IPipeServiceProvider service) {
		if (itemStack.isEmpty()) {
			return null;
		}
		if (itemStack.getItem() != this) {
			return null;
		}
		/*
		if (currentModule != null) {
			if (moduleFactory.getILogisticsModuleClass().equals(currentModule.getClass())) {
				return currentModule;
			}
		}
		*/
		LogisticsModule newmodule = moduleFactory.construct();
		/*
		if (newmodule == null) {
			return null;
		}
		newmodule.registerHandler(world, service);
		 */
		return newmodule;
	}

	@Override
	public String getModelSubdir() {
		return "module";
	}

	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		if (stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
			assert nbt != null;

			if (nbt.hasKey("informationList")) {
				if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
					NBTTagList nbttaglist = nbt.getTagList("informationList", 8);
					for (int i = 0; i < nbttaglist.tagCount(); i++) {
						Object nbttag = nbttaglist.get(i);
						String data = ((NBTTagString) nbttag).getString();
						if (data.equals("<inventory>") && i + 1 < nbttaglist.tagCount()) {
							nbttag = nbttaglist.get(i + 1);
							data = ((NBTTagString) nbttag).getString();
							if (data.startsWith("<that>")) {
								String prefix = data.substring(6);
								NBTTagCompound module = nbt.getCompoundTag("moduleInformation");
								int size = module.getTagList(prefix + "items", module.getId()).tagCount();
								if (module.hasKey(prefix + "itemsCount")) {
									size = module.getInteger(prefix + "itemsCount");
								}
								ItemIdentifierInventory inv = new ItemIdentifierInventory(size, "InformationTempInventory", Integer.MAX_VALUE);
								inv.readFromNBT(module, prefix);
								for (int pos = 0; pos < inv.getSizeInventory(); pos++) {
									ItemIdentifierStack identStack = inv.getIDStackInSlot(pos);
									if (identStack != null) {
										if (identStack.getStackSize() > 1) {
											tooltip.add("  " + identStack.getStackSize() + "x " + identStack.getFriendlyName());
										} else {
											tooltip.add("  " + identStack.getFriendlyName());
										}
									}
								}
							}
							i++;
						} else {
							tooltip.add(data);
						}
					}
				} else {
					tooltip.add(StringUtils.translate(StringUtils.KEY_HOLDSHIFT));
				}
			} else {
				StringUtils.addShiftAddition(stack, tooltip);
			}
		} else {
			StringUtils.addShiftAddition(stack, tooltip);
		}
	}
}
