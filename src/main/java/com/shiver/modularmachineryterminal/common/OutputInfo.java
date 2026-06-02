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

    /**
     * 创建一个表示无输出的输出信息。
     * @return 方法执行结果
     */
    public static OutputInfo none() {
        return new OutputInfo();
    }

    /**
     * 创建一个物品输出信息。
     * @param stack 物品堆
     * @return 方法执行结果
     */
    public static OutputInfo item(ItemStack stack) {
        OutputInfo info = new OutputInfo();
        info.type = Type.ITEM;
        info.item = stack == null ? ItemStack.EMPTY : stack.copy();
        info.name = info.item.isEmpty() ? "" : info.item.getDisplayName();
        return info;
    }

    /**
     * 创建一个流体输出信息。
     * @param stack 物品堆
     * @return 方法执行结果
     */
    public static OutputInfo fluid(FluidStack stack) {
        OutputInfo info = new OutputInfo();
        info.type = Type.FLUID;
        info.fluid = stack == null ? null : stack.copy();
        info.name = stack == null ? "" : stack.getLocalizedName();
        return info;
    }

    /**
     * 创建一个文本形式的输出信息。
     * @param type 待检查的类型
     * @param name 目标名称
     * @return 方法执行结果
     */
    public static OutputInfo text(Type type, String name) {
        OutputInfo info = new OutputInfo();
        info.type = type;
        info.name = name == null ? "" : name;
        return info;
    }

    /**
     * 将当前对象的数据写入目标缓冲区。
     * @param buffer 网络数据缓冲区
     */
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

    /**
     * 从目标缓冲区读取并创建对象。
     * @param buffer 网络数据缓冲区
     * @return 方法执行结果
     */
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

    /**
     * 返回输出内容的显示名称。
     * @return 对应的文本
     */
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
