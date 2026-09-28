package NokhXyr.NokhFrame;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Stream;

public final class StudioScreen extends Screen {
    private final Path skinDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("nokhframe").resolve("skins");
    private final Path backgroundDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("nokhframe").resolve("backgrounds");
    private final List<Path> skinFiles = new ArrayList<>();
    private final List<Path> backgroundFiles = new ArrayList<>();
    private Component status = Component.empty();
    private int backgroundIndex;
    private @Nullable Integer customBackground;
    private int skinIndex = -1;
    private float yaw;
    private float pitch;
    private float roll;
    private float zoom = 1.0F;
    private boolean draggingPreview;
    private int dragButton;
    private StudioAvatarRenderer.Motion motion = StudioAvatarRenderer.Motion.IDLE;
    private long motionStarted = System.nanoTime();
    private boolean itemMode;
    private ItemStack selectedStack = ItemStack.EMPTY;
    private boolean captureRequested;
    private @Nullable ResourceLocation customSkin;
    private @Nullable ResourceLocation backgroundTexture;
    private int backgroundWidth;
    private int backgroundHeight;
    private @Nullable Button selectionButton;
    private @Nullable EditBox hexInput;

    public StudioScreen() {
        super(Component.translatable("screen.nokhframe.title"));
        reloadSkins();
        reloadBackgrounds();
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int x = this.width - panelWidth + 8;
        int buttonWidth = panelWidth - 16;
        int row = Math.max(21, Math.min(24, (this.height - 82) / 7));
        int y = 66;

        this.addRenderableWidget(Button.builder(modeLabel(), button -> {
            itemMode = !itemMode;
            button.setMessage(modeLabel());
            updateSelectionButton();
        }).bounds(x, y, buttonWidth, 20).build());

        this.selectionButton = this.addRenderableWidget(Button.builder(selectionLabel(), button ->
                this.minecraft.setScreen(new StudioCatalogScreen(this, itemMode)))
                .bounds(x, y + row, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(motionLabel(), button -> {
            StudioAvatarRenderer.Motion[] modes = StudioAvatarRenderer.Motion.values();
            motion = modes[(motion.ordinal() + 1) % modes.length];
            motionStarted = System.nanoTime();
            button.setMessage(motionLabel());
        }).bounds(x, y + row * 2, buttonWidth, 20).build());

        int half = (buttonWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.color"), button -> {
            releaseBackground();
            customBackground = null;
            backgroundIndex = StudioRules.nextBackground(backgroundIndex);
            status = Component.translatable("status.nokhframe.background_color",
                    Component.translatable("background.nokhframe." + StudioRules.BACKGROUNDS.get(backgroundIndex).name()));
        }).bounds(x, y + row * 3, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.image"), button ->
                this.minecraft.setScreen(new StudioBackgroundScreen(this)))
                .bounds(x + half + 4, y + row * 3, buttonWidth - half - 4, 20).build());

