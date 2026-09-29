package NokhXyr.NokhFrame;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

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
    private final Path sceneDirectory = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("nokhframe").resolve("scenes");
    private final List<Path> skinFiles = new ArrayList<>();
    private final List<Path> backgroundFiles = new ArrayList<>();
    private final List<Path> sceneFiles = new ArrayList<>();
    private @Nullable StudioScene scene;
    private Component status = Component.empty();
    private int backgroundIndex;
    private @Nullable Integer customBackground;
    private int skinIndex = -1;
    private float yaw;
    private float pitch;
    private float roll;
    private float zoom = 1.0F;
    private float playerSize = 1.0F;
    private boolean worldMode;
    private @Nullable Float worldFacing;
    private final StudioPlacement placement = new StudioPlacement();
    private StudioPlacement.Target placementTarget = StudioPlacement.Target.PLAYER;
    private boolean placementPage;
    private boolean posePage;
    private final StudioPose studioPose = new StudioPose();
    private StudioPose.Part posePart = StudioPose.Part.RIGHT_ARM;
    private int stepIndex = 1;
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
        if (placementPage) {
            initPlacement(x, y, row, buttonWidth);
            return;
        }
        if (posePage) {
            initPose(x, y, row, buttonWidth);
            return;
        }

        int half = (buttonWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(modeLabel(), button -> {
            itemMode = !itemMode;
            button.setMessage(modeLabel());
            updateSelectionButton();
        }).bounds(x, y, half, 20).build());
        this.addRenderableWidget(Button.builder(renderLabel(), button -> {
            worldMode = !worldMode;
            worldFacing = null;
            button.setMessage(renderLabel());
            status = Component.translatable("status.nokhframe.render." + (worldMode ? "world" : "studio"));
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.render_world")))
                .bounds(x + half + 4, y, buttonWidth - half - 4, 20).build());

        this.selectionButton = this.addRenderableWidget(Button.builder(selectionLabel(), button ->
                this.minecraft.setScreen(new StudioCatalogScreen(this, itemMode)))
                .bounds(x, y + row, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(motionLabel(), button -> {
            StudioAvatarRenderer.Motion[] modes = StudioAvatarRenderer.Motion.values();
            motion = modes[(motion.ordinal() + 1) % modes.length];
            motionStarted = System.nanoTime();
            button.setMessage(motionLabel());
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.motion")))
                .bounds(x, y + row * 2, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.pose"), button -> {
            posePage = true;
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.pose")))
                .bounds(x + half + 4, y + row * 2, buttonWidth - half - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.color"), button -> {
            releaseBackground();
            customBackground = null;
            backgroundIndex = StudioRules.nextBackground(backgroundIndex);
            status = Component.translatable("status.nokhframe.background_color",
                    Component.translatable("background.nokhframe." + StudioRules.BACKGROUNDS.get(backgroundIndex).name()));
        }).bounds(x, y + row * 3, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.image"), button ->
                this.minecraft.setScreen(new StudioBackgroundScreen(this, StudioBackgroundScreen.Kind.IMAGE)))
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

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.scene"), button ->
                this.minecraft.setScreen(new StudioBackgroundScreen(this, StudioBackgroundScreen.Kind.SCENE)))
                .bounds(x, y + row * 5, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.placement"), button -> {
            placementPage = true;
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.placement")))
                .bounds(x + half + 4, y + row * 5, buttonWidth - half - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.photo"), button ->
                captureRequested = true).bounds(x, y + row * 6, buttonWidth, 20).build());
    }

    /** Placement page: moves, turns and sizes the player, the scene or the view, one step per click. */
    private void initPlacement(int x, int y, int row, int buttonWidth) {
        int half = (buttonWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(targetLabel(), button -> {
            StudioPlacement.Target[] targets = StudioPlacement.Target.values();
            placementTarget = targets[(placementTarget.ordinal() + 1) % targets.length];
            button.setMessage(targetLabel());
        }).bounds(x, y, half, 20).build());
        this.addRenderableWidget(Button.builder(stepLabel(), button -> {
            stepIndex = StudioPlacement.nextStep(stepIndex);
            button.setMessage(stepLabel());
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.step")))
                .bounds(x + half + 4, y, buttonWidth - half - 4, 20).build());
        for (int field = 0; field < PLACEMENT_FIELDS; field++) {
            final int index = field;
            int rowY = y + row * (field + 1);
            this.addRenderableWidget(Button.builder(Component.literal("−"), button -> adjustPlacement(index, -1))
                    .bounds(x, rowY, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustPlacement(index, 1))
                    .bounds(x + buttonWidth - 20, rowY, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.reset"), button -> {
            placement.reset(placementTarget);
            if (placementTarget == StudioPlacement.Target.PLAYER) playerSize = 1.0F;
            if (placementTarget == StudioPlacement.Target.VIEW) {
                yaw = pitch = roll = 0.0F;
                zoom = 1.0F;
            }
            status = Component.translatable("status.nokhframe.placement_reset", targetName());
        }).bounds(x, y + row * 6, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> {
            placementPage = false;
            rebuildWidgets();
        }).bounds(x + half + 4, y + row * 6, buttonWidth - half - 4, 20).build());
    }

    private static final int PLACEMENT_FIELDS = 5;

    /** Pose page: preset poses, then per-part X/Y/Z rotations (5° per click, Shift 1°, Ctrl 15°). */
    private void initPose(int x, int y, int row, int buttonWidth) {
        int half = (buttonWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(presetLabel(), button -> {
            studioPose.nextPreset();
            button.setMessage(presetLabel());
            status = Component.translatable("status.nokhframe.pose_preset", presetName());
        }).bounds(x, y, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(partLabel(), button -> {
            StudioPose.Part[] parts = StudioPose.Part.values();
            posePart = parts[(posePart.ordinal() + 1) % parts.length];
            button.setMessage(partLabel());
        }).bounds(x, y + row, buttonWidth, 20).build());
        for (int axis = 0; axis < 3; axis++) {
            final int index = axis;
            int rowY = y + row * (axis + 2);
            this.addRenderableWidget(Button.builder(Component.literal("−"), button -> adjustPose(index, -1))
                    .bounds(x, rowY, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustPose(index, 1))
                    .bounds(x + buttonWidth - 20, rowY, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.reset_part"), button -> {
            studioPose.resetPart(posePart);
            rebuildWidgets();
        }).bounds(x, y + row * 5, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.mirror"), button -> {
            studioPose.mirror(posePart);
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.translatable("tooltip.nokhframe.mirror")))
                .bounds(x + half + 4, y + row * 5, buttonWidth - half - 4, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> {
            posePage = false;
            rebuildWidgets();
        }).bounds(x, y + row * 6, buttonWidth, 20).build());
    }

    private void adjustPose(int axis, int direction) {
        float step = hasShiftDown() ? 1.0F : hasControlDown() ? 15.0F : 5.0F;
        studioPose.adjust(posePart, axis, step * direction);
        // The preset button shows "custom" after the first edit.
        rebuildWidgets();
    }

    private Component presetLabel() {
        return Component.translatable("button.nokhframe.pose_preset", presetName());
    }

    private Component presetName() {
        return Component.translatable("pose.nokhframe.preset." + studioPose.preset().name().toLowerCase(Locale.ROOT));
    }

    private Component partLabel() {
        return Component.translatable("button.nokhframe.pose_part",
                Component.translatable("pose.nokhframe.part." + posePart.name().toLowerCase(Locale.ROOT)));
    }

    private Component poseAxis(int axis) {
        String name = switch (axis) {
            case 0 -> "x";
            case 1 -> "y";
            default -> "z";
        };
        return Component.translatable("pose.nokhframe.axis." + name, Math.round(studioPose.angle(posePart, axis)));
    }

    StudioPose pose() {
        return studioPose;
    }

    private Component targetLabel() {
        return Component.translatable("button.nokhframe.target", targetName());
    }

    private Component targetName() {
        return Component.translatable("placement.nokhframe.target." + placementTarget.name().toLowerCase(Locale.ROOT));
    }

    private Component stepLabel() {
        return Component.translatable("button.nokhframe.step", formatNumber(StudioPlacement.STEPS[stepIndex]));
    }

    private static String formatNumber(float value) {
        String text = String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.equals("-0") ? "0" : text;
    }

    /** Label and current value of a placement row, for the selected target. */
    private Component placementField(int field) {
        StudioPlacement p = placement;
        return switch (placementTarget) {
            case PLAYER -> switch (field) {
                case 0 -> axis("x", p.playerX);
                case 1 -> axis("y", p.playerY);
                case 2 -> axis("z", p.playerZ);
                case 3 -> Component.translatable("placement.nokhframe.rotation", Math.round(p.playerRotation));
                default -> Component.translatable("placement.nokhframe.size", Math.round(playerSize * 100.0F));
            };
            case SCENE -> switch (field) {
                case 0 -> axis("x", p.sceneX);
                case 1 -> axis("y", p.sceneY);
                case 2 -> axis("z", p.sceneZ);
                case 3 -> Component.translatable("placement.nokhframe.rotation", Math.round(p.sceneRotation));
                default -> Component.translatable("placement.nokhframe.scale", Math.round(p.sceneScale * 100.0F));
            };
            case VIEW -> switch (field) {
                case 0 -> axis("pan_x", p.panX);
                case 1 -> axis("pan_y", -p.panY);
                case 2 -> Component.translatable("placement.nokhframe.zoom", Math.round(zoom * 100.0F));
                case 3 -> Component.translatable("placement.nokhframe.rotation", Math.round(yaw));
                default -> Component.translatable("placement.nokhframe.pitch", Math.round(pitch));
            };
        };
    }

    private static Component axis(String name, float value) {
        return Component.translatable("placement.nokhframe." + name, formatNumber(value));
    }

    private void adjustPlacement(int field, int direction) {
        float step = StudioPlacement.STEPS[stepIndex] * direction;
        float turn = StudioPlacement.ROTATION_STEP * direction;
        StudioPlacement p = placement;
        switch (placementTarget) {
            case PLAYER -> {
                switch (field) {
                    case 0 -> p.playerX = StudioPlacement.move(p.playerX, step);
                    case 1 -> p.playerY = StudioPlacement.move(p.playerY, step);
                    case 2 -> p.playerZ = StudioPlacement.move(p.playerZ, step);
                    case 3 -> p.playerRotation = StudioPlacement.rotate(p.playerRotation, turn);
                    default -> playerSize = StudioRules.nextPlayerSize(playerSize, direction);
                }
            }
            case SCENE -> {
                switch (field) {
                    case 0 -> p.sceneX = StudioPlacement.move(p.sceneX, step);
                    case 1 -> p.sceneY = StudioPlacement.move(p.sceneY, step);
                    case 2 -> p.sceneZ = StudioPlacement.move(p.sceneZ, step);
                    case 3 -> p.sceneRotation = StudioPlacement.rotate(p.sceneRotation, turn);
                    default -> p.sceneScale = StudioPlacement.nextSceneScale(p.sceneScale, direction);
                }
            }
            case VIEW -> {
                switch (field) {
                    case 0 -> p.panX = StudioPlacement.move(p.panX, step);
                    case 1 -> p.panY = StudioPlacement.move(p.panY, -step);
                    case 2 -> zoom = StudioRules.clampZoom((float) (zoom * Math.pow(1.12, direction)));
                    case 3 -> yaw = StudioPlacement.rotate(yaw, turn);
                    default -> pitch = StudioPlacement.rotate(pitch, turn);
                }
            }
        }
    }

    StudioPlacement placement() {
        return placement;
    }

    /** GUI pixels per block for the player preview, before the player size. */
    private float stageScale() {
        int stageRight = this.width - panelWidth();
        int margin = Math.max(8, stageRight / 15);
        float entityScale = Minecraft.getInstance().player == null ? 1.0F : Minecraft.getInstance().player.getScale();
        return Math.round(Math.max(25, Math.min((int) ((this.height - 85) / 2.5F),
                (stageRight - 2 * margin) / 3)) * zoom) / entityScale;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean photo = captureRequested;
        int stageRight = photo ? this.width : this.width - panelWidth();
        // In the world view the game render (and any shaderpack) is the stage: draw nothing over it.
        if (!worldView()) {
            renderStageBackground(graphics);
            // The flat background writes depth at z=0; clear it so a 3D set can extend behind that plane.
            clearDepth(graphics);
            if (itemMode) renderItem(graphics, stageRight);
            else renderAvatar(graphics, stageRight);
            clearDepth(graphics);
        }

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
            if (placementPage) {
                int row = Math.max(21, Math.min(24, (this.height - 82) / 7));
                int centerX = panelLeft + panelWidth() / 2;
                for (int field = 0; field < PLACEMENT_FIELDS; field++) {
                    String text = this.font.plainSubstrByWidth(placementField(field).getString(), panelWidth() - 64);
                    graphics.drawCenteredString(this.font, text, centerX, 66 + row * (field + 1) + 6, 0xFFE7EDF3);
                }
            }
            if (posePage) {
                int row = Math.max(21, Math.min(24, (this.height - 82) / 7));
                int centerX = panelLeft + panelWidth() / 2;
                for (int axis = 0; axis < 3; axis++) {
                    graphics.drawCenteredString(this.font, poseAxis(axis), centerX, 66 + row * (axis + 2) + 6, 0xFFE7EDF3);
                }
            }
            Component hint = Component.translatable(placementPage ? "label.nokhframe.placement_hint"
                    : posePage ? "label.nokhframe.pose_hint" : "label.nokhframe.rotate_hint");
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
        beginSkinOverride();
        try {
            StudioAvatarRenderer.render(graphics, minecraft.player, margin, 8, stageRight - margin,
                    Math.max(80, this.height - 12), scale, yaw, pitch, roll, motion, motionElapsed(),
                    scene, stageRight, this.height, playerSize, placement, studioPose);
        } finally {
            SkinOverride.end();
        }
    }

    /** Applies the selected studio skin to the local player until {@link SkinOverride#end()}. */
    void beginSkinOverride() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        PlayerSkin original = minecraft.player.getSkin();
        SkinOverride.begin(customSkin, original, selectedModel(original.model()));
    }

    float motionElapsed() {
        return (System.nanoTime() - motionStarted) / 1_000_000_000.0F;
    }

    /** Facing used by the world view, fixed the first time it is asked for so the shot does not drift. */
    float worldFacing(float currentYaw) {
        if (worldFacing == null) worldFacing = currentYaw;
        return worldFacing;
    }

    /**
     * True when the player is shown through the real world render instead of the GUI: the World mode, or the
     * Studio mode while a shaderpack is active (shaderpacks never process GUI rendering).
     */
    boolean worldView() {
        return !itemMode && (worldMode || ShaderSupport.shaderPackInUse());
    }

    /** Studio mode drawn through the world pipeline for shaders: the world is hidden behind a studio backdrop. */
    boolean shaderStudio() {
        return !itemMode && !worldMode && ShaderSupport.shaderPackInUse();
    }

    int backdropColor() {
        return backgroundColor();
    }

    @Nullable ResourceLocation backdropTexture() {
        return backgroundTexture;
    }

    int backdropWidth() {
        return backgroundWidth;
    }

    int backdropHeight() {
        return backgroundHeight;
    }

    float viewYaw() {
        return yaw;
    }

    float viewPitch() {
        return pitch;
    }

    float viewRoll() {
        return roll;
    }

    float viewZoom() {
        return zoom;
    }

    float playerSize() {
        return playerSize;
    }

    @Nullable StudioScene scene() {
        return scene;
    }

    /** Share of the window width used by the stage; the side panel is hidden while a photo is taken. */
    float stageFraction() {
        return captureRequested || this.width <= 0 ? 1.0F : (float) (this.width - panelWidth()) / this.width;
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

    private static void clearDepth(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
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

    private Component renderLabel() {
        return Component.translatable("button.nokhframe.render." + (worldMode ? "world" : "studio"));
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
        if ((button == 0 || button == 1 || button == 2) && mouseX >= 0 && mouseX < this.width - panelWidth()) {
            draggingPreview = true;
            dragButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && button == dragButton) {
            if (button == 2) {
                // Middle drag pans the view; the content follows the cursor.
                float perBlock = Math.max(1.0F, stageScale());
                placement.panX = StudioPlacement.move(placement.panX, (float) dragX / perBlock);
                placement.panY = StudioPlacement.move(placement.panY, (float) dragY / perBlock);
            } else if (button == 1 || hasShiftDown()) {
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
            if (hasControlDown()) {
                playerSize = StudioRules.nextPlayerSize(playerSize, scrollY);
                status = Component.translatable("status.nokhframe.player_size", Math.round(playerSize * 100.0F));
            } else {
                zoom = StudioRules.clampZoom((float) (zoom * Math.pow(1.12, scrollY)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (placementPage && getFocused() == null) {
            // Arrows move along X and Z (away from the viewer), Page Up / Page Down along Y.
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> adjustPlacement(0, -1);
                case GLFW.GLFW_KEY_RIGHT -> adjustPlacement(0, 1);
                case GLFW.GLFW_KEY_PAGE_UP -> adjustPlacement(1, 1);
                case GLFW.GLFW_KEY_PAGE_DOWN -> adjustPlacement(1, -1);
                case GLFW.GLFW_KEY_UP -> adjustPlacement(2, 1);
                case GLFW.GLFW_KEY_DOWN -> adjustPlacement(2, -1);
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    List<Path> sceneFiles() {
        reloadScenes();
        return List.copyOf(sceneFiles);
    }

    boolean importScene(Path source) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!StudioRules.isSceneFile(normalizedSource) || !Files.isRegularFile(normalizedSource)) {
            status = Component.translatable("status.nokhframe.invalid_scene_file");
            return false;
        }
        try {
            Files.createDirectories(sceneDirectory);
            if (Files.isSameFile(normalizedSource.getParent(), sceneDirectory)) return selectScene(normalizedSource);
            // Validate before copying so a broken file never lands in the library.
            if (!selectScene(normalizedSource)) return false;
            String fileName = normalizedSource.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String stem = fileName.substring(0, dot);
            String extension = fileName.substring(dot);
            Path destination = sceneDirectory.resolve(fileName);
            int suffix = 2;
            while (Files.exists(destination)) destination = sceneDirectory.resolve(stem + "-" + suffix++ + extension);
            Files.copy(normalizedSource, destination);
            if (extension.equalsIgnoreCase(".json")) copyModelTextures(normalizedSource.getParent());
            reloadScenes();
            status = Component.translatable("status.nokhframe.scene_selected", destination.getFileName().toString());
            return true;
        } catch (IOException | RuntimeException exception) {
            status = Component.translatable("status.nokhframe.import_scene_error");
            return false;
        }
    }

    /** Java model exports reference PNG files next to them; bring those along without replacing existing ones. */
    private void copyModelTextures(Path sourceFolder) throws IOException {
        try (Stream<Path> files = Files.list(sourceFolder)) {
            for (Path png : files.filter(Files::isRegularFile).filter(StudioRules::isSkinFile).toList()) {
                Path destination = sceneDirectory.resolve(png.getFileName().toString());
                if (!Files.exists(destination)) Files.copy(png, destination);
            }
        }
    }

    boolean selectScene(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!StudioRules.isSceneFile(normalized) || !Files.isRegularFile(normalized)) {
            status = Component.translatable("status.nokhframe.invalid_scene_file");
            return false;
        }
        try {
            if (Files.size(normalized) > StudioRules.MAX_SCENE_BYTES) {
                status = Component.translatable("status.nokhframe.invalid_scene_size");
                return false;
            }
            StudioScene loaded = StudioScene.load(normalized);
            releaseScene();
            scene = loaded;
            status = Component.translatable("status.nokhframe.scene_selected", normalized.getFileName().toString());
            return true;
        } catch (StudioScene.SceneException exception) {
            status = Component.translatable(exception.translationKey());
        } catch (IOException | RuntimeException exception) {
            NokhFrameMod.LOGGER.warn("Could not load studio scene {}", normalized, exception);
            status = Component.translatable("status.nokhframe.scene_load_error");
        }
        return false;
    }

    void clearScene() {
        releaseScene();
        status = Component.translatable("status.nokhframe.scene_cleared");
    }

    private void releaseScene() {
        if (scene != null) {
            scene.close();
            scene = null;
        }
    }

    private void reloadScenes() {
        sceneFiles.clear();
        try {
            Files.createDirectories(sceneDirectory);
            try (Stream<Path> files = Files.list(sceneDirectory)) {
                files.filter(Files::isRegularFile).filter(StudioRules::isSceneFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(sceneFiles::add);
            }
        } catch (IOException exception) {
            status = Component.translatable("status.nokhframe.scene_folder_error");
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
        releaseScene();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
