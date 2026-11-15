package kingdom;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Mod(modid = KingdomMod.MODID, name = KingdomMod.NAME, version = KingdomMod.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class KingdomMod {
    public static final String MODID = "kingdom";
    public static final String NAME = "Тридесятое королевство";
    public static final String VERSION = "1.0.0";

    @SidedProxy(clientSide = "kingdom.client.ClientProxy", serverSide = "kingdom.CommonProxy")
    public static CommonProxy proxy;

    public static Item decree;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        decree = new ItemDecree();
        GameRegistry.registerItem(decree, "decree");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Королевский указ: бумага + золотой слиток
        GameRegistry.addShapelessRecipe(new ItemStack(decree), Items.paper, Items.gold_ingot);
        proxy.init(event);
    }
}