        int applyWidth = 52;
        this.hexInput = new EditBox(this.font, x, y + row * 4, buttonWidth - applyWidth - 4, 20,
                Component.translatable("label.nokhframe.hex"));
        this.hexInput.setMaxLength(7);
        this.hexInput.setValue(customBackground == null ? "#4FC16E" : String.format("#%06X", customBackground & 0xFFFFFF));
        this.hexInput.setHint(Component.literal("#RRGGBB"));
        this.addRenderableWidget(this.hexInput);
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.apply"), button -> applyHex())
                .bounds(x + buttonWidth - applyWidth, y + row * 4, applyWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.left"), button -> yaw -= 30.0F)
                .bounds(x, y + row * 5, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.right"), button -> yaw += 30.0F)
                .bounds(x + half + 4, y + row * 5, buttonWidth - half - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.photo"), button ->
                captureRequested = true).bounds(x, y + row * 6, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean photo = captureRequested;
        renderStageBackground(graphics);
        int stageRight = photo ? this.width : this.width - panelWidth();
        if (itemMode) renderItem(graphics, stageRight);
        else renderAvatar(graphics, stageRight);

        if (!photo) {
            int panelLeft = stageRight;
            graphics.fill(panelLeft, 0, this.width, this.height, 0xEF171B22);
            graphics.drawString(this.font, this.title, panelLeft + 8, 10, 0xFFFFFFFF, false);
            String name = itemMode
                    ? selectedStack.isEmpty() ? "" : selectedStack.getHoverName().getString()
                    : Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getDisplayName().getString();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(name, panelWidth() - 16),
                    panelLeft + 8, 26, 0xFFE7EDF3, false);
            String detail = itemMode ? selectedStack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(selectedStack.getItem()).toString()
                    : skinIndex < 0 ? Component.translatable("skin.nokhframe.current").getString()
                    : skinFiles.get(skinIndex).getFileName().toString();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(detail, panelWidth() - 16),
                    panelLeft + 8, 40, 0xFFB7C3CF, false);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), panelWidth() - 16),
                    panelLeft + 8, this.height - 17, 0xFFDDE5EC, false);
            Component hint = Component.translatable("label.nokhframe.rotate_hint");
            int hintWidth = Math.min(stageRight - 16, this.font.width(hint) + 12);
            graphics.fill(8, this.height - 22, 8 + hintWidth, this.height - 6, 0xB0171B22);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(hint.getString(), hintWidth - 10),
                    13, this.height - 18, 0xFFFFFFFF, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderAvatar(GuiGraphics graphics, int stageRight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        int margin = Math.max(8, stageRight / 15);
        int scale = Math.round(Math.max(25, Math.min((int) ((this.height - 85) / 2.5F),
                (stageRight - 2 * margin) / 3)) * zoom);
        PlayerSkin original = minecraft.player.getSkin();
        SkinOverride.begin(customSkin, original, selectedModel(original.model()));
        try {
            float elapsed = (System.nanoTime() - motionStarted) / 1_000_000_000.0F;
            StudioAvatarRenderer.render(graphics, minecraft.player, margin, 8, stageRight - margin,
                    Math.max(80, this.height - 12), scale, yaw, pitch, roll, motion, elapsed);
        } finally {
            SkinOverride.end();
        }
    }

    private void renderItem(GuiGraphics graphics, int stageRight) {
        if (selectedStack.isEmpty()) {
            Component hint = Component.translatable("label.nokhframe.choose_item");
            graphics.drawCenteredString(this.font, hint, stageRight / 2, this.height / 2, 0xFF30363D);
            return;
        }
        StudioItemRenderer.render(graphics, selectedStack, stageRight, this.height, yaw, pitch, roll, zoom);
    }

    private int backgroundColor() {
        return customBackground == null ? StudioRules.BACKGROUNDS.get(backgroundIndex).color() : customBackground;
    }

    private void renderStageBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, backgroundColor());
        if (backgroundTexture == null) return;
        float scale = Math.max((float) this.width / backgroundWidth, (float) this.height / backgroundHeight);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate((this.width - backgroundWidth * scale) / 2.0F,
                    (this.height - backgroundHeight * scale) / 2.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.blit(backgroundTexture, 0, 0, 0.0F, 0.0F,
                    backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void applyHex() {
        if (hexInput == null) return;
        OptionalInt parsed = StudioRules.parseHexColor(hexInput.getValue());
        if (parsed.isEmpty()) {
            status = Component.translatable("status.nokhframe.invalid_hex");
            return;
        }
        releaseBackground();
        customBackground = parsed.getAsInt();
        status = Component.translatable("status.nokhframe.hex_applied");
    }

    private Component modeLabel() {
        return Component.translatable("button.nokhframe.mode." + (itemMode ? "item" : "player"));
    }

    private Component selectionLabel() {
        return Component.translatable("button.nokhframe.select." + (itemMode ? "item" : "skin"));
    }

    private void updateSelectionButton() {
        if (selectionButton != null) selectionButton.setMessage(selectionLabel());
    }

    private Component motionLabel() {
        return Component.translatable("button.nokhframe.motion",
                Component.translatable("motion.nokhframe." + motion.name().toLowerCase(Locale.ROOT)));
    }

    List<Path> skinFiles() {
        return List.copyOf(skinFiles);
    }

    List<Path> backgroundFiles() {
        reloadBackgrounds();
        return List.copyOf(backgroundFiles);
    }

    Component statusMessage() {
        return status;
    }

    void selectCurrentSkin() {
        skinIndex = -1;
        loadSelectedSkin();
    }

    void selectSkin(Path path) {
        reloadSkins();
        skinIndex = skinFiles.indexOf(path);
        loadSelectedSkin();
    }

    void selectItem(Item item) {
        selectItem(new ItemStack(item));
    }

    void selectItem(ItemStack stack) {
        selectedStack = stack.copy();
        itemMode = true;
        status = Component.translatable("status.nokhframe.item_selected");
    }

    boolean previewingItem() {
        return itemMode;
    }

    ItemStack previewItem() {
        return selectedStack;
    }

    StudioAvatarRenderer.Motion previewMotion() {
        return motion;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && mouseX >= 0 && mouseX < this.width - panelWidth()) {
            draggingPreview = true;
            dragButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && button == dragButton) {
            if (button == 1 || hasShiftDown()) {
                roll = Mth.wrapDegrees(roll + (float) dragX * 0.8F);
                pitch = Mth.wrapDegrees(pitch + (float) dragY * 0.8F);
            } else {
                yaw = Mth.wrapDegrees(yaw + (float) dragX * 0.8F);
                pitch = Mth.wrapDegrees(pitch + (float) dragY * 0.8F);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview && button == dragButton) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= 0 && mouseX < this.width - panelWidth() && scrollY != 0.0) {
            zoom = Mth.clamp((float) (zoom * Math.pow(1.12, scrollY)), 0.35F, 3.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    boolean importSkin(Path source) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!StudioRules.isSkinFile(normalizedSource) || !Files.isRegularFile(normalizedSource)) {
            status = Component.translatable("status.nokhframe.invalid_skin_file");
            return false;
        }
        try (InputStream input = Files.newInputStream(normalizedSource); NativeImage image = NativeImage.read(input)) {
            if (!StudioRules.isSupportedSkinSize(image.getWidth(), image.getHeight())) {
                status = Component.translatable("status.nokhframe.invalid_skin");
                return false;
            }
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.load_error");
            return false;
        }
        try {
            Files.createDirectories(skinDirectory);
            String fileName = normalizedSource.getFileName().toString();
            Path destination = skinDirectory.resolve(fileName);
            if (!Files.isSameFile(normalizedSource.getParent(), skinDirectory)) {
                int dot = fileName.lastIndexOf('.');
                String stem = fileName.substring(0, dot);
                int suffix = 2;
                while (Files.exists(destination)) destination = skinDirectory.resolve(stem + "-" + suffix++ + ".png");
                Files.copy(normalizedSource, destination);
            }
            selectSkin(destination);
            status = Component.translatable("status.nokhframe.imported_skin", destination.getFileName().toString());
            return true;
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.import_error");
            return false;
        }
    }

    boolean importBackground(Path source) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!StudioRules.isSkinFile(normalizedSource) || !Files.isRegularFile(normalizedSource)) {
            status = Component.translatable("status.nokhframe.invalid_background_file");
            return false;
        }
        try {
            if (Files.size(normalizedSource) > 16L * 1024L * 1024L) {
                status = Component.translatable("status.nokhframe.invalid_background_size");
                return false;
            }
            try (InputStream input = Files.newInputStream(normalizedSource); NativeImage image = NativeImage.read(input)) {
                if (!StudioRules.isSupportedBackgroundSize(image.getWidth(), image.getHeight())) {
                    status = Component.translatable("status.nokhframe.invalid_background_size");
                    return false;
                }
            }
            Files.createDirectories(backgroundDirectory);
            String fileName = normalizedSource.getFileName().toString();
            Path destination = backgroundDirectory.resolve(fileName);
            if (!Files.isSameFile(normalizedSource.getParent(), backgroundDirectory)) {
                String stem = fileName.substring(0, fileName.lastIndexOf('.'));
                int suffix = 2;
                while (Files.exists(destination)) destination = backgroundDirectory.resolve(stem + "-" + suffix++ + ".png");
                Files.copy(normalizedSource, destination);
            }
            return selectBackground(destination);
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.import_background_error");
            return false;
        }
    }

    boolean selectBackground(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!StudioRules.isSkinFile(normalized) || !Files.isRegularFile(normalized)) {
            status = Component.translatable("status.nokhframe.invalid_background_file");
            return false;
        }
        try {
            if (Files.size(normalized) > 16L * 1024L * 1024L) {
                status = Component.translatable("status.nokhframe.invalid_background_size");
                return false;
            }
            try (InputStream input = Files.newInputStream(normalized)) {
                NativeImage image = NativeImage.read(input);
                if (!StudioRules.isSupportedBackgroundSize(image.getWidth(), image.getHeight())) {
                    image.close();
                    status = Component.translatable("status.nokhframe.invalid_background_size");
                    return false;
                }
                try {
                    DynamicTexture texture = new DynamicTexture(image);
                    texture.setFilter(true, false);
                    releaseBackground();
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(NokhFrameMod.MOD_ID, "background_preview");
                    Minecraft.getInstance().getTextureManager().register(location, texture);
                    backgroundTexture = location;
                    backgroundWidth = image.getWidth();
                    backgroundHeight = image.getHeight();
                    reloadBackgrounds();
                    status = Component.translatable("status.nokhframe.background_selected", normalized.getFileName().toString());
                    return true;
                } catch (RuntimeException exception) {
                    image.close();
                    throw exception;
                }
            }
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.background_load_error");
            return false;
        }
    }

    private void reloadBackgrounds() {
        backgroundFiles.clear();
        try {
            Files.createDirectories(backgroundDirectory);
            try (Stream<Path> files = Files.list(backgroundDirectory)) {
                files.filter(Files::isRegularFile).filter(StudioRules::isSkinFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(backgroundFiles::add);
            }
        } catch (IOException exception) {
            status = Component.translatable("status.nokhframe.background_folder_error");
        }
    }

    private void reloadSkins() {
        Path previous = skinIndex >= 0 && skinIndex < skinFiles.size() ? skinFiles.get(skinIndex) : null;
        skinFiles.clear();
        try {
            Files.createDirectories(skinDirectory);
            try (Stream<Path> files = Files.list(skinDirectory)) {
                files.filter(Files::isRegularFile).filter(StudioRules::isSkinFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(skinFiles::add);
            }
            skinIndex = previous == null ? -1 : skinFiles.indexOf(previous);
            status = Component.translatable("status.nokhframe.skins", skinFiles.size());
        } catch (IOException exception) {
            skinIndex = -1;
            status = Component.translatable("status.nokhframe.folder_error");
        }
    }

    private void loadSelectedSkin() {
        releaseSkin();
        if (skinIndex < 0) {
            status = Component.translatable("status.nokhframe.current_skin");
            return;
        }
        Path path = skinFiles.get(skinIndex);
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            if (!StudioRules.isSupportedSkinSize(image.getWidth(), image.getHeight())) {
                image.close();
                status = Component.translatable("status.nokhframe.invalid_skin");
                return;
            }
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(NokhFrameMod.MOD_ID, "skin_preview");
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            customSkin = location;
            status = Component.translatable("status.nokhframe.loaded_skin");
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.load_error");
        }
    }

    private PlayerSkin.Model selectedModel(PlayerSkin.Model original) {
        if (skinIndex < 0) return original;
        return switch (StudioRules.skinShape(skinFiles.get(skinIndex).getFileName().toString())) {
            case SLIM -> PlayerSkin.Model.SLIM;
            case WIDE -> PlayerSkin.Model.WIDE;
            case CURRENT -> original;
        };
    }

    private void releaseSkin() {
        if (customSkin != null) {
            Minecraft.getInstance().getTextureManager().release(customSkin);
            customSkin = null;
        }
    }

    private void releaseBackground() {
        if (backgroundTexture != null) {
            Minecraft.getInstance().getTextureManager().release(backgroundTexture);
            backgroundTexture = null;
        }
    }

    private int panelWidth() {
        return Math.min(180, Math.max(136, this.width / 3));
    }

    public boolean takeCapture() {
        if (!captureRequested) return false;
        captureRequested = false;
        return true;
    }

    public void showCaptureResult(Component message) {
        status = message;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) importSkin(path);
    }

    @Override
    public void onClose() {
        releaseSkin();
        releaseBackground();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
