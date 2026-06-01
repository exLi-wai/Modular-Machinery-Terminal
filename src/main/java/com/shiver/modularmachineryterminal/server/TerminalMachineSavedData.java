package com.shiver.modularmachineryterminal.server;

import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TerminalMachineSavedData extends WorldSavedData {

    private static final String DATA_NAME = "modularmachinery_terminal_machines";

    private final Map<MachineKey, PersistedMachine> machines = new LinkedHashMap<>();

    public TerminalMachineSavedData(String name) {
        super(name);
    }

    public static TerminalMachineSavedData get(MinecraftServer server) {
        WorldServer world = server == null ? null : server.getWorld(0);
        if (world == null && server != null && server.worlds != null && server.worlds.length > 0) {
            world = server.worlds[0];
        }
        if (world == null) {
            return null;
        }

        MapStorage storage = world.getMapStorage();
        TerminalMachineSavedData data = (TerminalMachineSavedData) storage.getOrLoadData(TerminalMachineSavedData.class, DATA_NAME);
        if (data == null) {
            data = new TerminalMachineSavedData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public Map<MachineKey, PersistedMachine> entries() {
        return Collections.unmodifiableMap(machines);
    }

    public void put(MachineInfo info, UUID owner) {
        if (info == null || info.key == null) {
            return;
        }
        PersistedMachine machine = PersistedMachine.from(info, owner);
        PersistedMachine previous = machines.put(info.key, machine);
        if (!machine.sameData(previous)) {
            markDirty();
        }
    }

    public void remove(MachineKey key) {
        if (key != null && machines.remove(key) != null) {
            markDirty();
        }
    }

    public boolean contains(MachineKey key) {
        return machines.containsKey(key);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        machines.clear();
        NBTTagList list = nbt.getTagList("machines", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            PersistedMachine machine = PersistedMachine.read(list.getCompoundTagAt(i));
            if (machine != null) {
                machines.put(machine.key, machine);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (PersistedMachine machine : machines.values()) {
            list.appendTag(machine.write());
        }
        compound.setTag("machines", list);
        return compound;
    }

    public static class PersistedMachine {

        public final MachineKey key;
        public final UUID owner;
        public final String name;
        public final ItemStack controllerIcon;
        public final boolean formed;
        public final String status;

        private PersistedMachine(MachineKey key, UUID owner, String name, ItemStack controllerIcon, boolean formed, String status) {
            this.key = key;
            this.owner = owner;
            this.name = name == null ? "" : name;
            this.controllerIcon = controllerIcon == null ? ItemStack.EMPTY : controllerIcon.copy();
            this.formed = formed;
            this.status = status == null ? "" : status;
        }

        public static PersistedMachine from(MachineInfo info, UUID owner) {
            return new PersistedMachine(info.key, owner, info.name, info.controllerIcon, info.formed, info.status);
        }

        public MachineInfo toUnloadedInfo() {
            MachineInfo info = new MachineInfo();
            info.key = key;
            info.name = name;
            info.controllerIcon = controllerIcon.copy();
            info.loaded = false;
            info.formed = formed;
            info.running = false;
            info.status = status;
            return info;
        }

        private boolean sameData(PersistedMachine other) {
            return other != null
                    && key.equals(other.key)
                    && (owner == null ? other.owner == null : owner.equals(other.owner))
                    && name.equals(other.name)
                    && ItemStack.areItemStacksEqual(controllerIcon, other.controllerIcon)
                    && formed == other.formed
                    && status.equals(other.status);
        }

        private NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", key.dimension);
            tag.setInteger("x", key.pos.getX());
            tag.setInteger("y", key.pos.getY());
            tag.setInteger("z", key.pos.getZ());
            if (owner != null) {
                tag.setUniqueId("owner", owner);
            }
            tag.setString("name", name);
            tag.setBoolean("formed", formed);
            tag.setString("status", status);
            if (!controllerIcon.isEmpty()) {
                tag.setTag("controllerIcon", controllerIcon.writeToNBT(new NBTTagCompound()));
            }
            return tag;
        }

        private static PersistedMachine read(NBTTagCompound tag) {
            try {
                MachineKey key = new MachineKey(tag.getInteger("dimension"), new BlockPos(
                        tag.getInteger("x"),
                        tag.getInteger("y"),
                        tag.getInteger("z")
                ));
                UUID owner = tag.hasUniqueId("owner") ? tag.getUniqueId("owner") : null;
                ItemStack icon = ItemStack.EMPTY;
                if (tag.hasKey("controllerIcon", 10)) {
                    icon = new ItemStack(tag.getCompoundTag("controllerIcon"));
                }
                return new PersistedMachine(
                        key,
                        owner,
                        tag.getString("name"),
                        icon,
                        tag.getBoolean("formed"),
                        tag.getString("status")
                );
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
