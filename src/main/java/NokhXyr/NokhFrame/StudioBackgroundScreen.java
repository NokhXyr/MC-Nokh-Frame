package NokhXyr.NokhFrame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** PNG library shared by player and item photo previews. */
public final class StudioBackgroundScreen extends Screen {
    private final StudioScreen parent;
    private final List<Path> all;
    private final List<Path> filtered = new ArrayList<>();
    private final List<Button> rows = new ArrayList<>();
    private @Nullable EditBox search;
    private @Nullable EditBox pathInput;
    private @Nullable Button previous;
    private @Nullable Button next;
    private String query = "";
    private int page;
    private int pageSize;

    public StudioBackgroundScreen(StudioScreen parent) {
        super(Component.translatable("screen.nokhframe.backgrounds"));
        this.parent = parent;
        this.all = parent.backgroundFiles();
        filter();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(360, this.width - 24);
        int x = (this.width - contentWidth) / 2;
        pageSize = Math.max(3, Math.min(9, (this.height - 150) / 22));
        search = new EditBox(this.font, x, 34, contentWidth, 20, Component.translatable("label.nokhframe.search_background"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; filter(); refreshRows(); });
        search.setHint(Component.translatable("label.nokhframe.search_background"));
        this.addRenderableWidget(search);
        for (int i = 0; i < pageSize; i++) {
            final int slot = i;
            Button row = this.addRenderableWidget(Button.builder(Component.empty(), button -> choose(slot))
                    .bounds(x, 60 + i * 22, contentWidth, 20).build());
            rows.add(row);
        }
        int navY = 64 + pageSize * 22;
        previous = this.addRenderableWidget(Button.builder(Component.literal("←"), button -> { page--; refreshRows(); })
                .bounds(x, navY, 42, 20).build());
        next = this.addRenderableWidget(Button.builder(Component.literal("→"), button -> { page++; refreshRows(); })
                .bounds(x + contentWidth - 42, navY, 42, 20).build());
        pathInput = new EditBox(this.font, x, navY + 24, contentWidth, 20,
                Component.translatable("label.nokhframe.background_path"));
        pathInput.setMaxLength(1024);
        pathInput.setHint(Component.translatable("label.nokhframe.background_path"));
        this.addRenderableWidget(pathInput);
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.import"), button -> importPath())
                .bounds(x, navY + 48, contentWidth / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> onClose())
                .bounds(x + contentWidth / 2 + 2, navY + 48, contentWidth / 2 - 2, 20).build());
        refreshRows();
    }

    private void filter() {
        filtered.clear();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (Path path : all) {
            if (path.getFileName().toString().toLowerCase(Locale.ROOT).contains(needle)) filtered.add(path);
        }
    }

    private void refreshRows() {
        if (rows.isEmpty()) return;
        int pages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < rows.size(); i++) {
            int index = page * pageSize + i;
            Button button = rows.get(i);
            button.visible = index < filtered.size();
            if (button.visible) button.setMessage(Component.literal(
                    this.font.plainSubstrByWidth(filtered.get(index).getFileName().toString(), button.getWidth() - 14)));
        }
        if (previous != null) previous.active = page > 0;
        if (next != null) next.active = page + 1 < pages;
    }

    private void choose(int slot) {
        int index = page * pageSize + slot;
        if (index < filtered.size() && parent.selectBackground(filtered.get(index))) this.minecraft.setScreen(parent);
    }

    private void importPath() {
        if (pathInput == null || pathInput.getValue().isBlank()) return;
        String raw = pathInput.getValue().trim();
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) raw = raw.substring(1, raw.length() - 1);
        try {
            if (parent.importBackground(Path.of(raw))) this.minecraft.setScreen(parent);
        } catch (InvalidPathException exception) {
            parent.showCaptureResult(Component.translatable("status.nokhframe.invalid_background_file"));
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        boolean imported = false;
        for (Path path : paths) imported |= parent.importBackground(path);
        if (imported) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF20242B);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, parent.statusMessage(), this.width / 2,
                this.height - 25, 0xFFE1E8EF);
        graphics.drawCenteredString(this.font, Component.translatable("label.nokhframe.drop_background"),
                this.width / 2, this.height - 12, 0xFFB8C6D2);
        graphics.drawCenteredString(this.font, Component.literal((page + 1) + " / "
                + Math.max(1, (filtered.size() + pageSize - 1) / pageSize) + "  ·  " + filtered.size()),
                this.width / 2, 68 + pageSize * 22, 0xFFB8C6D2);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
