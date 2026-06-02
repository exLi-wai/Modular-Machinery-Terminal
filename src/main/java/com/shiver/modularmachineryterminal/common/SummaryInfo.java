package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;

public class SummaryInfo {

    public int total;
    public int loaded;
    public int formed;
    public int running;

    /**
     * 将当前对象的数据写入目标缓冲区。
     * @param buffer 网络数据缓冲区
     */
    public void write(PacketBuffer buffer) {
        buffer.writeInt(total);
        buffer.writeInt(loaded);
        buffer.writeInt(formed);
        buffer.writeInt(running);
    }

    /**
     * 从目标缓冲区读取并创建对象。
     * @param buffer 网络数据缓冲区
     * @return 方法执行结果
     */
    public static SummaryInfo read(PacketBuffer buffer) {
        SummaryInfo info = new SummaryInfo();
        info.total = buffer.readInt();
        info.loaded = buffer.readInt();
        info.formed = buffer.readInt();
        info.running = buffer.readInt();
        return info;
    }
}
