package com.shiver.modularmachineryterminal.server;

import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.OutputInfo;
import com.shiver.modularmachineryterminal.common.SummaryInfo;
import com.shiver.modularmachineryterminal.common.ThreadInfo;
import com.shiver.modularmachineryterminal.network.PacketDynamic;
import com.shiver.modularmachineryterminal.network.PacketFullList;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MachineCache {

    private static final Map<MachineKey, CachedMachine> CACHE = new LinkedHashMap<>();

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
        CACHE.remove(key);
    }

    public static PacketFullList createFullListPacket(EntityPlayerMP player) {
        refreshLoadedMachines();
        List<MachineInfo> machines = new ArrayList<>();
        SummaryInfo summary = new SummaryInfo();
        UUID playerId = player.getUniqueID();
        for (CachedMachine cached : CACHE.values()) {
            if (!playerId.equals(cached.owner)) {
                continue;
            }
            MachineInfo info = cached.info;
            machines.add(info);
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
        return new PacketFullList(summary, machines);
    }

    public static PacketDynamic createDynamicPacket(EntityPlayerMP player, List<MachineKey> keys) {
        refreshLoadedMachines();
        List<MachineInfo> machines = new ArrayList<>();
        UUID playerId = player.getUniqueID();
        for (MachineKey key : keys) {
            CachedMachine cached = CACHE.get(key);
            if (cached != null && playerId.equals(cached.owner)) {
                machines.add(cached.info);
            }
        }
        return new PacketDynamic(machines);
    }

    private static void refreshLoadedMachines() {
        Set<MachineKey> foundLoaded = new HashSet<>();

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
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
            CACHE.remove(key);
        }
    }

    private static void scanWorld(WorldServer world, Set<MachineKey> foundLoaded) {
        for (Chunk chunk : new ArrayList<>(world.getChunkProvider().getLoadedChunks())) {
            for (TileEntity tile : new ArrayList<>(chunk.getTileEntityMap().values())) {
                trackTile(tile, foundLoaded);
            }
        }

        for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
            trackTile(tile, foundLoaded);
        }
    }

    private static void trackTile(TileEntity tile, Set<MachineKey> foundLoaded) {
        if (!(tile instanceof TileMultiblockMachineController)) {
            return;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
        if (controller.getWorld() == null) {
            return;
        }
        MachineKey key = new MachineKey(controller.getWorld().provider.getDimension(), controller.getPos());
        if (update(controller, true)) {
            foundLoaded.add(key);
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
        cached.info.threads.clear();
    }

    private static boolean isCachedPositionLoaded(MinecraftServer server, MachineKey key) {
        WorldServer world = server.getWorld(key.dimension);
        if (world == null) {
            return false;
        }
        int chunkX = key.pos.getX() >> 4;
        int chunkZ = key.pos.getZ() >> 4;
        return world.getChunkProvider().getLoadedChunk(chunkX, chunkZ) != null;
    }

    private static boolean update(TileMultiblockMachineController controller, boolean loaded) {
        if (controller == null || controller.getWorld() == null || controller.getOwner() == null) {
            return false;
        }

        MachineKey key = new MachineKey(controller.getWorld().provider.getDimension(), controller.getPos());
        CachedMachine cached = CACHE.computeIfAbsent(key, ignored -> new CachedMachine());
        cached.owner = controller.getOwner();
        cached.info = capture(controller, loaded);
        return true;
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
                info.parallelism += threadInfo.parallelism;
                info.maxParallelism += threadInfo.maxParallelism;
                if (info.output.type == OutputInfo.Type.NONE && threadInfo.output.type != OutputInfo.Type.NONE) {
                    info.output = threadInfo.output;
                }
                info.threads.add(threadInfo);
            }
        }

        if (info.maxParallelism <= 0) {
            info.maxParallelism = Math.max(0, controller.getMaxParallelism());
        }
        if (info.output.type == OutputInfo.Type.NONE && controller.getActiveRecipe() != null) {
            info.output = firstOutput(controller.getActiveRecipe());
        }
        return info;
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
            return ((TileFactoryController) controller).getMaxThreads();
        }
        return threads == null ? 0 : threads.length;
    }

    private static String machineName(TileMultiblockMachineController controller) {
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null && controller instanceof TileMachineController) {
            machine = ((TileMachineController) controller).getParentMachine();
        }
        if (machine != null) {
            String localized = machine.getLocalizedName();
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
            if (machine.getRegistryName() != null) {
                return machine.getRegistryName().toString();
            }
        }
        String formedName = controller.getFormedMachineName();
        return formedName == null || formedName.isEmpty() ? "Unknown Machine" : formedName;
    }

    private static ItemStack controllerIcon(TileMultiblockMachineController controller) {
        Block block = controller.getWorld().getBlockState(controller.getPos()).getBlock();
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            return new ItemStack(item, 1, block.damageDropped(controller.getWorld().getBlockState(controller.getPos())));
        }
        DynamicMachine machine = controller.getFoundMachine();
        if (machine != null) {
            BlockController blockController = BlockController.getControllerWithMachine(machine);
            if (blockController != null) {
                return new ItemStack(blockController);
            }
        }
        return ItemStack.EMPTY;
    }

    private static String statusText(CraftingStatus status) {
        if (status == null) {
            return "";
        }
        String text = status.getUnlocMessage();
        return text == null ? "" : text;
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
