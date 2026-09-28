package NokhXyr.NokhFrame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StudioCatalogScreen extends Screen {
    private final StudioScreen parent;
    private final boolean items;
    private final List<Entry> all = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private final List<Button> rows = new ArrayList<>();
    private @Nullable EditBox search;
    private @Nullable EditBox pathInput;
    private @Nullable Button previous;
    private @Nullable Button next;
    private String query = "";
    private int page;
    private int pageSize;

    public StudioCatalogScreen(StudioScreen parent, boolean items) {
        super(Component.translatable("screen.nokhframe.catalog." + (items ? "items" : "skins")));
        this.parent = parent;
        this.items = items;
        loadEntries();
    }

    private void loadEntries() {
        all.clear();
        if (items) {
            BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).forEach(item -> {
                ItemStack stack = new ItemStack(item);
                all.add(new Entry(stack.getHoverName().getString(), BuiltInRegistries.ITEM.getKey(item).toString(), null, item));
            });
            all.sort(Comparator.comparing(Entry::id));
        } else {
            all.add(new Entry(Component.translatable("skin.nokhframe.current").getString(), "", null, null));
            for (Path path : parent.skinFiles()) {
                all.add(new Entry(path.getFileName().toString(), path.getFileName().toString(), path, null));
            }
        }
        filter();
    }

    @Override
    protected void init() {
        int width = Math.min(360, this.width - 24);
        int x = (this.width - width) / 2;
        pageSize = Math.max(3, Math.min(9, (this.height - (items ? 112 : 150)) / 22));
        search = new EditBox(this.font, x, 34, width, 20, Component.translatable("label.nokhframe.search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; filter(); refreshRows(); });
        search.setHint(Component.translatable("label.nokhframe.search"));
        this.addRenderableWidget(search);
        for (int i = 0; i < pageSize; i++) {
            final int slot = i;
            Button row = this.addRenderableWidget(Button.builder(Component.empty(), button -> choose(slot))
                    .bounds(x + (items ? 22 : 0), 60 + i * 22, width - (items ? 22 : 0), 20).build());
            rows.add(row);
        }
        int navY = 64 + pageSize * 22;
        previous = this.addRenderableWidget(Button.builder(Component.literal("←"), button -> { page--; refreshRows(); })
                .bounds(x, navY, 42, 20).build());
        next = this.addRenderableWidget(Button.builder(Component.literal("→"), button -> { page++; refreshRows(); })
                .bounds(x + width - 42, navY, 42, 20).build());
        if (!items) {
            pathInput = new EditBox(this.font, x, navY + 24, width, 20,
                    Component.translatable("label.nokhframe.skin_path"));
            pathInput.setMaxLength(1024);
            pathInput.setHint(Component.translatable("label.nokhframe.skin_path"));
            this.addRenderableWidget(pathInput);
            this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.import"), button -> importPath())
                    .bounds(x, navY + 48, width / 2 - 2, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> onClose())
                    .bounds(x + width / 2 + 2, navY + 48, width / 2 - 2, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> onClose())
                    .bounds(x, navY + 24, width, 20).build());
        }
        refreshRows();
    }

    private void filter() {
        filtered.clear();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (Entry entry : all) {
            if (entry.name().toLowerCase(Locale.ROOT).contains(needle) || entry.id().toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(entry);
            }
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
            if (button.visible) {
                Entry entry = filtered.get(index);
                button.setMessage(Component.literal(this.font.plainSubstrByWidth(entry.name(), button.getWidth() - 14)));
            }
        }
        if (previous != null) previous.active = page > 0;
        if (next != null) next.active = page + 1 < pages;
    }

    private void choose(int slot) {
        int index = page * pageSize + slot;
        if (index >= filtered.size()) return;
        Entry entry = filtered.get(index);
        if (items && entry.item() != null) parent.selectItem(entry.item());
        else if (entry.path() == null) parent.selectCurrentSkin();
        else parent.selectSkin(entry.path());
        this.minecraft.setScreen(parent);
    }

    private void importPath() {
        if (pathInput == null || pathInput.getValue().isBlank()) return;
        String raw = pathInput.getValue().trim();
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) raw = raw.substring(1, raw.length() - 1);
        try {
            if (parent.importSkin(Path.of(raw))) this.minecraft.setScreen(parent);
        } catch (InvalidPathException exception) {
            pathInput.setValue("");
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (items) return;
        boolean imported = false;
        for (Path path : paths) imported |= parent.importSkin(path);
        if (imported) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF20242B);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 0xFFFFFFFF);
        int width = Math.min(360, this.width - 24);
        int x = (this.width - width) / 2;
        if (items) {
            for (int i = 0; i < rows.size(); i++) {
                int index = page * pageSize + i;
                if (index < filtered.size()) graphics.renderItem(new ItemStack(filtered.get(index).item()), x + 2, 62 + i * 22);
            }
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("label.nokhframe.drop_skin"),
                    this.width / 2, this.height - 12, 0xFFB8C6D2);
            graphics.drawCenteredString(this.font, parent.statusMessage(), this.width / 2,
                    this.height - 25, 0xFFE1E8EF);
        }
        graphics.drawCenteredString(this.font, Component.literal((page + 1) + " / " + Math.max(1, (filtered.size() + pageSize - 1) / pageSize)
                + "  ·  " + filtered.size()), this.width / 2, 68 + pageSize * 22, 0xFFB8C6D2);
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

    private record Entry(String name, String id, @Nullable Path path, @Nullable Item item) {
    }
}
