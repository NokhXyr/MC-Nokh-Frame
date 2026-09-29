# User guide — Nokh Frame 1.0.0

## Install and open

Nokh Frame is a **client-side** mod for **Minecraft 1.21.1**. Version 1.0.0 requires **NeoForge 21.1.1 or newer** in the 21.1.x line; older versions cannot load it. The JAR is compiled against NeoForge 21.1.1. Place `nokhframe-1.0.0.jar` in your instance's `mods` folder, then start the game. Cosmetic mods are optional. Installing it on a server as well restricts `/nokhframe` to operators or to the `nokhframe.use` permission (LuckPerms); see the README.

Client checks (opening and rendering the studio) passed on NeoForge 21.1.1 and 21.1.252 for 0.5.5. For 1.0.0, all 15 GameTests pass on 21.1.1 and the studio was checked in game on 21.1.1 (World mode with Iris on 21.1.252). Intermediate builds satisfy the declared range but have not each been tested.

Join a world and type `/nokhframe` in chat. The client command opens the studio for your local player. There is no keybind.

## Photograph a player

1. Equip the items, cosmetics and companions that you want to show in game.
2. Open the studio with `/nokhframe` and keep **Player** mode selected.
3. Keep your current skin or open the skin library. Drop a PNG into the library window, or paste its file path and click **Import PNG**.
4. Choose idle, walk, run, sneak or attack motion.
5. Drag in the preview to rotate the character. Left drag rotates horizontally and vertically. Right drag or Shift + drag controls the third axis and vertical axis. Scroll to zoom.
6. Click **Color** to cycle presets, enter `#RRGGBB` and click **Apply**, or use **PNG image** to import and select an image background.
   **3D scene** adds a 3D set in front of that background: a `.nbt` structure saved with a structure block, or a Blockbench `.bbmodel` / Java `.json` model. See the README for placement rules.
7. Click **Take photo**. The capture is saved in the instance's `screenshots` folder.

The name above the character is Minecraft's native name tag. A skin selected in the studio does not change your account skin for other players.

## Photograph an item

Switch to **Item** mode and open the catalog. Search by name, registry ID (`minecraft:diamond_sword`) or mod filter (`@minecraft`). **From inventory** copies the selected stack with its data components, which is useful for customized items. Rotate and zoom the item with the mouse, choose a background, and take the photo.

## Imported files

| File | Format | Copy destination | Limit |
| --- | --- | --- | --- |
| Skin | PNG | `config/nokhframe/skins` | 64 × 64 pixels |
| Flat background | PNG | `config/nokhframe/backgrounds` | 4096 × 4096 pixels and 16 MB |

For skins, `_slim` in the filename chooses slim arms and `_wide` chooses classic arms. Without a suffix, the player's current model is kept. A background image covers the whole photo, so its edges may be cropped.

## Compatibility and limits

The studio renders your real local player through Minecraft and NeoForge's rendering paths for cosmetic layers, items, and some world and particle effects. Whether a third-party effect appears depends on how that mod draws it. Effects that use custom renderers or shaders may not appear. Companions can appear when their mods draw them through these paths or when they are nearby owned living entities. The studio does not change your real inventory or position.

Motion and visual effects need visual verification in a Minecraft client. Headless GameTests check mod rules but do not validate screenshot appearance.

## Help

- **Studio does not open:** join a world first, then enter `/nokhframe` exactly.
- **Skin is rejected:** use a 64 × 64 PNG.
- **Background is rejected:** check PNG format, the 16 MB limit and maximum dimensions.
- **A cosmetic effect is missing:** check that it is active on the actual player or item and that its mod uses a supported rendering path.
- **Cannot find a capture:** open the `screenshots` folder of the game instance you used.

Nokh Frame is published by NokhXyr under a [proprietary license](../LICENSE). Official, unmodified JARs may be included in modpacks and launchers under its terms. Report bugs through [GitHub issues](https://github.com/NokhXyr/Nokh-Frame/issues).
