package com.shiver.modularmachineryterminal.common;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class MachineKey {

    public final int dimension;
    public final BlockPos pos;

    public MachineKey(int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public void write(PacketBuffer buffer) {
        buffer.writeInt(dimension);
        buffer.writeBlockPos(pos);
    }

    public static MachineKey read(PacketBuffer buffer) {
        return new MachineKey(buffer.readInt(), buffer.readBlockPos());
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }
}
