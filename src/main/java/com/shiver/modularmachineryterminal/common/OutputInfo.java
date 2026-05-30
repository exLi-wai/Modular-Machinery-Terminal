package com.shiver.modularmachineryterminal.common;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;

public class OutputInfo {

    public enum Type {
        NONE,
        ITEM,
        FLUID,
        GAS,
        TEXT
    }

    public Type type = Type.NONE;
    public ItemStack item = ItemStack.EMPTY;
    public FluidStack fluid;
    public String name = "";

    public static OutputInfo none() {
        return new OutputInfo();
    }

    public static OutputInfo item(ItemStack stack) {
        OutputInfo info = new OutputInfo();
        info.type = Type.ITEM;
        info.item = stack == null ? ItemStack.EMPTY : stack.copy();
        info.name = info.item.isEmpty() ? "" : info.item.getDisplayName();
        return info;
    }

    public static OutputInfo fluid(FluidStack stack) {
        OutputInfo info = new OutputInfo();
        info.type = Type.FLUID;
        info.fluid = stack == null ? null : stack.copy();
        info.name = stack == null ? "" : stack.getLocalizedName();
        return info;
    }

    public static OutputInfo text(Type type, String name) {
        OutputInfo info = new OutputInfo();
        info.type = type;
        info.name = name == null ? "" : name;
        return info;
    }

    public void write(PacketBuffer buffer) {
        buffer.writeByte(type.ordinal());
        buffer.writeString(name);
        buffer.writeItemStack(item);
        buffer.writeBoolean(fluid != null);
        if (fluid != null) {
            NBTTagCompound tag = new NBTTagCompound();
            fluid.writeToNBT(tag);
            buffer.writeCompoundTag(tag);
        }
    }

    public static OutputInfo read(PacketBuffer buffer) {
        OutputInfo info = new OutputInfo();
        info.type = Type.values()[buffer.readUnsignedByte()];
        info.name = buffer.readString(32767);
        try {
            info.item = buffer.readItemStack();
        } catch (Exception ignored) {
            info.item = ItemStack.EMPTY;
        }
        if (buffer.readBoolean()) {
            try {
                info.fluid = FluidStack.loadFluidStackFromNBT(buffer.readCompoundTag());
            } catch (Exception ignored) {
                info.fluid = null;
            }
        }
        return info;
    }

    public String displayName() {
        if (type == Type.ITEM && !item.isEmpty()) {
            return item.getDisplayName();
        }
        if (type == Type.FLUID && fluid != null) {
            return fluid.getLocalizedName();
        }
        return name;
    }
}
