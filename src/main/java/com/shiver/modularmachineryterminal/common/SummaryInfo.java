package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;

public class SummaryInfo {

    public int total;
    public int loaded;
    public int formed;
    public int running;

    public void write(PacketBuffer buffer) {
        buffer.writeInt(total);
        buffer.writeInt(loaded);
        buffer.writeInt(formed);
        buffer.writeInt(running);
    }

    public static SummaryInfo read(PacketBuffer buffer) {
        SummaryInfo info = new SummaryInfo();
        info.total = buffer.readInt();
        info.loaded = buffer.readInt();
        info.formed = buffer.readInt();
        info.running = buffer.readInt();
        return info;
    }
}
