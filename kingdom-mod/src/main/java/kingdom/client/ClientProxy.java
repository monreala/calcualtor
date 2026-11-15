package kingdom.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import kingdom.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class ClientProxy extends CommonProxy {
    public static KeyBinding openKey;

    @Override
    public void init(FMLInitializationEvent event) {
        openKey = new KeyBinding("key.kingdom.open", Keyboard.KEY_K, "key.categories.kingdom");
        ClientRegistry.registerKeyBinding(openKey);
        FMLCommonHandler.instance().bus().register(this);
    }

    @Override
    public void openGame() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiKingdom());
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (openKey != null && openKey.isPressed() && mc.currentScreen == null) openGame();
    }
}
