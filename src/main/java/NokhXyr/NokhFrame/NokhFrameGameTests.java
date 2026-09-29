package NokhXyr.NokhFrame;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
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

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sceneFilesAndLimits(GameTestHelper helper) {
        helper.assertTrue(StudioRules.isSceneFile(Path.of("Arcadia.NBT")), "Structure extension should be case insensitive");
        helper.assertTrue(StudioRules.isSceneFile(Path.of("halo.bbmodel")), "Blockbench projects are scenes");
        helper.assertTrue(StudioRules.isSceneFile(Path.of("model.json")), "Java model exports are scenes");
        helper.assertFalse(StudioRules.isSceneFile(Path.of("background.png")), "PNG files belong to the image library");
        helper.assertTrue(StudioRules.isSupportedStructureSize(48, 48, 48), "Structure block maximum size must load");
        helper.assertFalse(StudioRules.isSupportedStructureSize(StudioRules.MAX_STRUCTURE_SIDE + 1, 4, 4), "Oversized structures are rejected");
        helper.assertFalse(StudioRules.isSupportedStructureSize(0, 4, 4), "Empty dimensions are rejected");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void scenePlayerStandsOnFirstFloor(GameTestHelper helper) {
        helper.assertTrue(StudioRules.standingHeight(new boolean[]{true, true, false, false}) == 2,
                "The player stands on top of a two-layer floor");
        helper.assertTrue(StudioRules.standingHeight(new boolean[]{true, false, false, true}) == 1,
                "A roof above the player must be ignored");
        helper.assertTrue(StudioRules.standingHeight(new boolean[]{false, false, false}) == 0,
                "An empty center column puts the feet at the bottom");
        helper.assertTrue(StudioRules.standingHeight(new boolean[]{true, true}) == 2,
                "A solid column puts the feet on its top");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void zoomAndPlayerSizeLimits(GameTestHelper helper) {
        helper.assertTrue(StudioRules.clampZoom(0.01F) == StudioRules.MIN_ZOOM, "Zoom out stops at the minimum");
        helper.assertTrue(StudioRules.MIN_ZOOM <= 0.1F, "The studio must zoom out to at least 10 %");
        helper.assertTrue(StudioRules.clampZoom(10.0F) == StudioRules.MAX_ZOOM, "Zoom in stops at the maximum");
        helper.assertTrue(StudioRules.nextPlayerSize(1.0F, 1) > 1.0F, "Scrolling up enlarges the player");
        helper.assertTrue(StudioRules.nextPlayerSize(1.0F, -1) < 1.0F, "Scrolling down shrinks the player");
        helper.assertTrue(StudioRules.nextPlayerSize(1.1F, -1) == 1.0F, "Player size snaps back to exactly 100 %");
        helper.assertTrue(StudioRules.nextPlayerSize(4.0F, 5) == StudioRules.MAX_PLAYER_SIZE, "Player size has a maximum");
        helper.assertTrue(StudioRules.nextPlayerSize(0.25F, -5) == StudioRules.MIN_PLAYER_SIZE, "Player size has a minimum");
        helper.assertTrue(StudioRules.worldCameraDistance(0.5F) == 2 * StudioRules.worldCameraDistance(1.0F),
                "Zooming out moves the world camera away proportionally");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void placementStepsAndLimits(GameTestHelper helper) {
        helper.assertTrue(StudioRules.MAX_SCENE_BYTES == 125L * 1024L * 1024L, "Scene files may reach 125 MB");
        helper.assertTrue(StudioPlacement.move(0.0F, 0.25F) == 0.25F, "A step moves by its distance");
        helper.assertTrue(StudioPlacement.move(StudioPlacement.MAX_OFFSET, 1.0F) == StudioPlacement.MAX_OFFSET,
                "Offsets stop at the maximum distance");
        helper.assertTrue(StudioPlacement.rotate(180.0F, 15.0F) == -165.0F, "Rotation wraps around");
        helper.assertTrue(StudioPlacement.nextSceneScale(1.1F, -1) == 1.0F, "Scene scale snaps back to 100 %");
        helper.assertTrue(StudioPlacement.nextSceneScale(StudioPlacement.MIN_SCENE_SCALE, -1) == StudioPlacement.MIN_SCENE_SCALE,
                "Scene scale has a minimum");
        helper.assertTrue(StudioPlacement.nextStep(StudioPlacement.STEPS.length - 1) == 0, "Step sizes cycle");
        StudioPlacement placement = new StudioPlacement();
        placement.playerX = 2.0F;
        placement.sceneScale = 3.0F;
        placement.panY = 1.0F;
        placement.reset(StudioPlacement.Target.SCENE);
        helper.assertTrue(placement.sceneScale == 1.0F && placement.playerX == 2.0F && placement.panY == 1.0F,
                "Reset only affects the selected target");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void posePresetsAndEdits(GameTestHelper helper) {
        StudioPose pose = new StudioPose();
        helper.assertFalse(pose.active(), "The studio starts on the motion animation");
        pose.apply(StudioPose.Preset.SIT);
        helper.assertTrue(pose.active() && pose.angle(StudioPose.Part.RIGHT_LEG, 0) < -45.0F, "Sitting raises the legs");
        pose.adjust(StudioPose.Part.HEAD, 1, 20.0F);
        helper.assertTrue(pose.preset() == StudioPose.Preset.CUSTOM, "Editing a part makes the pose custom");
        helper.assertTrue(pose.angle(StudioPose.Part.RIGHT_LEG, 0) < -45.0F, "Custom poses keep the preset angles");
        pose.adjust(StudioPose.Part.HEAD, 0, 1000.0F);
        helper.assertTrue(pose.angle(StudioPose.Part.HEAD, 0) == StudioPose.MAX_ANGLE, "Angles are limited");
        pose.apply(StudioPose.Preset.WAVE);
        pose.mirror(StudioPose.Part.RIGHT_ARM);
        helper.assertTrue(pose.angle(StudioPose.Part.LEFT_ARM, 2) == -pose.angle(StudioPose.Part.RIGHT_ARM, 2),
                "Mirroring flips the outward rotation");
        for (StudioPose.Preset preset : StudioPose.Preset.values()) {
            pose.apply(preset);
            helper.assertTrue(pose.nextPreset() != StudioPose.Preset.CUSTOM, "Cycling never lands on custom");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void attackMotionSwingsThenRests(GameTestHelper helper) {
        helper.assertTrue(StudioRules.attackProgress(0.0F) == 0.0F, "A swing starts from rest");
        helper.assertTrue(StudioRules.attackProgress(StudioRules.ATTACK_SWING_SECONDS / 2) > 0.4F,
                "The arm must be mid-swing halfway through the hit");
        helper.assertTrue(StudioRules.attackProgress(StudioRules.ATTACK_SWING_SECONDS + 0.1F) == 0.0F,
                "The arm rests after the hit");
        helper.assertTrue(StudioRules.attackProgress(StudioRules.ATTACK_CYCLE_SECONDS + 0.1F) > 0.0F,
                "The hit repeats every cycle");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void studioRequiresOperatorByDefault(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerList players = helper.getLevel().getServer().getPlayerList();
        helper.assertFalse(StudioAccessNetwork.canUse(player), "Players without permission must not open the studio");
        helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("nokhframe") != null,
                "The server must register /nokhframe");
        // GameTestServer grants level 0 to plain ops, so add a level-2 entry like a default dedicated server op.
        players.getOps().add(new ServerOpListEntry(player.getGameProfile(), StudioAccessNetwork.DEFAULT_PERMISSION_LEVEL, false));
        try {
            helper.assertTrue(StudioAccessNetwork.canUse(player), "Operators must open the studio without a permission mod");
        } finally {
            players.getOps().remove(player.getGameProfile());
        }
        helper.succeed();
    }
}
