package com.shiver.modularmachineryterminal.common;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;

public class MachineInfo {

    public MachineKey key;
    public String name = "";
    public ItemStack controllerIcon = ItemStack.EMPTY;
    public boolean loaded;
    public boolean formed;
    public boolean running;
    public String status = "";
    public int activeThreads;
    public int maxThreads;
    public int parallelism;
    public int maxParallelism;
    public long energyPerTick;
    public OutputInfo output = OutputInfo.none();
    public final List<ThreadInfo> threads = new ArrayList<>();
    public final List<SmartInterfaceInfo> smartInterfaces = new ArrayList<>();

    /**
     * 复制机器基础信息，不包含线程等动态明细。
     * @return 方法执行结果
     */
    public MachineInfo copyBasic() {
        MachineInfo copy = new MachineInfo();
        copy.key = key;
        copy.name = name;
        copy.controllerIcon = controllerIcon.copy();
        copy.loaded = loaded;
        copy.formed = formed;
        copy.running = running;
        copy.status = status;
        copy.activeThreads = activeThreads;
        copy.maxThreads = maxThreads;
        copy.parallelism = parallelism;
        copy.maxParallelism = maxParallelism;
        copy.energyPerTick = energyPerTick;
        copy.output = output;
        copy.smartInterfaces.addAll(smartInterfaces);
        return copy;
    }

    /**
     * 将当前对象的数据写入目标缓冲区。
     * @param buffer 网络数据缓冲区
     */
    public void write(PacketBuffer buffer) {
        key.write(buffer);
        buffer.writeString(name);
        buffer.writeItemStack(controllerIcon);
        buffer.writeBoolean(loaded);
        buffer.writeBoolean(formed);
        buffer.writeBoolean(running);
        buffer.writeString(status);
        buffer.writeInt(activeThreads);
        buffer.writeInt(maxThreads);
        buffer.writeInt(parallelism);
        buffer.writeInt(maxParallelism);
        buffer.writeLong(energyPerTick);
        output.write(buffer);
        buffer.writeInt(threads.size());
        for (ThreadInfo thread : threads) {
            thread.write(buffer);
        }
        buffer.writeInt(smartInterfaces.size());
        for (SmartInterfaceInfo smartInterface : smartInterfaces) {
            smartInterface.write(buffer);
        }
    }

    /**
     * 从目标缓冲区读取并创建对象。
     * @param buffer 网络数据缓冲区
     * @return 方法执行结果
     */
    public static MachineInfo read(PacketBuffer buffer) {
        MachineInfo info = new MachineInfo();
        info.key = MachineKey.read(buffer);
        info.name = buffer.readString(32767);
        try {
            info.controllerIcon = buffer.readItemStack();
        } catch (Exception ignored) {
            info.controllerIcon = ItemStack.EMPTY;
        }
        info.loaded = buffer.readBoolean();
        info.formed = buffer.readBoolean();
        info.running = buffer.readBoolean();
        info.status = buffer.readString(32767);
        info.activeThreads = buffer.readInt();
        info.maxThreads = buffer.readInt();
        info.parallelism = buffer.readInt();
        info.maxParallelism = buffer.readInt();
        info.energyPerTick = buffer.readLong();
        info.output = OutputInfo.read(buffer);
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            info.threads.add(ThreadInfo.read(buffer));
        }
        int smartInterfaceCount = buffer.readInt();
        for (int i = 0; i < smartInterfaceCount; i++) {
            info.smartInterfaces.add(SmartInterfaceInfo.read(buffer));
        }
        return info;
    }
}
