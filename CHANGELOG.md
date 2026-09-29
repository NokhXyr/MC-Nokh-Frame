# Changelog / Journal des versions

The version shown here is the mod version. Minecraft and NeoForge versions are listed in the [user guide](docs/USER_GUIDE.en.md).

## 1.0.0 — 29 September 2026

First stable release. It brings together everything built from 0.3.0 to 0.5.10; the detailed history follows below.

### Photo studio

- Open the studio with `/nokhframe` and photograph your player or any registered item. Photos are saved to the instance's `screenshots` folder without the side panel.
- Player previews show the real local player: equipment, cosmetic render layers, the native name tag, owned pets, particles and world-rendered effects from mods that use the standard rendering paths.
- Item previews in 3D, with a catalog searchable by name, registry ID or `@mod`, and selection from the inventory with the item's data components.
- Skin library (`config/nokhframe/skins`, 64 × 64 PNG, `_slim` / `_wide` suffixes) with import by path or drag and drop. The studio skin never changes your account skin.
- Backgrounds: preset colors, any `#RRGGBB` color, or a PNG library (`config/nokhframe/backgrounds`, up to 4096 × 4096 pixels and 16 MB).
- Camera: three-axis mouse rotation, zoom from 8 % to 300 %, middle-button drag to pan the view.

### Motion and pose

- Motions: idle, walk, run, sneak and attack (a quick main-hand hit followed by a short rest).
- Pose editor, like an armor stand: ten ready-made poses (neutral, sitting, wave, T-pose, victory, point, thinker, proud, frozen run) and X/Y/Z angles for the head, body, arms and legs, with mirroring. Armor and cosmetics that follow the player model take the same pose.

### 3D scenes and placement

- 3D sets around the player: Minecraft structures (`.nbt`, saved with a structure block, blocks from every installed mod) and Blockbench models (`.bbmodel` with embedded textures, or Java `.json`). Library in `config/nokhframe/scenes`, files up to 125 MB (256 blocks per side, 2 million blocks). Studio mode draws scenes from GPU buffers built once.
- Placement page: move, turn and resize the player and the scene, and pan, zoom, turn or tilt the view, by steps of 1/16 to 1 block, with keyboard shortcuts. The view follows the player.
- Player size from 25 % to 400 % (Ctrl + mouse wheel), feet kept on the scene.

### Shaderpacks

- **World** mode: the real game camera orbits the player, so Iris/Oculus shaderpacks render the preview and the photo; the pose, skin, size and scene are applied in the world.
- **Studio** mode with an active shaderpack also goes through the world render: terrain, sky, weather and other creatures are hidden and the color or PNG background becomes a backdrop.

### Servers

- On a server with the mod, `/nokhframe` is guarded by the `nokhframe.use` permission (LuckPerms and other NeoForge permission handlers) and hidden from auto-completion without it. Without a permission mod, operators (level 2+) have access; singleplayer and the LAN host always do.
- On servers without the mod, the client command is limited to operators.

### Compatibility

- Minecraft 1.21.1 with NeoForge 21.1.1 or newer (21.1.x). French and English interface.
- Checked in game on NeoForge 21.1.1, and with Iris 1.8.14 + Sodium 0.8.13 and the Complementary Reimagined shaderpack on 21.1.252. 15 headless GameTests pass.

## 0.5.10 — 29 September 2026

- Shaderpacks now apply to the **Studio** preview: while an Iris/Oculus shaderpack is active, Studio mode renders through the world pipeline with the terrain, sky, clouds, weather, world block entities and other creatures hidden, and the color or PNG background drawn as a backdrop.
- Fixed the head twitching and the view drifting in World mode: the facing is now fixed when the mode starts instead of following the body rotation, which Minecraft keeps easing toward the head.

## 0.5.9 — 29 September 2026

