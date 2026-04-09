package kimiram.imagelib;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static kimiram.imagelib.Constants.MOD_ID;

///
public class ImageLib {
    private static final Identifier DEFAULT_IMAGE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/default_image.png");

    private final String namespace;
    private final Logger logger;
    private int imagesCount = 0;
    private int gifsCount = 0;
    private final Map<String, Identifier> loadingImages = new HashMap<>();
    private final Map<String, String> loadingGifs = new HashMap<>();
    private final Map<String, DownloadedImage> downloadedImages = new HashMap<>();
    private final Map<String, List<DownloadedGifFrame> > downloadedGifs = new HashMap<>();
    private final Map<String, Image> registeredImages = new HashMap<>();
    private final Map<String, Gif> registeredGifs = new HashMap<>();
    private final Map<String, Integer> attemptsToDownloadImage = new HashMap<>();
    private final Map<String, Integer> attemptsToDownloadGif = new HashMap<>();
    private final int maxAttempts;
    private final Size defaultImageSize;

    public ImageLib(String namespace, int maxAttempts, Size defaultImageSize) {
        this.namespace = namespace;
        this.logger = LoggerFactory.getLogger(namespace);
        this.maxAttempts = maxAttempts;
        this.attemptsToDownloadImage.put("", maxAttempts);
        this.attemptsToDownloadImage.put(null, maxAttempts);
        this.attemptsToDownloadGif.put("", maxAttempts);
        this.attemptsToDownloadGif.put(null, maxAttempts);
        this.defaultImageSize = defaultImageSize;
    }

    public ImageLib(String namespace) {
        this(namespace, 3, new Size(64, 64));
    }

    public ImageLib(String namespace, int maxAttempts) {
        this(namespace, maxAttempts, new Size(64, 64));
    }

    public ImageLib(String namespace, Size defaultImageSize) {
        this(namespace, 3, defaultImageSize);
    }

    public void downloadImage(String url, Type type) {
        if (type == Type.STATIC_IMAGE) {
            if (!loadingImages.containsKey(url) && !registeredImages.containsKey(url) && attemptsToDownloadImage.getOrDefault(url, 0) < maxAttempts) {
                downloadStaticImage(url);
            }
        } else if (type == Type.GIF) {
            if (!loadingGifs.containsKey(url) && !registeredGifs.containsKey(url) && attemptsToDownloadGif.getOrDefault(url, 0) < maxAttempts) {
                downloadGif(url);
            }
        }
    }

