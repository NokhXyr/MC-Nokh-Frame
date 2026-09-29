# CurseForge publication kit — Nokh Frame 0.5.5

Use the English text first on CurseForge. The French translation follows it. The image captions correspond to authentic captures from the isolated Minecraft test instance in `media/screenshots/`.

## Project fields

- **Name:** Nokh Frame
- **Summary:** Photograph your Minecraft character and items with skins, cosmetics, native name tags and custom backgrounds.
- **Game:** Minecraft
- **Project type:** Mod
- **Loader:** NeoForge
- **Minecraft version:** 1.21.1
- **NeoForge version:** 21.1.1 minimum (21.1.x)
- **Side:** Client
- **Author:** NokhXyr
- **License:** Custom — use the full text from [LICENSE](../LICENSE), including the modpack permission. Do not select a generic open-source license.
- **Issues and source:** `https://github.com/NokhXyr/Nokh-Frame`
- **File:** `nokhframe-0.5.5.jar`
- **Display name:** Nokh Frame 0.5.5 — Minecraft 1.21.1 / NeoForge 21.1.1+

## Description — English

**Nokh Frame** is an in-game photo studio for Minecraft characters and items. Open it with `/nokhframe`, pose your player or select an item, choose a background, and save a PNG screenshot. The mod runs on the client with Minecraft 1.21.1 and **NeoForge 21.1.1 or newer in the 21.1.x line**. Versions 21.1.1 and 21.1.252 passed automated client and GameTest checks; intermediate builds have not each been tested.

### What you can do

- Photograph your actual player with equipped gear, visible cosmetic render layers and Minecraft's native name tag.
- Preview idle, walking, running, sneaking and attacking poses.
- Keep your current skin or import a 64 × 64 PNG skin through the skin library by path or drag and drop.
- Browse registered items, search by name or ID, and type `@modname` to filter by mod. Choose an item from your inventory to preserve its data components.
- Rotate the player or item by dragging in the preview and zoom with the mouse wheel.
- Choose a preset color, enter a `#RRGGBB` color, or import a PNG image background by path or drag and drop.
- Save the photo to your Minecraft instance's `screenshots` folder.

### How to use

1. Install the JAR in your Minecraft 1.21.1 NeoForge instance's `mods` folder and join a world.
2. Equip any items and cosmetics you want to show, then type `/nokhframe` in chat.
3. Choose **Player** or **Item**. Use the skin library, item catalog or **From inventory** as needed.
4. Choose a motion and background. Left drag rotates horizontally and vertically; right drag or Shift + drag controls the third axis. Scroll to zoom.
5. Click **Take photo** and find the PNG in the instance's `screenshots` folder.

The studio uses Minecraft and NeoForge rendering paths for cosmetic layers, items, nearby owned pets and some particle and world effects. A third-party effect appears only if its mod renders through a path the studio can capture; custom shaders or proprietary renderers may behave differently. Nokh Frame has no required cosmetic-mod dependency. The selected studio skin does not change the skin other players see.

Imported skins are copied to `config/nokhframe/skins` and must be 64 × 64 PNGs. Imported image backgrounds are copied to `config/nokhframe/backgrounds` and may be up to 4096 × 4096 pixels and 16 MB. The game interface is available in English and French.

