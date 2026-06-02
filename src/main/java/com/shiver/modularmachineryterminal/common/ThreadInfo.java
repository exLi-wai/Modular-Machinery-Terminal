package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;

public class ThreadInfo {

    public String name = "";
    public String status = "";
    public boolean working;
    public int parallelism;
    public int maxParallelism;
    public int progress;
    public OutputInfo output = OutputInfo.none();

    /**
     * 将当前对象的数据写入目标缓冲区。
     * @param buffer 网络数据缓冲区
     */
    public void write(PacketBuffer buffer) {
        buffer.writeString(name);
        buffer.writeString(status);
        buffer.writeBoolean(working);
        buffer.writeInt(parallelism);
        buffer.writeInt(maxParallelism);
        buffer.writeInt(progress);
        output.write(buffer);
    }

    /**
     * 从目标缓冲区读取并创建对象。
     * @param buffer 网络数据缓冲区
     * @return 方法执行结果
     */
    public static ThreadInfo read(PacketBuffer buffer) {
        ThreadInfo info = new ThreadInfo();
        info.name = buffer.readString(32767);
        info.status = buffer.readString(32767);
        info.working = buffer.readBoolean();
        info.parallelism = buffer.readInt();
        info.maxParallelism = buffer.readInt();
        info.progress = buffer.readInt();
        info.output = OutputInfo.read(buffer);
        return info;
    }
}
