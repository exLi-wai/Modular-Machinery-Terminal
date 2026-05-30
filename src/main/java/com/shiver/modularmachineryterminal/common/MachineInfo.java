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
    public OutputInfo output = OutputInfo.none();
    public final List<ThreadInfo> threads = new ArrayList<>();

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
        copy.output = output;
        return copy;
    }

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
        output.write(buffer);
        buffer.writeInt(threads.size());
        for (ThreadInfo thread : threads) {
            thread.write(buffer);
        }
    }

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
        info.output = OutputInfo.read(buffer);
        int count = buffer.readInt();
        for (int i = 0; i < count; i++) {
            info.threads.add(ThreadInfo.read(buffer));
        }
        return info;
    }
}