Nokh Frame is © 2026 NokhXyr, all rights reserved. Official, unmodified JARs may be included in public or monetized modpacks and launchers under the [custom license](https://github.com/NokhXyr/Nokh-Frame/blob/main/LICENSE). Report bugs through [GitHub issues](https://github.com/NokhXyr/Nokh-Frame/issues).

## Description — Français

**Nokh Frame** est un studio photo intégré à Minecraft pour les personnages et les items. Ouvrez-le avec `/nokhframe`, préparez votre personnage ou choisissez un item, sélectionnez un fond et enregistrez une capture PNG. Le mod est côté client pour Minecraft 1.21.1 avec **NeoForge 21.1.1 minimum (branche 21.1.x)**. Les versions 21.1.1 et 21.1.252 ont passé les tests automatisés client et GameTest ; les versions intermédiaires n'ont pas été vérifiées individuellement.

### Fonctionnalités

- Photographiez votre vrai personnage avec son équipement, les couches cosmétiques visibles et son pseudo natif Minecraft.
- Choisissez une pose : repos, marche, course, accroupissement ou attaque.
- Gardez votre skin ou importez un PNG 64 × 64 dans la bibliothèque par chemin ou glisser-déposer.
- Parcourez les items, cherchez par nom, identifiant ou `@nomdumod`, ou prenez un item de votre inventaire en conservant ses composants.
- Tournez le personnage ou l'item en glissant dans l'aperçu et utilisez la molette pour zoomer.
- Choisissez une couleur prédéfinie, appliquez une couleur `#RRGGBB` ou importez une image PNG comme fond.
- Enregistrez la photo dans le dossier `screenshots` de l'instance Minecraft.

### Comment l'utiliser

1. Placez le JAR dans le dossier `mods` d'une instance Minecraft 1.21.1 avec NeoForge et entrez dans un monde.
2. Équipez les objets et cosmétiques souhaités, puis saisissez `/nokhframe` dans le chat.
3. Choisissez **Joueur** ou **Item**. Utilisez la bibliothèque de skins, le catalogue d'items ou **Depuis l'inventaire**.
4. Choisissez le mouvement et le fond. Glissez avec le bouton gauche pour tourner sur deux axes, avec le bouton droit ou Maj pour le troisième axe, et utilisez la molette pour zoomer.
5. Cliquez sur **Prendre la photo**. Le PNG se trouve dans `screenshots` dans le dossier de l'instance.

Le studio utilise les chemins de rendu de Minecraft et NeoForge pour les couches cosmétiques, items, pets proches appartenant au joueur et certains effets de particules et de monde. L'affichage d'un effet dépend de la façon dont son mod le rend ; les shaders ou rendus particuliers peuvent se comporter différemment. Aucun mod cosmétique n'est obligatoire. Le skin choisi dans le studio ne change pas celui que voient les autres joueurs.

Les skins importés sont copiés dans `config/nokhframe/skins` et doivent être des PNG 64 × 64. Les fonds PNG importés sont copiés dans `config/nokhframe/backgrounds` ; limite de 4096 × 4096 pixels et 16 Mo. L'interface est disponible en français et en anglais.

Nokh Frame est © 2026 NokhXyr, tous droits réservés. Les JAR officiels non modifiés peuvent être inclus dans les modpacks et launchers publics ou monétisés aux conditions de la [licence personnalisée](https://github.com/NokhXyr/Nokh-Frame/blob/main/LICENSE). Signalez les bugs sur [GitHub](https://github.com/NokhXyr/Nokh-Frame/issues).

## File changelog — English

**0.5.5**

- Lowered the Minecraft 1.21.1 NeoForge minimum from 21.1.250 to 21.1.1.
- Built against NeoForge 21.1.1; seven GameTests and an automated studio opening and rendering check passed on both 21.1.1 and 21.1.252.

## File changelog — Français

**0.5.5**

- Version minimale de NeoForge pour Minecraft 1.21.1 abaissée de 21.1.250 à 21.1.1.
- JAR compilé avec NeoForge 21.1.1 ; sept GameTests et le test automatisé d'ouverture et de rendu du studio réussis sur 21.1.1 et 21.1.252.

## Gallery captions

1. **01-player-studio.png** — The real player, native name tag, color background and photo controls.
2. **02-skin-library.png** — The library with two imported sample skins.
3. **03-item-studio.png** — A Netherite sword rendered in the item preview.
4. **04-item-catalog.png** — Search the registered item catalog using `@minecraft sword`.
5. **05-png-background.png** — The player preview with an imported PNG background.
6. **06-background-library.png** — Choose an imported PNG from the background library.
7. **08-clean-photo.png** — A photo saved without the studio controls.

The screenshots show the 0.5.4 client running in a dedicated test world. The studio interface is unchanged in 0.5.5. The sample PNG background was made for the demonstration; the gallery images themselves are unedited in-game captures. This isolated instance contained vanilla gear; the images do not demonstrate third-party cosmetic effects.
