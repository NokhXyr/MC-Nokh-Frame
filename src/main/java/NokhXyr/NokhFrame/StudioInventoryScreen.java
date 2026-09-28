package NokhXyr.NokhFrame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Read-only picker: selecting a slot copies the complete stack, including data components. */
public final class StudioInventoryScreen extends Screen {
    private final StudioScreen parent;
    private final List<Button> slots = new ArrayList<>();

    public StudioInventoryScreen(StudioScreen parent) {
        super(Component.translatable("screen.nokhframe.inventory"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - 216) / 2;
        int top = Math.max(36, (this.height - 170) / 2);
        for (int visual = 0; visual < 41; visual++) {
            int index = visual < 27 ? visual + 9 : visual < 36 ? visual - 27 : visual;
            int column = visual % 9;
            int row = visual / 9;
            Button button = this.addRenderableWidget(Button.builder(Component.empty(), ignored -> choose(index))
                    .bounds(left + column * 24, top + row * 24, 22, 22).build());
            slots.add(button);
        }
        this.addRenderableWidget(Button.builder(Component.translatable("button.nokhframe.back"), button -> onClose())
                .bounds(left, top + 124, 216, 20).build());
    }

    private void choose(int index) {
        if (this.minecraft.player == null) return;
        ItemStack stack = this.minecraft.player.getInventory().getItem(index);
        if (stack.isEmpty()) return;
        parent.selectItem(stack.copy());
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF20242B);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 13, 0xFFFFFFFF);
        if (this.minecraft.player == null) return;
        Inventory inventory = this.minecraft.player.getInventory();
        ItemStack hovered = ItemStack.EMPTY;
        for (int visual = 0; visual < slots.size(); visual++) {
            int index = visual < 27 ? visual + 9 : visual < 36 ? visual - 27 : visual;
            ItemStack stack = inventory.getItem(index);
            Button button = slots.get(visual);
            button.active = !stack.isEmpty();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int visual = 0; visual < slots.size(); visual++) {
            int index = visual < 27 ? visual + 9 : visual < 36 ? visual - 27 : visual;
            ItemStack stack = inventory.getItem(index);
            Button button = slots.get(visual);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, button.getX() + 3, button.getY() + 3);
                if (button.isMouseOver(mouseX, mouseY)) hovered = stack;
            }
        }
        if (!hovered.isEmpty()) graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new StudioCatalogScreen(parent, true));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
