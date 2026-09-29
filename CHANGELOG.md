# Changelog / Journal des versions

The version shown here is the mod version. Minecraft and NeoForge versions are listed in the [user guide](docs/USER_GUIDE.en.md).

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
