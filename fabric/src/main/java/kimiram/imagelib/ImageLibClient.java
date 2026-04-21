package kimiram.imagelib;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

import static kimiram.imagelib.Constants.MOD_ID;

public class ImageLibClient implements ClientModInitializer {
    public static final KeyMapping openDebugScreenKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.imagelib.open_debug_screen",
            InputConstants.Type.KEYSYM,
            -1,
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(MOD_ID, "debug"))
    ));
    public static final ImageLib imageHelper = new ImageLib(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openDebugScreenKey.isDown()) {
                client.setScreen(new DebugScreen(imageHelper));
            }
        });
    }
}
