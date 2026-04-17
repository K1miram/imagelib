package kimiram.imagelib;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DebugScreen extends Screen {
    private String url = "";
    private final ImageLib imageHelper;
    private ImageLib.Type type = ImageLib.Type.STATIC_IMAGE;

    protected DebugScreen(ImageLib imageHelper) {
        super(Component.empty());
        this.imageHelper = imageHelper;
    }

    @Override
    protected void init() {
        EditBox urlField = new EditBox(font, (width - 300) / 2, height - 180, 300, 20, Component.empty());
        urlField.setMaxLength(256);
        addRenderableWidget(urlField);

        Button typeButton = Button.builder(Component.literal("Static Image"), button -> {
            if (type == ImageLib.Type.STATIC_IMAGE) {
                type = ImageLib.Type.GIF;
                button.setMessage(Component.literal("Gif"));
            } else {
                type = ImageLib.Type.STATIC_IMAGE;
                button.setMessage(Component.literal("Static Image"));
            }
        })
                .pos((width - 300) / 2, height - 150)
                .size(140, 20)
                .build();
        addRenderableWidget(typeButton);

        Button applyButton = Button.builder(Component.literal("Apply"), button -> {
            url = urlField.getValue();
//            imageHelper.downloadImage(url, type);
        })
                .pos((width - 300) / 2 + 140 + 20, height - 150)
                .size(130, 20)
                .build();
        addRenderableWidget(applyButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.renderOutline(5, 5, 150, 100, 0x33FFFFFF);

        Identifier id = imageHelper.getImageId(url, type);
        ImageLib.Size size = imageHelper.fitImageSize(url, type, 150, 100);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, id, 5, 5, 0, 0,
                size.width(), size.height(), size.width(), size.height(), size.width(), size.height());
    }
}
