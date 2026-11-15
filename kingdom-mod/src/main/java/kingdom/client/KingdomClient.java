package kingdom.client;

import java.io.File;
import java.util.Random;

import kingdom.game.Game;
import kingdom.game.Store;
import net.minecraft.client.Minecraft;

/** Одна партия на клиент. Сохранение: .minecraft/kingdom/save.properties */
public final class KingdomClient {
    private static Game game;

    private KingdomClient() {}

    public static Game game() {
        if (game == null) {
            File file = new File(new File(Minecraft.getMinecraft().mcDataDir, "kingdom"), "save.properties");
            game = Game.open(Store.load(file), new Random());
        }
        return game;
    }
}
