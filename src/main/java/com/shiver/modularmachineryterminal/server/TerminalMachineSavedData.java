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

    /**
     * 创建 TerminalMachineSavedData 实例。
     * @param name 目标名称
     */
    public TerminalMachineSavedData(String name) {
        super(name);
    }

    /**
     * 执行 get 相关逻辑。
     * @param server 当前服务器实例
     * @return 方法执行结果
     */
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

    /**
     * 执行 entries 相关逻辑。
     * @return 方法执行结果
     */
    public Map<MachineKey, PersistedMachine> entries() {
        return Collections.unmodifiableMap(machines);
    }

    /**
     * 执行 put 相关逻辑。
     * @param info 机器信息
     * @param owner 机器拥有者 UUID
     */
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

    /**
     * 执行 remove 相关逻辑。
     * @param key 目标机器键
     */
    public void remove(MachineKey key) {
        if (key != null && machines.remove(key) != null) {
            markDirty();
        }
    }

    /**
     * 执行 contains 相关逻辑。
     * @param key 目标机器键
     * @return 条件成立时返回 true，否则返回 false
     */
    public boolean contains(MachineKey key) {
        return machines.containsKey(key);
    }

    /**
     * 从 NBT 数据中读取保存的机器信息。
     * @param nbt nbt 参数
     */
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

    /**
     * 把保存的机器信息写入 NBT 数据。
     * @param compound compound 参数
     * @return 方法执行结果
     */
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

        /**
         * 创建 PersistedMachine 实例。
         * @param key 目标机器键
         * @param owner 机器拥有者 UUID
         * @param name 目标名称
         * @param controllerIcon controllerIcon 参数
         * @param formed formed 参数
         * @param status 机器或线程状态
         */
        private PersistedMachine(MachineKey key, UUID owner, String name, ItemStack controllerIcon, boolean formed, String status) {
            this.key = key;
            this.owner = owner;
            this.name = name == null ? "" : name;
            this.controllerIcon = controllerIcon == null ? ItemStack.EMPTY : controllerIcon.copy();
            this.formed = formed;
            this.status = status == null ? "" : status;
        }

        /**
         * 执行 from 相关逻辑。
         * @param info 机器信息
         * @param owner 机器拥有者 UUID
         * @return 方法执行结果
         */
        public static PersistedMachine from(MachineInfo info, UUID owner) {
            return new PersistedMachine(info.key, owner, info.name, info.controllerIcon, info.formed, info.status);
        }

        /**
         * 执行 to unloaded info 相关逻辑。
         * @return 方法执行结果
         */
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

        /**
         * 执行 same data 相关逻辑。
         * @param other other 参数
         * @return 条件成立时返回 true，否则返回 false
         */
        private boolean sameData(PersistedMachine other) {
            return other != null
                    && key.equals(other.key)
                    && (owner == null ? other.owner == null : owner.equals(other.owner))
                    && name.equals(other.name)
                    && ItemStack.areItemStacksEqual(controllerIcon, other.controllerIcon)
                    && formed == other.formed
                    && status.equals(other.status);
        }

        /**
         * 将当前对象的数据写入目标缓冲区。
         * @return 方法执行结果
         */
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

        /**
         * 从目标缓冲区读取并创建对象。
         * @param tag tag 参数
         * @return 方法执行结果
         */
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
