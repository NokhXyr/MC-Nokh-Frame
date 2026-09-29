# Changelog / Journal des versions

The version shown here is the mod version. Minecraft and NeoForge versions are listed in the [user guide](docs/USER_GUIDE.en.md).

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
