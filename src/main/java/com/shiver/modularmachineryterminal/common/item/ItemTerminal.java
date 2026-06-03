package com.shiver.modularmachineryterminal.common.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.shiver.modularmachineryterminal.ModularMachineryTerminal;
import com.shiver.modularmachineryterminal.common.registry.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemTerminal extends Item implements IBauble {

    /**
     * 创建 ItemTerminal 实例。
     */
    public ItemTerminal() {
        setRegistryName(ModularMachineryTerminal.MOD_ID, "terminal");
        setTranslationKey(ModularMachineryTerminal.MOD_ID + ".terminal");
        setCreativeTab(ModItems.tab());
        setMaxStackSize(1);
    }

    /**
     * 处理终端物品右键使用并打开终端界面。
     * @param world 目标世界
     * @param player 目标玩家
     * @param hand hand 参数
     * @return 方法执行结果
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            ModularMachineryTerminal.proxy.openTerminalGui();
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    /**
     * 获取饰品类型，允许终端放入任意饰品栏。
     * @param itemStack 物品堆
     * @return 饰品类型
     */
    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.TRINKET;
    }
}
