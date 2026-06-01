package com.shiver.modularmachineryterminal.server;

import com.shiver.modularmachineryterminal.common.MachineAccess;
import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.OutputInfo;
import com.shiver.modularmachineryterminal.common.SmartInterfaceInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import com.shiver.modularmachineryterminal.common.ThreadInfo;
import com.shiver.modularmachineryterminal.network.PacketDynamic;
import com.shiver.modularmachineryterminal.network.PacketFullList;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MachineCache {

    private static final Map<MachineKey, CachedMachine> CACHE = new LinkedHashMap<>();
    private static final int DISCOVERY_SCAN_INTERVAL_TICKS = 100;
    private static boolean persistedLoaded;
    private static boolean discoveryDirty = true;
    private static int lastDiscoveryScanTick = -DISCOVERY_SCAN_INTERVAL_TICKS;
    private static MinecraftServer loadedServer;

    @SubscribeEvent
    public void onMachineTick(MachineTickEvent event) {
        update(event.getController(), true);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        MachineKey key = new MachineKey(event.getWorld().provider.getDimension(), event.getPos());
        removeMachine(key);
        discoveryDirty = true;
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        scheduleTrackPosition((WorldServer) event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public void onMultiBlockPlace(BlockEvent.MultiPlaceEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        WorldServer world = (WorldServer) event.getWorld();
        for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            scheduleTrackPosition(world, snapshot.getPos());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof WorldServer) || event.getWorld().isRemote || event.getChunk() == null) {
            return;
        }
        WorldServer world = (WorldServer) event.getWorld();
        Chunk chunk = event.getChunk();
        discoveryDirty = true;
        scanChunk(world, chunk, null);
        world.addScheduledTask(() -> {
            scanChunk(world, chunk, null);
            validatePersistedInChunk(world, chunk.x, chunk.z);
        });
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getWorld() == null || event.getWorld().isRemote || event.getChunk() == null) {
            return;
        }

        int dimension = event.getWorld().provider.getDimension();
        int chunkX = event.getChunk().x;
        int chunkZ = event.getChunk().z;
        for (Map.Entry<MachineKey, CachedMachine> entry : CACHE.entrySet()) {
            MachineKey key = entry.getKey();
            if (key.dimension == dimension && (key.pos.getX() >> 4) == chunkX && (key.pos.getZ() >> 4) == chunkZ) {
                markUnloaded(entry.getValue());
            }
        }
    }

    public static PacketFullList createFullListPacket(EntityPlayerMP player, boolean includeTeamControllers) {
        MinecraftServer server = player.getServer();
        loadPersistedIfNeeded(server);
        refreshLoadedMachinesIfDue(server, false);
        List<MachineInfo> machines = new ArrayList<>();
        for (CachedMachine cached : CACHE.values()) {
            if (!visibleTo(cached, player, includeTeamControllers)) {
                continue;
            }
            machines.add(cached.info);
        }
        return new PacketFullList(createSummary(player, includeTeamControllers), machines);
    }

    public static PacketDynamic createDynamicPacket(EntityPlayerMP player, List<MachineKey> keys, boolean includeTeamControllers) {
        MinecraftServer server = player.getServer();
        loadPersistedIfNeeded(server);
        List<MachineInfo> machines = new ArrayList<>();
        List<MachineKey> removed = new ArrayList<>();
        for (MachineKey key : keys) {
            if (key == null) {
                continue;
            }
            refreshKey(server, key);
            CachedMachine cached = CACHE.get(key);
            if (visibleTo(cached, player, includeTeamControllers)) {
                machines.add(cached.info);
            } else {
                removed.add(key);
            }
        }
        return new PacketDynamic(createSummary(player, includeTeamControllers), machines, removed);
    }

    private static SummaryInfo createSummary(EntityPlayerMP player, boolean includeTeamControllers) {
        SummaryInfo summary = new SummaryInfo();
        for (CachedMachine cached : CACHE.values()) {
            if (!visibleTo(cached, player, includeTeamControllers)) {
                continue;
            }
            MachineInfo info = cached.info;
            summary.total++;
            if (info.loaded) {
                summary.loaded++;
            }
            if (info.loaded && info.formed) {
                summary.formed++;
            }
            if (info.loaded && info.running) {
                summary.running++;
            }
        }
        return summary;
    }

    private static void refreshLoadedMachinesIfDue(MinecraftServer server, boolean force) {
        if (server == null) {
            return;
        }
        int tick = server.getTickCounter();
        boolean due = tick - lastDiscoveryScanTick >= DISCOVERY_SCAN_INTERVAL_TICKS;
        if (!force && !due) {
            return;
        }
        refreshLoadedMachines(server);
        lastDiscoveryScanTick = tick;
        discoveryDirty = false;
    }

    private static void refreshLoadedMachines(MinecraftServer server) {
        Set<MachineKey> foundLoaded = new HashSet<>();

        if (server == null) {
            return;
        }

        for (WorldServer world : server.worlds) {
            scanWorld(world, foundLoaded);
        }

        List<MachineKey> remove = new ArrayList<>();
        for (Map.Entry<MachineKey, CachedMachine> entry : CACHE.entrySet()) {
            if (!foundLoaded.contains(entry.getKey())) {
                if (isCachedPositionLoaded(server, entry.getKey())) {
                    remove.add(entry.getKey());
                } else {
                    markUnloaded(entry.getValue());
                }
            }
        }
        for (MachineKey key : remove) {
            removeMachine(key);
        }
    }

    private static void scanWorld(WorldServer world, Set<MachineKey> foundLoaded) {
        for (Chunk chunk : new ArrayList<>(world.getChunkProvider().getLoadedChunks())) {
            scanChunk(world, chunk, foundLoaded);
        }

        for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
            trackTile(tile, foundLoaded);
        }
    }

    private static void scanChunk(WorldServer world, Chunk chunk, Set<MachineKey> foundLoaded) {
        if (world == null || chunk == null) {
            return;
        }
        for (TileEntity tile : new ArrayList<>(chunk.getTileEntityMap().values())) {
            trackTile(tile, foundLoaded);
        }
    }

    private static void trackTile(TileEntity tile, Set<MachineKey> foundLoaded) {
        if (!(tile instanceof TileMultiblockMachineController)) {
            return;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
        MachineKey key = new MachineKey(controller.getWorld().provider.getDimension(), controller.getPos());
        if (update(controller, true)) {
            if (foundLoaded != null) {
                foundLoaded.add(key);
            }
        }
    }

    private static void markUnloaded(CachedMachine cached) {
        if (cached == null || cached.info == null) {
            return;
        }
        cached.info.loaded = false;
        cached.info.running = false;
        cached.info.activeThreads = 0;
        cached.info.parallelism = 0;
        cached.info.energyPerTick = 0;
        cached.info.threads.clear();
    }

    private static boolean isCachedPositionLoaded(MinecraftServer server, MachineKey key) {
        WorldServer world = server.getWorld(key.dimension);
        int chunkX = key.pos.getX() >> 4;
        int chunkZ = key.pos.getZ() >> 4;
        return world.getChunkProvider().getLoadedChunk(chunkX, chunkZ) != null;
    }

    private static void refreshKey(MinecraftServer server, MachineKey key) {
        if (server == null || key == null) {
            return;
        }
        WorldServer world = server.getWorld(key.dimension);
        if (world == null) {
            return;
        }
        if (!isCachedPositionLoaded(server, key)) {
            CachedMachine cached = CACHE.get(key);
            if (cached != null) {
                markUnloaded(cached);
            }
            return;
        }
        TileEntity tile = world.getTileEntity(key.pos);
        if (tile instanceof TileMultiblockMachineController) {
            update((TileMultiblockMachineController) tile, true);
        } else {
            removeMachine(key);
        }
    }

    private static void scheduleTrackPosition(WorldServer world, BlockPos pos) {
        discoveryDirty = true;
        trackPosition(world, pos);
        world.addScheduledTask(() -> trackPosition(world, pos));
    }

    private static void trackPosition(WorldServer world, BlockPos pos) {
        if (world == null || pos == null) {
            return;
        }
        trackTile(world.getTileEntity(pos), null);
    }

    private static void validatePersistedInChunk(WorldServer world, int chunkX, int chunkZ) {
        MinecraftServer server = world.getMinecraftServer();
        loadPersistedIfNeeded(server);
        TerminalMachineSavedData savedData = savedData(server);
        if (savedData == null) {
            return;
        }
        List<MachineKey> remove = new ArrayList<>();
        for (MachineKey key : savedData.entries().keySet()) {
            if (key.dimension == world.provider.getDimension()
                    && (key.pos.getX() >> 4) == chunkX
                    && (key.pos.getZ() >> 4) == chunkZ
                    && !(world.getTileEntity(key.pos) instanceof TileMultiblockMachineController)) {
                remove.add(key);
            }
        }
        for (MachineKey key : remove) {
            removeMachine(key);
        }
    }

    private static void loadPersistedIfNeeded(MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (loadedServer != server) {
            CACHE.clear();
            persistedLoaded = false;
            discoveryDirty = true;
            lastDiscoveryScanTick = -DISCOVERY_SCAN_INTERVAL_TICKS;
            loadedServer = server;
        }
        if (persistedLoaded) {
            return;
        }
        TerminalMachineSavedData savedData = savedData(server);
        if (savedData == null) {
            return;
        }
        for (Map.Entry<MachineKey, TerminalMachineSavedData.PersistedMachine> entry : savedData.entries().entrySet()) {
            if (CACHE.containsKey(entry.getKey())) {
                continue;
            }
            CachedMachine cached = new CachedMachine();
            cached.owner = entry.getValue().owner;
            cached.info = entry.getValue().toUnloadedInfo();
            CACHE.put(entry.getKey(), cached);
        }
        persistedLoaded = true;
    }

    private static TerminalMachineSavedData savedData(MinecraftServer server) {
        return TerminalMachineSavedData.get(server);
    }

    private static void removeMachine(MachineKey key) {
        if (key == null) {
            return;
        }
        CACHE.remove(key);
        TerminalMachineSavedData savedData = savedData(FMLCommonHandler.instance().getMinecraftServerInstance());
        if (savedData != null) {
            savedData.remove(key);
        }
    }

    private static boolean update(TileMultiblockMachineController controller, boolean loaded) {
        if (controller == null || controller.getWorld() == null) {
            return false;
        }

        MachineKey key = new MachineKey(controller.getWorld().provider.getDimension(), controller.getPos());
        CachedMachine cached = CACHE.computeIfAbsent(key, ignored -> new CachedMachine());
        cached.owner = controller.getOwner();
        cached.info = capture(controller, loaded);
        TerminalMachineSavedData savedData = savedData(controller.getWorld().getMinecraftServer());
        if (savedData != null) {
            savedData.put(cached.info, cached.owner);
        }
        return true;
    }

    private static boolean visibleTo(CachedMachine cached, EntityPlayerMP player, boolean includeTeamControllers) {
        return cached != null && MachineAccess.canAccess(player, cached.owner, includeTeamControllers);
    }

    private static MachineInfo capture(TileMultiblockMachineController controller, boolean loaded) {
        MachineInfo info = new MachineInfo();
        info.key = new MachineKey(controller.getWorld().provider.getDimension(), controller.getPos());
        info.name = machineName(controller);
        info.controllerIcon = controllerIcon(controller);
        info.loaded = loaded;
        info.formed = loaded && controller.isStructureFormed();
        info.running = loaded && controller.isWorking();
        info.status = statusText(controller.getControllerStatus());

        RecipeThread[] recipeThreads = controller.getRecipeThreadList();
        info.maxThreads = maxThreads(controller, recipeThreads);
        if (recipeThreads != null) {
            int index = 1;
            for (RecipeThread thread : recipeThreads) {
                ThreadInfo threadInfo = captureThread(index++, thread);
                if (threadInfo.working) {
                    info.activeThreads++;
                }
                info.energyPerTick += energyFlowPerTick(thread);
                info.parallelism += threadInfo.parallelism;
                info.maxParallelism += threadInfo.maxParallelism;
                if (info.output.type == OutputInfo.Type.NONE && threadInfo.output.type != OutputInfo.Type.NONE) {
                    info.output = threadInfo.output;
                }
                info.threads.add(threadInfo);
            }
        }

        if (info.maxParallelism <= 0) {
            info.maxParallelism = safeMaxParallelism(controller);
        }
        if (info.output.type == OutputInfo.Type.NONE && controller.getActiveRecipe() != null) {
            info.output = firstOutput(controller.getActiveRecipe());
        }
        captureSmartInterfaces(controller, info);
        return info;
    }

    private static void captureSmartInterfaces(TileMultiblockMachineController controller, MachineInfo info) {
        Map<TileSmartInterface.SmartInterfaceProvider, String> smartInterfaces = controller.getFoundSmartInterfaces();
        if (smartInterfaces == null || smartInterfaces.isEmpty()) {
            return;
        }
        for (Map.Entry<TileSmartInterface.SmartInterfaceProvider, String> entry : smartInterfaces.entrySet()) {
            TileSmartInterface.SmartInterfaceProvider provider = entry.getKey();
            TileSmartInterface smartInterface = smartInterfaceTile(provider);
            if (provider == null || smartInterface == null) {
                continue;
            }
            for (int i = 0; i < provider.getBoundSize(); i++) {
                SmartInterfaceData data = provider.getMachineData(i);
                if (data == null || !controller.getPos().equals(data.getPos())) {
                    continue;
                }
                SmartInterfaceInfo smartInfo = new SmartInterfaceInfo();
                smartInfo.interfacePos = smartInterface.getPos();
                smartInfo.dataIndex = i;
                smartInfo.type = data.getType();
                smartInfo.parentMachineName = data.getParentMachineName();
                smartInfo.value = data.getValue();
                info.smartInterfaces.add(smartInfo);
            }
        }
    }

    private static TileSmartInterface smartInterfaceTile(TileSmartInterface.SmartInterfaceProvider provider) {
        try {
            Field parent = provider.getClass().getDeclaredField("parent");
            parent.setAccessible(true);
            Object tile = parent.get(provider);
            return tile instanceof TileSmartInterface ? (TileSmartInterface) tile : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ThreadInfo captureThread(int index, RecipeThread thread) {
        ThreadInfo info = new ThreadInfo();
        info.name = "Thread " + index;
        if (thread == null) {
            info.status = "";
            return info;
        }

        ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
        CraftingStatus status = thread.getStatus();
        info.status = statusText(status);
        info.working = activeRecipe != null && status != null && status.isCrafting();
        if (activeRecipe != null) {
            info.parallelism = Math.max(0, activeRecipe.getParallelism());
            info.maxParallelism = Math.max(0, activeRecipe.getMaxParallelism());
            info.progress = recipeProgress(activeRecipe);
            info.output = firstOutput(activeRecipe);
            MachineRecipe recipe = activeRecipe.getRecipe();
            if (recipe != null && recipe.getThreadName() != null && !recipe.getThreadName().isEmpty()) {
                info.name = recipe.getThreadName();
            }
        }
        return info;
    }

    private static int maxThreads(TileMultiblockMachineController controller, RecipeThread[] threads) {
        if (controller instanceof TileFactoryController) {
            return safeMaxThreads((TileFactoryController) controller);
        }
        return threads == null ? 0 : threads.length;
    }

    private static int safeMaxThreads(TileFactoryController controller) {
        try {
            return Math.max(0, controller.getMaxThreads());
        } catch (NullPointerException ignored) {
            return 0;
        }
    }

    private static String machineName(TileMultiblockMachineController controller) {
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null && controller instanceof TileMachineController) {
            machine = ((TileMachineController) controller).getParentMachine();
        }
        if (machine != null) {
            String localized = machine.getOriginalLocalizedName();
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
            ResourceLocation rl = machine.getRegistryName();
            String key = rl.getNamespace() + "." + rl.getPath();
            if (I18n.canTranslate(key)) {
                return I18n.translateToLocal(key);
            }
            return rl.toString();
        }
        String formedName = controller.getFormedMachineName();
        return formedName == null || formedName.isEmpty() ? "Unknown Machine" : formedName;
    }

    private static ItemStack controllerIcon(TileMultiblockMachineController controller) {
        Block block = controller.getWorld().getBlockState(controller.getPos()).getBlock();
        Item item = Item.getItemFromBlock(block);
        return new ItemStack(item, 1, block.damageDropped(controller.getWorld().getBlockState(controller.getPos())));
    }

    private static String statusText(CraftingStatus status) {
        if (status == null) {
            return "";
        }
        String text = status.getUnlocMessage();
        return text == null ? "" : text;
    }

    private static int safeMaxParallelism(TileMultiblockMachineController controller) {
        try {
            return Math.max(0, controller.getMaxParallelism());
        } catch (NullPointerException ignored) {
            return 0;
        }
    }

    private static int recipeProgress(ActiveMachineRecipe activeRecipe) {
        int totalTick = activeRecipe.getTotalTick();
        if (totalTick <= 0) {
            return 0;
        }
        int tick = activeRecipe.getTick();
        return Math.max(0, Math.min(100, tick * 100 / totalTick));
    }

    private static long energyFlowPerTick(RecipeThread thread) {
        if (thread == null || thread.getActiveRecipe() == null) {
            return 0;
        }
        RecipeCraftingContext context = thread.getContext();
        if (context == null) {
            return 0;
        }
        return energyPerTick(context, IOType.OUTPUT) - energyPerTick(context, IOType.INPUT);
    }

    private static long energyPerTick(RecipeCraftingContext context, IOType ioType) {
        long total = 0;
        List<ComponentRequirement<?, ?>> requirements = context.getRequirementBy(RequirementTypesMM.REQUIREMENT_ENERGY, ioType);
        for (ComponentRequirement<?, ?> requirement : requirements) {
            if (requirement instanceof RequirementEnergy) {
                total += energyPerTick(context, (RequirementEnergy) requirement);
            }
        }
        return total;
    }

    private static long energyPerTick(RecipeCraftingContext context, RequirementEnergy energy) {
        long perTick = energy.getRequiredEnergyPerTick();
        return Math.round(RecipeModifier.applyModifiers(context, energy, (double) perTick, false)
                * context.getDurationMultiplier()
                * energy.getParallelism());
    }

    private static OutputInfo firstOutput(ActiveMachineRecipe activeRecipe) {
        if (activeRecipe == null || activeRecipe.getRecipe() == null) {
            return OutputInfo.none();
        }
        for (ComponentRequirement<?, ?> requirement : activeRecipe.getRecipe().getCraftingRequirements()) {
            if (requirement.getActionType() != IOType.OUTPUT) {
                continue;
            }
            OutputInfo output = outputFromRequirement(requirement);
            if (output.type != OutputInfo.Type.NONE) {
                return output;
            }
        }
        return OutputInfo.none();
    }

    private static OutputInfo outputFromRequirement(ComponentRequirement<?, ?> requirement) {
        if (requirement instanceof RequirementItem) {
            RequirementItem item = (RequirementItem) requirement;
            if (item.required != null && !item.required.isEmpty()) {
                return OutputInfo.item(item.required);
            }
            if (item.previewItemStacks != null && !item.previewItemStacks.isEmpty()) {
                return OutputInfo.item(item.previewItemStacks.get(0));
            }
            if (item.oreDictName != null && !item.oreDictName.isEmpty()) {
                return OutputInfo.text(OutputInfo.Type.TEXT, item.oreDictName);
            }
        }
        if (requirement instanceof RequirementFluid) {
            return OutputInfo.fluid(((RequirementFluid) requirement).required);
        }
        if ("hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas".equals(requirement.getClass().getName())) {
            Object gasStack = fieldValue(requirement, "required");
            return OutputInfo.text(OutputInfo.Type.GAS, gasName(gasStack));
        }
        return OutputInfo.text(OutputInfo.Type.TEXT, requirement.getRequirementType().getClass().getSimpleName());
    }

    private static Object fieldValue(Object target, String name) {
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String gasName(Object gasStack) {
        if (gasStack == null) {
            return "";
        }
        try {
            Method getGas = gasStack.getClass().getMethod("getGas");
            Object gas = getGas.invoke(gasStack);
            if (gas != null) {
                Method localizedName = gas.getClass().getMethod("getLocalizedName");
                Object name = localizedName.invoke(gas);
                return String.valueOf(name);
            }
        } catch (Exception ignored) {
        }
        try {
            Field gasField = gasStack.getClass().getField("gas");
            Object gas = gasField.get(gasStack);
            return String.valueOf(gas);
        } catch (Exception ignored) {
        }
        return gasStack.toString();
    }

    private static class CachedMachine {
        private UUID owner;
        private MachineInfo info;
    }
}
