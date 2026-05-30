package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;

public class ThreadInfo {

    public String name = "";
    public String status = "";
    public boolean working;
    public int parallelism;
    public int maxParallelism;
    public OutputInfo output = OutputInfo.none();

    public void write(PacketBuffer buffer) {
        buffer.writeString(name);
        buffer.writeString(status);
        buffer.writeBoolean(working);
        buffer.writeInt(parallelism);
        buffer.writeInt(maxParallelism);
        output.write(buffer);
    }

    public static ThreadInfo read(PacketBuffer buffer) {
        ThreadInfo info = new ThreadInfo();
        info.name = buffer.readString(32767);
        info.status = buffer.readString(32767);
        info.working = buffer.readBoolean();
        info.parallelism = buffer.readInt();
        info.maxParallelism = buffer.readInt();
        info.output = OutputInfo.read(buffer);
        return info;
    }
}
