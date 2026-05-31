package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class SmartInterfaceInfo {

    public BlockPos interfacePos = BlockPos.ORIGIN;
    public int dataIndex;
    public String type = "";
    public String parentMachineName = "";
    public float value;

    public void write(PacketBuffer buffer) {
        buffer.writeBlockPos(interfacePos);
        buffer.writeInt(dataIndex);
        buffer.writeString(type);
        buffer.writeString(parentMachineName);
        buffer.writeFloat(value);
    }

    public static SmartInterfaceInfo read(PacketBuffer buffer) {
        SmartInterfaceInfo info = new SmartInterfaceInfo();
        info.interfacePos = buffer.readBlockPos();
        info.dataIndex = buffer.readInt();
        info.type = buffer.readString(32767);
        info.parentMachineName = buffer.readString(32767);
        info.value = buffer.readFloat();
        return info;
    }
}
