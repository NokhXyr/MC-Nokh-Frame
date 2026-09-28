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
    private final List<Path> skinFiles = new ArrayList<>();
    private Component status = Component.empty();
    private int backgroundIndex;
    private @Nullable Integer customBackground;
    private int skinIndex = -1;
    private float angle;
    private StudioAvatarRenderer.Motion motion = StudioAvatarRenderer.Motion.IDLE;
    private long motionStarted = System.nanoTime();
    private boolean itemMode;
    private @Nullable Item selectedItem;
    private boolean captureRequested;
    private @Nullable ResourceLocation customSkin;
    private @Nullable Button backgroundButton;
    private @Nullable Button selectionButton;
    private @Nullable EditBox hexInput;

    public StudioScreen() {
        super(Component.translatable("screen.nokhframe.title"));
        reloadSkins();
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

        this.backgroundButton = this.addRenderableWidget(Button.builder(backgroundLabel(), button -> {
            customBackground = null;
            backgroundIndex = StudioRules.nextBackground(backgroundIndex);
            button.setMessage(backgroundLabel());
        }).bounds(x, y + row * 3, buttonWidth, 20).build());

        int applyWidth = 52;
        this.hexInput = new EditBox(this.font, x, y + row * 4, buttonWidth - applyWidth - 4, 20,
                Component.translatable("label.nokhframe.hex"));
        this.hexInput.setMaxLength(7);
        this.hexInput.setValue(customBackground == null ? "#4FC16E" : String.format("#%06X", customBackground & 0xFFFFFF));
        this.hexInput.setHint(Component.literal("#RRGGBB"));
        this.addRenderableWidget(this.hexInput);
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.apply"), button -> applyHex())
                .bounds(x + buttonWidth - applyWidth, y + row * 4, applyWidth, 20).build());

        int half = (buttonWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.left"), button -> angle -= 1.5F)
                .bounds(x, y + row * 5, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.right"), button -> angle += 1.5F)
                .bounds(x + half + 4, y + row * 5, buttonWidth - half - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.photo"), button ->
                captureRequested = true).bounds(x, y + row * 6, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean photo = captureRequested;
        graphics.fill(0, 0, this.width, this.height, backgroundColor());
        int stageRight = photo ? this.width : this.width - panelWidth();
        if (itemMode) renderItem(graphics, stageRight);
        else renderAvatar(graphics, stageRight);

        if (!photo) {
            int panelLeft = stageRight;
            graphics.fill(panelLeft, 0, this.width, this.height, 0xEF171B22);
            graphics.drawString(this.font, this.title, panelLeft + 8, 10, 0xFFFFFFFF, false);
            String name = itemMode
                    ? selectedItem == null ? "" : new ItemStack(selectedItem).getHoverName().getString()
                    : Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getDisplayName().getString();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(name, panelWidth() - 16),
                    panelLeft + 8, 26, 0xFFE7EDF3, false);
            String detail = itemMode ? selectedItem == null ? "" : BuiltInRegistries.ITEM.getKey(selectedItem).toString()
                    : skinIndex < 0 ? Component.translatable("skin.nokhframe.current").getString()
                    : skinFiles.get(skinIndex).getFileName().toString();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(detail, panelWidth() - 16),
                    panelLeft + 8, 40, 0xFFB7C3CF, false);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), panelWidth() - 16),
                    panelLeft + 8, this.height - 17, 0xFFDDE5EC, false);
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
        int scale = Math.max(25, Math.min((int) ((this.height - 85) / 2.5F), (stageRight - 2 * margin) / 3));
        PlayerSkin original = minecraft.player.getSkin();
        SkinOverride.begin(customSkin, original, selectedModel(original.model()));
        try {
            float elapsed = (System.nanoTime() - motionStarted) / 1_000_000_000.0F;
            StudioAvatarRenderer.render(graphics, minecraft.player, margin, 8, stageRight - margin,
                    Math.max(80, this.height - 12), scale, angle, motion, elapsed);
        } finally {
            SkinOverride.end();
        }
    }

    private void renderItem(GuiGraphics graphics, int stageRight) {
        if (selectedItem == null) {
            Component hint = Component.translatable("label.nokhframe.choose_item");
            graphics.drawCenteredString(this.font, hint, stageRight / 2, this.height / 2, 0xFF30363D);
            return;
        }
        int size = Math.max(4, Math.min(stageRight / 24, this.height / 24));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(stageRight / 2.0F - 8.0F * size, this.height / 2.0F - 8.0F * size, 100.0F);
            graphics.pose().scale(size, size, size);
            graphics.renderItem(new ItemStack(selectedItem), 0, 0);
        } finally {
            graphics.pose().popPose();
        }
    }

    private int backgroundColor() {
        return customBackground == null ? StudioRules.BACKGROUNDS.get(backgroundIndex).color() : customBackground;
    }

    private void applyHex() {
        if (hexInput == null) return;
        OptionalInt parsed = StudioRules.parseHexColor(hexInput.getValue());
        if (parsed.isEmpty()) {
            status = Component.translatable("status.nokhframe.invalid_hex");
            return;
        }
        customBackground = parsed.getAsInt();
        status = Component.translatable("status.nokhframe.hex_applied");
        if (backgroundButton != null) backgroundButton.setMessage(backgroundLabel());
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

    private Component backgroundLabel() {
        Component value = customBackground == null
                ? Component.translatable("background.nokhframe." + StudioRules.BACKGROUNDS.get(backgroundIndex).name())
                : Component.literal(String.format("#%06X", customBackground & 0xFFFFFF));
        return Component.translatable("button.nokhframe.background", value);
    }

    List<Path> skinFiles() {
        return List.copyOf(skinFiles);
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
        selectedItem = item;
        itemMode = true;
        status = Component.translatable("status.nokhframe.item_selected");
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
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