    private void downloadStaticImage(String url) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, "imagelib_image" + imagesCount);
        imagesCount++;
        loadingImages.put(url, id);
        attemptsToDownloadImage.put(url, attemptsToDownloadImage.getOrDefault(url, 0) + 1);

        Thread downloadThread = new Thread(() -> {
            try {
                URI uri = new URI(url);
                URL imageUrl = uri.toURL();
                BufferedImage image = ImageIO.read(imageUrl);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                downloadedImages.put(url, new DownloadedImage(id, baos.toByteArray()));
            } catch (Exception e) {
                logger.warn("Failed to download image from " + url + " because: " + e);
                logger.warn("Attempt " + attemptsToDownloadImage.get(url));
                loadingImages.remove(url);
            }
        });
        downloadThread.start();
    }

    private void downloadGif(String url) {
        String name = "imagelib_gif" + gifsCount;
        gifsCount++;
        loadingGifs.put(url, name);
        attemptsToDownloadGif.put(url, attemptsToDownloadGif.getOrDefault(url, 0) + 1);

        Thread downloadThread = new Thread(() -> {
            try {
                URI uri = new URI(url);
                ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
                ImageInputStream iis = ImageIO.createImageInputStream(uri.toURL().openStream());
                reader.setInput(iis);

                BufferedImage img = reader.read(0);
                int width = img.getWidth(), height = img.getHeight();
                BufferedImage base = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                int numImages = reader.getNumImages(true);
                List<DownloadedGifFrame> frames = new ArrayList<>();

                for (int i = 0; i < numImages; i++) {
                    BufferedImage image = reader.read(i);
                    BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);;

                    IIOMetadata metadata = reader.getImageMetadata(i);
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                    IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
                    IIOMetadataNode imgDesc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                    String disposalMethod = gce.getAttribute("disposalMethod");
                    int delayTime = Integer.parseInt(gce.getAttribute("delayTime"));
                    int x = Integer.parseInt(imgDesc.getAttribute("imageLeftPosition"));
                    int y = Integer.parseInt(imgDesc.getAttribute("imageTopPosition"));

                    if (disposalMethod != "restoreToPrevious") {
                        base.createGraphics().drawImage(image, x, y, null);
                        frame.createGraphics().drawImage(base, 0, 0, null);

                        if (disposalMethod == "restoreToBackgroundColor") {
                            base = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        }
                    } else {
                        frame.createGraphics().drawImage(base, 0, 0, null);
                        frame.createGraphics().drawImage(image, x, y, null);
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(frame, "png", baos);
                    Identifier id = Identifier.fromNamespaceAndPath(namespace, name + "_frame" + i);
                    if (delayTime == 0) delayTime = 6;
                    frames.add(new DownloadedGifFrame(id, baos.toByteArray(), delayTime * 10));
                }

                downloadedGifs.put(url, frames);
            } catch (Exception e) {
                logger.warn("Failed to download gif from " + url + " because: " + e);
                logger.warn("Attempt " + attemptsToDownloadGif.get(url));
                loadingGifs.remove(url);
            }
        });
        downloadThread.start();
    }


    private void registerStaticImage(String url) {
        try {
            DownloadedImage image = downloadedImages.get(url);
            NativeImage nativeImage = NativeImage.read(image.bytes());
            DynamicTexture texture = new DynamicTexture(nativeImage::toString, nativeImage);
            Minecraft.getInstance().getTextureManager().register(image.id(), texture);
            registeredImages.put(url, new Image(image.id(), nativeImage.getWidth(), nativeImage.getHeight()));
        } catch (Exception e) {
            logger.warn("Failed to register image " + url + " because: " + e);
        }
        loadingImages.remove(url);
        downloadedImages.remove(url);
    }

    private void registerGif(String url) {
        try {
            List<DownloadedGifFrame> downloadedFrames = downloadedGifs.get(url);
            List<GifFrame> frames = new ArrayList<>();
            int width = 0, height = 0;
            for (DownloadedGifFrame frame: downloadedFrames) {
                NativeImage nativeImage = NativeImage.read(frame.bytes());
                DynamicTexture texture = new DynamicTexture(nativeImage::toString, nativeImage);
                Minecraft.getInstance().getTextureManager().register(frame.id(), texture);
                frames.add(new GifFrame(frame.id(), frame.delay()));
                width = nativeImage.getWidth();
                height = nativeImage.getHeight();
            }
            registeredGifs.put(url, new Gif(frames, width, height));
        } catch (Exception e) {
            logger.warn("Failed to register gif " + url + " because: " + e);
        }
        loadingGifs.remove(url);
        downloadedGifs.remove(url);
    }


    public Identifier getImageId(String url) {
        return getImageId(url, true);
    }

    public Identifier getImageId(String url, boolean shouldDownload) {
        if (downloadedImages.containsKey(url)) {
            registerStaticImage(url);
        }
        if (registeredImages.containsKey(url)) {
            return registeredImages.get(url).id();
        }
        if (shouldDownload) {
            if (!loadingImages.containsKey(url) && attemptsToDownloadImage.getOrDefault(url, 0) < maxAttempts) {
                downloadStaticImage(url);
            }
        }
        return DEFAULT_IMAGE;
    }

    public Identifier getGifFrameId(String url) {
        return getGifFrameId(url, true);
    }

    public Identifier getGifFrameId(String url, boolean shouldDownload) {
        if (downloadedGifs.containsKey(url)) {
            registerGif(url);
        }
        if (registeredGifs.containsKey(url)) {
            return registeredGifs.get(url).getCurrentFrame();
        }
        if (shouldDownload) {
            if (!loadingGifs.containsKey(url) && attemptsToDownloadGif.getOrDefault(url, 0) < maxAttempts) {
                downloadGif(url);
            }
        }
        return DEFAULT_IMAGE;
    }


    public Size getImageSize(String url) {
        return getImageSize(url, defaultImageSize);
    }

    public Size getImageSize(String url, Size defaultSize) {
        return registeredImages.containsKey(url) ? registeredImages.get(url).size() : defaultSize;
    }

    public Size fitImageSize(String url, int areaWidth, int areaHeight) {
        Size imageSize = getImageSize(url);
        if (imageSize.width() * areaHeight > imageSize.height() * areaWidth) {
            return new Size(areaWidth, imageSize.height() * areaWidth / imageSize.width());
        } else {
            return new Size(imageSize.width * areaHeight / imageSize.height(), areaHeight);
        }
    }

    public Size getGifSize(String url) {
        return getGifSize(url, defaultImageSize);
    }

    public Size getGifSize(String url, Size defaultSize) {
        return registeredGifs.containsKey(url) ? registeredGifs.get(url).getGifSize() : defaultSize;
    }

    public Size fitGifSize(String url, int areaWidth, int areaHeight) {
        Size gifSize = getGifSize(url);
        if (gifSize.width() * areaHeight > gifSize.height() * areaWidth) {
            return new Size(areaWidth, gifSize.height() * areaWidth / gifSize.width());
        } else {
            return new Size(gifSize.width * areaHeight / gifSize.height(), areaHeight);
        }
    }


    private record DownloadedImage(Identifier id, byte[] bytes) {
    }

    public record Image(Identifier id, Size size) {
        public Image(Identifier id, int width, int height) {
            this(id, new Size(width, height));
        }
    }


    private record DownloadedGifFrame(Identifier id, byte[] bytes, int delay) {
    }

    private record GifFrame(Identifier id, int delay) {
    }

    private static class Gif {
        private final List<GifFrame> frames;
        private final Size size;
        private int currentImage = 0;
        private long time;

        private Gif(List<GifFrame> frames, int width, int height) {
            this.frames = frames;
            this.size = new Size(width, height);
            this.time = Util.getMillis();
        }

        public Size getGifSize() {
            return size;
        }

        public Identifier getCurrentFrame() {
            if (Util.getMillis() - time >= frames.get(currentImage).delay) {
                currentImage = (currentImage + 1) % frames.size();
                time = Util.getMillis();
            }
            return frames.get(currentImage).id();
        }
    }

    public record Size(int width, int height) {
    }

    public enum Type {
        STATIC_IMAGE,
        GIF
    }
}
