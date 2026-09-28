package NokhXyr.NokhFrame;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Path;

@GameTestHolder(NokhFrameMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NokhFrameGameTests {
    private NokhFrameGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void backgroundsCycleInOrder(GameTestHelper helper) {
        String[] expected = {"green", "white", "gray", "blue", "black"};
        helper.assertTrue(StudioRules.BACKGROUNDS.size() == expected.length, "Expected five backgrounds");
        int index = 0;
        for (String name : expected) {
            helper.assertTrue(StudioRules.BACKGROUNDS.get(index).name().equals(name), "Wrong background order: " + name);
            index = StudioRules.nextBackground(index);
        }
        helper.assertTrue(index == 0, "Background selection must wrap to green");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void skinsCycleThroughCurrentAndFiles(GameTestHelper helper) {
        helper.assertTrue(StudioRules.nextSkin(-1, 0) == -1, "No skin files should keep the current skin");
        helper.assertTrue(StudioRules.nextSkin(-1, 2) == 0, "First click should select the first PNG");
        helper.assertTrue(StudioRules.nextSkin(0, 2) == 1, "Second click should select the second PNG");
        helper.assertTrue(StudioRules.nextSkin(1, 2) == -1, "Selection should return to the game skin");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void skinFilesAndShapes(GameTestHelper helper) {
        helper.assertTrue(StudioRules.isSkinFile(Path.of("Alex_SLIM.PNG")), "PNG extension should be case insensitive");
        helper.assertFalse(StudioRules.isSkinFile(Path.of("skin.jpg")), "Only PNG files should be listed");
        helper.assertTrue(StudioRules.isSupportedSkinSize(64, 64), "Standard 64x64 skin should load");
        helper.assertFalse(StudioRules.isSupportedSkinSize(64, 32), "Legacy skin dimensions should be rejected");
        helper.assertTrue(StudioRules.skinShape("Alex_SLIM.PNG") == StudioRules.SkinShape.SLIM, "Slim suffix should select slim arms");
        helper.assertTrue(StudioRules.skinShape("Steve_wide.png") == StudioRules.SkinShape.WIDE, "Wide suffix should select standard arms");
        helper.assertTrue(StudioRules.skinShape("custom.png") == StudioRules.SkinShape.CURRENT, "Untagged skin should retain the player's model");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void customBackgroundAcceptsOpaqueRgbOnly(GameTestHelper helper) {
        helper.assertTrue(StudioRules.parseHexColor("#12aBcD").orElse(0) == 0xFF12ABCD, "Hex input should accept a leading #");
        helper.assertTrue(StudioRules.parseHexColor(" 4FC16E ").orElse(0) == 0xFF4FC16E, "Hex input should accept whitespace");
        helper.assertFalse(StudioRules.parseHexColor("#FFF").isPresent(), "Three digit colors are unsupported");
        helper.assertFalse(StudioRules.parseHexColor("#12345678").isPresent(), "Alpha must not make the screenshot translucent");
        helper.assertFalse(StudioRules.parseHexColor("#GG0000").isPresent(), "Invalid digits must be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void backgroundImageDimensionsHaveLimits(GameTestHelper helper) {
        helper.assertTrue(StudioRules.isSupportedBackgroundSize(1920, 1080), "Common background image should load");
        helper.assertTrue(StudioRules.isSupportedBackgroundSize(4096, 4096), "Maximum allowed image should load");
        helper.assertFalse(StudioRules.isSupportedBackgroundSize(0, 100), "Empty image should be rejected");
        helper.assertFalse(StudioRules.isSupportedBackgroundSize(4097, 100), "Oversized image should be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void itemCatalogHasRegisteredItems(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString().equals("minecraft:diamond"),
                "Item catalog must use registry IDs for search");
        helper.assertTrue(BuiltInRegistries.ITEM.stream().anyMatch(item -> item == Items.DIAMOND),
                "Registered items must be enumerable");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void itemCatalogFiltersModAndName(GameTestHelper helper) {
        helper.assertTrue(StudioRules.matchesItemQuery("Diamond Sword", "minecraft:diamond_sword", "@minecraft sword"),
                "Mod filter and item name should combine");
        helper.assertTrue(StudioRules.matchesItemQuery("Copper Gadget", "create:copper_gadget", "@crea copper"),
                "Mod filter should accept a partial namespace");
        helper.assertFalse(StudioRules.matchesItemQuery("Diamond Sword", "minecraft:diamond_sword", "@create"),
                "Other mod namespaces should not match");
        helper.assertFalse(StudioRules.matchesItemQuery("Diamond Sword", "minecraft:diamond_sword", "@minecraft axe"),
                "Text must match the item name or ID");
        helper.succeed();
    }

}
