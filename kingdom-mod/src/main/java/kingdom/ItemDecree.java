package kingdom;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Королевский указ: правый клик открывает игру. */
public class ItemDecree extends Item {
    public ItemDecree() {
        setUnlocalizedName("kingdom.decree");
        setTextureName("kingdom:decree");
        setCreativeTab(CreativeTabs.tabMisc);
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) KingdomMod.proxy.openGame();
        return stack;
    }
}
