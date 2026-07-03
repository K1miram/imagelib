package kimiram.imagelib;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static kimiram.imagelib.Constants.MOD_ID;

@Mod(value = MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ImageLibClient {
    public static final KeyMapping openDebugScreenKey = new KeyMapping(
            "key.imagelib.open_debug_screen",
            InputConstants.Type.KEYSYM,
            -1,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "debug"))
    );
    public static final ImageLib imageHelper = new ImageLib(MOD_ID);

    @SubscribeEvent
    public static void registerKey(RegisterKeyMappingsEvent event) {
        event.register(openDebugScreenKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openDebugScreenKey.isDown()) {
            Minecraft.getInstance().setScreenAndShow(new DebugScreen(imageHelper));
        }
    }
}