- Added a **Placement** page: move, turn and resize the player and the 3D scene, and pan, zoom, turn or tilt the view, by steps of 1/16 to 1 block (arrow and Page keys supported). The view now follows the player. Middle-button drag pans the view.
- Added a **Pose** editor: ten ready-made poses (neutral, sitting, wave, T-pose, victory, point, thinker, proud, frozen run) and per-part X/Y/Z angles for the head, body, arms and legs, with mirroring. Works in Studio and World modes.
- Scene files can now reach 125 MB (256 blocks per side, 2 million blocks). Studio mode draws scenes from GPU buffers built once, so large scenes stay smooth.
- The motion and Pose buttons share one row; the 3D scene and Placement buttons replace the rotation arrows (rotation is on the Placement page, View target).

## 0.5.8 — 29 September 2026

- Added the **World** render mode: the real game camera orbits the player so Iris/Oculus shaderpacks render the preview and the photo. The pose, skin, size and 3D scene are applied in the world; the HUD and block outline are hidden.
- Added player size: Ctrl + mouse wheel, 25 % to 400 %, with the feet kept on the scene.
- The zoom now goes out to 8 % (was 35 %).
- The Player/Item button is now shorter and shares the top row with the Studio/World toggle.

## 0.5.7 — 29 September 2026

- Added 3D scenes: Minecraft structures (`.nbt`) and Blockbench models (`.bbmodel`, Java `.json`) are rendered around the player and rotate with the camera, in front of the color or PNG background. Library in `config/nokhframe/scenes`, import by path or drag and drop.
- Fixed the Attack motion: the main hand now swings with a quick hit followed by a short rest. Before, the off hand moved while the player had not swung since joining.
- The rotation buttons are now arrows, with the **3D scene** button between them.

## 0.5.6 — 29 September 2026

- `/nokhframe` is now a server command guarded by the `nokhframe.use` permission node (LuckPerms and other NeoForge permission handlers). Players without it do not get the command in auto-completion.
- Without a permission mod, only operators (level 2+) can open the studio on a server. Singleplayer and the LAN host always keep access.
- On servers without the mod, the client-side command is limited to operators.

## 0.5.5 — 29 September 2026

- Lowered the required NeoForge version for Minecraft 1.21.1 from 21.1.250 to 21.1.1.
- Compiled the release against NeoForge 21.1.1 to avoid linking to newer API methods.
- Seven GameTests and an automated client studio opening and rendering check passed on 21.1.1 and 21.1.252.

## 0.5.4 — 29 September 2026

- Added a PNG background library with file path import and drag and drop. Images are copied to `config/nokhframe/backgrounds` and can be used for player or item photos.
- Added image size limits (4096 × 4096 pixels, 16 MB) and French/English interface text.
- Moved the Walk, Run and Sneak limb pose to the player's model render step so animation is applied after model setup.
- Added a headless GameTest for background image dimensions. Seven GameTests pass.

## 0.5.3 — 28 September 2026

- Clarified the proprietary license: official, unmodified JARs may be redistributed in modpacks and launchers under the stated conditions.

## 0.5.2 — 28 September 2026

- Added the `/nokhframe` client command and removed the keybind.
- Improved simulated movement and native item particle effects in previews.
- Added the community, contribution and security policies.

## 0.5.1 — 28 September 2026

- Added simulated client movement and held items to help mods display movement and item effects in the studio.
- Added native item particle rendering and rotation/zoom adjustments.

## 0.5.0 — 28 September 2026

- Added world-rendered cosmetics, particles and owned pets to the player preview where their mods use the supported rendering paths.
- Added zoom controls.

## 0.4.1 — 28 September 2026

- Replaced a mod-specific cosmetic bridge with generic particle rendering.

## 0.4.0 — 28 September 2026

- Added three-axis mouse rotation, 3D item previews, item search by `@mod`, and item selection from the inventory.

## 0.3.0 — 28 September 2026

- Initial Nokh Frame release with player and item photo previews, skin import, color backgrounds, native name tags and French/English localization.
