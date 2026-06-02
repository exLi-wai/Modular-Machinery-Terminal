package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class MachineKey {

    public final int dimension;
    public final BlockPos pos;

    /**
     * 创建 MachineKey 实例。
     * @param dimension dimension 参数
     * @param pos 目标方块位置
     */
    public MachineKey(int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    /**
     * 将当前对象的数据写入目标缓冲区。
     * @param buffer 网络数据缓冲区
     */
    public void write(PacketBuffer buffer) {
        buffer.writeInt(dimension);
        buffer.writeBlockPos(pos);
    }

    /**
     * 从目标缓冲区读取并创建对象。
     * @param buffer 网络数据缓冲区
     * @return 方法执行结果
     */
    public static MachineKey read(PacketBuffer buffer) {
        return new MachineKey(buffer.readInt(), buffer.readBlockPos());
    }

    /**
     * 判断另一个对象是否表示同一台机器。
     * @param obj obj 参数
     * @return 条件成立时返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MachineKey)) {
            return false;
        }
        MachineKey other = (MachineKey) obj;
        return dimension == other.dimension && Objects.equals(pos, other.pos);
    }

    /**
     * 计算机器键的哈希值。
     * @return 计算得到的数值
     */
    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }
}
