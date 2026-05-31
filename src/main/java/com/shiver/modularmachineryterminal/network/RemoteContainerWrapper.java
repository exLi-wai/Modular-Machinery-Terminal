package com.shiver.modularmachineryterminal.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Wraps an existing Container to bypass the canInteractWith distance check.
 * All operations are delegated to the original container so that
 * slots, shift-click, and listener synchronization work as expected.
 */
public class RemoteContainerWrapper extends Container {

    private final Container delegate;

    public RemoteContainerWrapper(Container delegate, EntityPlayer player) {
        this.delegate = delegate;
        // Share the same window ID so that client packets map correctly.
        this.windowId = delegate.windowId;
        // Share the slot and item stack lists so the game sees identical state.
        this.inventorySlots = delegate.inventorySlots;
        this.inventoryItemStacks = delegate.inventoryItemStacks;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return true;
    }

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        return delegate.transferStackInSlot(player, index);
    }

    @Override
    @Nonnull
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickType, @Nonnull EntityPlayer player) {
        return delegate.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public void detectAndSendChanges() {
        delegate.detectAndSendChanges();
    }

    @Override
    public void addListener(@Nonnull IContainerListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(@Nonnull IContainerListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void onContainerClosed(@Nonnull EntityPlayer player) {
        delegate.onContainerClosed(player);
    }

    @Override
    public void putStackInSlot(int slotId, @Nonnull ItemStack stack) {
        delegate.putStackInSlot(slotId, stack);
    }

    @Override
    public void setAll(@Nonnull List<ItemStack> items) {
        delegate.setAll(items);
    }

    @Override
    public boolean canMergeSlot(@Nonnull ItemStack stack, @Nonnull Slot slot) {
        return delegate.canMergeSlot(stack, slot);
    }

    @Override
    public boolean canDragIntoSlot(@Nonnull Slot slot) {
        return delegate.canDragIntoSlot(slot);
    }

    @Override
    @Nonnull
    public Slot getSlot(int slotId) {
        return delegate.getSlot(slotId);
    }

    @Override
    public boolean enchantItem(@Nonnull EntityPlayer player, int id) {
        return delegate.enchantItem(player, id);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        delegate.updateProgressBar(id, data);
    }

    @Override
    public short getNextTransactionID(@Nonnull net.minecraft.entity.player.InventoryPlayer invPlayer) {
        return delegate.getNextTransactionID(invPlayer);
    }

    @Override
    public boolean getCanCraft(@Nonnull EntityPlayer player) {
        return delegate.getCanCraft(player);
    }

    @Override
    public void setCanCraft(@Nonnull EntityPlayer player, boolean canCraft) {
        delegate.setCanCraft(player, canCraft);
    }
}
