# Nokh Frame

**Français** · [English](#english)

Nokh Frame est un mod client pour Minecraft **1.21.1** et NeoForge **21.1.1 minimum** (branche 21.1.x). Il crée des aperçus photo de votre personnage avec ses cosmétiques actifs, ou de n'importe quel item enregistré par le jeu et les mods.

La version 1.0.0 est compilée avec NeoForge 21.1.1. Les 15 GameTests réussissent sur 21.1.1. Le studio (scènes, placement, poses, taille, zoom, mode Monde, Studio avec shaderpack) a été vérifié en jeu sur 21.1.1, et les modes Monde et Studio avec Iris 1.8.14 + Sodium 0.8.13 et le shaderpack Complementary Reimagined sur 21.1.252.

[Guide complet en français](docs/USER_GUIDE.fr.md) · [Journal des versions](CHANGELOG.fr.md) · [Captures du jeu](media/screenshots/README.md)

![Aperçu du joueur dans Nokh Frame](media/screenshots/01-player-studio.png)

## Utilisation

1. Installer `nokhframe-1.0.0.jar` dans le dossier `mods`, puis lancer Minecraft.
2. Entrer dans un monde et activer les cosmétiques souhaités.
3. Saisir **`/nokhframe`** dans le chat pour ouvrir le studio.
4. Choisir **Joueur** ou **Item**. Le catalogue des items permet une recherche par nom ou identifiant (`modid:item`). Saisir `@nomdumod` pour filtrer par mod, éventuellement avec un nom d'item (`@minecraft épée`). Le bouton **Depuis l'inventaire** copie l'item choisi avec ses composants.
5. Pour le joueur, sélectionner un skin dans le catalogue ou importer un PNG par glisser-déposer ou par chemin. Choisir ensuite un mouvement : repos, marche, course, accroupissement ou attaque.
6. Dans l'aperçu du joueur ou de l'item, glisser avec le bouton gauche pour tourner sur les axes horizontal et vertical. Glisser avec le bouton droit pour tourner sur le troisième axe et l'axe vertical. Utiliser la molette dans l'aperçu pour zoomer ou dézoomer.
7. Cliquer sur **Couleur** pour faire défiler les fonds prédéfinis, saisir une couleur `#RRGGBB` et cliquer sur **OK**, ou ouvrir **Image PNG** pour choisir un fond dans la bibliothèque. On peut importer le PNG par chemin ou le glisser dans cette fenêtre.
8. Cliquer sur **Prendre la photo**. Le PNG est enregistré dans le dossier `screenshots` de l'instance.

Le studio utilise le vrai personnage local, avec ses équipements et couches cosmétiques. Le pseudo au-dessus du joueur utilise le nametag natif de Minecraft. Le skin sélectionné dans le studio ne modifie pas le skin du compte visible par les autres joueurs.

La compatibilité utilise les chemins de rendu natifs de Minecraft et NeoForge : couches du joueur, modèles d'items calculés depuis la pile complète et ses composants, moteur de particules et événements de rendu du monde près du joueur. Les pets et compagnons dessinés dans ces événements ainsi que les entités vivantes proches possédées par le joueur peuvent apparaître sans dépendance envers leur mod. Un item modifié dans l'inventaire conserve ses composants lors de la sélection. Pendant les ticks client, le studio présente temporairement l'item sélectionné comme item tenu et une position de déplacement simulée aux mods qui produisent leurs effets à partir de ces données ; l'inventaire et la position réels sont conservés. La vue d'item affiche aussi les particules natives près de la main du joueur. Les rendus qui contournent ces chemins (par exemple certains shaders ou rendus propriétaires) ne sont pas garantis.

Les skins importés sont copiés dans `config/nokhframe/skins`. Seuls les PNG **64 × 64 pixels** sont acceptés. Un nom contenant `_slim` utilise les bras fins ; `_wide` utilise les bras classiques. Sans suffixe, la forme actuelle du joueur est conservée.

Les images de fond importées sont copiées dans `config/nokhframe/backgrounds`. La taille maximale est de **4096 × 4096 pixels** et **16 Mo**. L'image couvre tout le cadre de la photo ; les bords peuvent être recadrés selon ses proportions.

### Scènes 3D

Le bouton **Scène 3D** ouvre la bibliothèque de décors en relief, affichés autour du joueur et qui tournent avec la caméra. Le fond couleur ou PNG reste visible derrière. Formats acceptés :

- **Structure Minecraft `.nbt`** : construire le décor en jeu, puis l'enregistrer avec un bloc de structure (fichier dans `saves/<monde>/generated/<namespace>/structures`). Les blocs de tous les mods installés s'affichent. Le joueur est placé au centre, debout sur le premier sol de la colonne centrale ; le côté des Z croissants (sud) est derrière lui. 256 blocs de côté et 2 millions de blocs au maximum.
- **Blockbench `.bbmodel`** (textures intégrées) ou **`.json`** exporté en « Java Block/Item » : les cubes, leurs rotations et celles des groupes sont pris en charge ; les maillages libres (« mesh ») sont ignorés. 16 pixels valent un bloc et l'origine du modèle correspond aux pieds du joueur. Les textures d'un `.json` sont cherchées à côté du fichier, puis dans les packs de ressources chargés.

Ctrl + molette dans l'aperçu change la **taille du joueur** (25 % à 400 %) sans changer la scène ; la molette seule zoome de 8 % à 300 %. Le clic molette glissé déplace la vue.

### Placement

Le bouton **Placement** ouvre une page de réglages avec trois cibles (bouton **Cible**) :

- **Joueur** : position X / Y / Z en blocs (X vers la droite, Y vers le haut, Z en s'éloignant), rotation sur lui-même et taille. La vue suit le joueur : c'est la scène qui se décale. Par exemple, pour l'asseoir sur un trône, montez-le (Y) et reculez-le (Z) jusqu'au siège, puis choisissez la pose **assis**.
- **Scène** : position, rotation et échelle du décor 3D.
- **Vue** : décalage de la vue, zoom, rotation et inclinaison.

Le bouton **Pas** règle la distance de chaque clic (1/16, 1/4, 1/2 ou 1 bloc). Sur cette page, les flèches du clavier déplacent sur X et Z, et Page préc./suiv. sur Y. **Réinitialiser** remet la cible choisie à zéro.

### Pose du joueur

Le bouton **Pose** (à côté du mouvement) ouvre l'éditeur de pose, comme pour un porte-armure. Choisissez une pose toute faite (neutre, assis, salut, T-pose, victoire, pointer, penseur, fier, course figée) ou réglez chaque partie : tête, corps, bras et jambes, sur X, Y et Z (5° par clic, 1° avec Maj, 15° avec Ctrl). **Miroir** copie un bras ou une jambe sur le côté opposé. Les armures et cosmétiques qui suivent le modèle du joueur prennent la même pose. La pose **animation** rend la main au mouvement choisi.

### Mode Monde et shaderpacks

Le bouton **Studio / Monde** (en haut du panneau) choisit le rendu. Les shaderpacks Iris/Oculus ne s'appliquent jamais aux écrans d'interface ; en mode **Monde**, la vraie caméra du jeu tourne autour du joueur (mêmes rotation, zoom et taille) et le rendu du monde, shaderpack compris, devient le fond de la photo. La scène 3D est alors placée dans le monde, à vos pieds, orientée selon votre regard. Choisissez un endroit dégagé : le décor peut traverser le terrain. L'orientation est fixée à l'ouverture du mode Monde, selon votre regard.

**Mode Studio avec shaderpack** : quand un shaderpack Iris/Oculus est actif, le mode Studio passe lui aussi par le rendu du monde pour que le shader s'applique. Le terrain, le ciel, les nuages, la météo, les coffres et autres blocs animés du monde ainsi que les autres créatures (sauf vos animaux) sont masqués ; votre couleur ou votre PNG sert de toile de fond derrière la scène. L'ombre ou la lumière du shader peut légèrement modifier la teinte du fond. Sans shaderpack, le mode Studio reste dessiné comme avant. Le mode Item n'utilise pas les shaders.

Les scènes importées sont copiées dans `config/nokhframe/scenes` (125 Mo maximum). Les blocs à rendu spécial (coffres, panneaux, têtes, liquides) ne sont pas encore affichés. Les scènes ne sont pas utilisées en mode Item.

## Serveurs et permissions

En solo, et pour l'hôte d'un monde ouvert en LAN, le studio est toujours disponible, même sans cheats.

Quand le mod est installé sur le serveur, `/nokhframe` devient une commande serveur protégée par la permission **`nokhframe.use`**. Un joueur sans cette permission ne voit pas la commande en auto-complétion et ne peut pas l'exécuter.

- **Sans mod de permissions** : seuls les opérateurs (niveau 2 ou plus) y ont accès.
- **Avec LuckPerms** (ou tout gestionnaire compatible avec l'API de permissions NeoForge) : `/lp group default permission set nokhframe.use true` donne l'accès à tous ; `/lp user <joueur> permission set nokhframe.use false` le retire, même à un opérateur. Si la permission n'est pas définie, la règle des opérateurs s'applique. Vérifiez que `permissionHandler` vaut `luckperms:permission_handler` dans `config/neoforge-server.toml`.

Sur un serveur qui n'a pas le mod, il ne peut pas vérifier les permissions : la commande est alors réservée aux opérateurs, selon le niveau que le serveur envoie au client.

## Licence et communauté

Nokh Frame est publié sous [licence propriétaire, tous droits réservés](LICENSE) par NokhXyr. Les JAR officiels non modifiés peuvent être inclus et redistribués dans les modpacks et launchers, y compris publics et monétisés, aux conditions de la licence. Consultez aussi les règles de [contribution](.github/CONTRIBUTING.md), le [code de conduite](.github/CODE_OF_CONDUCT.md) et la [politique de sécurité](.github/SECURITY.md).

## Développement

Construire avec `gradlew.bat build` (Windows) ou `./gradlew build` (Linux/macOS). Le JAR se trouve dans `build/libs`.

Exécuter les GameTests NeoForge sans interface graphique avec `gradlew.bat runGameTestServer` ou `./gradlew runGameTestServer`. Aucun test JUnit n'est nécessaire. Le rendu graphique, le nametag, les cosmétiques et la capture d'image nécessitent un client Minecraft et ne sont pas couverts par le serveur GameTest.

## English

Nokh Frame is a client-side mod for Minecraft **1.21.1** and NeoForge **21.1.1 or newer** in the 21.1.x line. It makes photo previews of your character with active cosmetics, or of any item registered by Minecraft and installed mods.

Version 1.0.0 is compiled against NeoForge 21.1.1. All 15 GameTests pass on 21.1.1. The studio (scenes, placement, poses, size, zoom, World mode, Studio with a shaderpack) was checked in game on 21.1.1, and World and Studio modes with Iris 1.8.14 + Sodium 0.8.13 and the Complementary Reimagined shaderpack on 21.1.252.

[Full English guide](docs/USER_GUIDE.en.md) · [Changelog](CHANGELOG.md) · [In-game screenshots](media/screenshots/README.md)

### How to use

1. Put `nokhframe-1.0.0.jar` in the `mods` folder and start Minecraft.
2. Join a world and enable the cosmetics you want to show.
3. Enter **`/nokhframe`** in chat to open the studio.
4. Choose **Player** or **Item**. Search the item catalog by display name or registry ID (`modid:item`). Type `@modname` to filter by mod, optionally followed by an item name (`@minecraft sword`). **From inventory** copies the selected stack with its data components.
5. For player previews, choose a skin from the library or import a PNG by dragging it into the window or entering its file path. Select idle, walking, running, sneaking or attacking motion.
6. In either preview, left drag to rotate horizontally and vertically. Right drag to rotate around the third axis and vertically. Scroll the mouse wheel over the preview to zoom in or out.
7. Click **Color** to cycle preset backgrounds, enter a `#RRGGBB` color and click **Apply**, or open **PNG image** to choose from the background library. Import a PNG by entering its path or dragging it into that window.
8. Click **Take photo**. The PNG is saved in the instance's `screenshots` folder.

The studio renders your actual local player, including equipment and cosmetic render layers. The name above the player uses Minecraft's native name tag. Studio skin selection does not change your account skin or the skin seen by other players.

Compatibility uses Minecraft and NeoForge's native rendering paths: player layers, item models resolved from the complete stack and its data components, the particle engine, and world rendering events near the player. Pets and companions drawn in those events, as well as nearby living entities owned by the player, can appear without a dependency on their mod. Selecting a modified item from inventory preserves its components. During client ticks, the studio temporarily presents the selected item as the held item and simulated movement to mods that emit effects from those values; the real inventory and position are preserved. The item view also renders native particles near the player's hand. Rendering that bypasses these paths (for example some shaders or proprietary renderers) is not guaranteed.

Imported skins are copied to `config/nokhframe/skins`. Only **64 × 64 PNG** files are accepted. Filenames containing `_slim` use slim arms; `_wide` uses classic arms. Other filenames keep your current player model.

Imported background images are copied to `config/nokhframe/backgrounds`. The maximum size is **4096 × 4096 pixels** and **16 MB**. The image covers the full photo frame; its edges may be cropped to fit.

#### 3D scenes

The **3D scene** button opens the library of 3D sets, shown around the player and rotating with the camera. The color or PNG background stays visible behind. Supported formats:

- **Minecraft structure `.nbt`**: build the set in game and save it with a structure block (the file goes to `saves/<world>/generated/<namespace>/structures`). Blocks from every installed mod are shown. The player is placed at the center, standing on the first floor of the center column; the positive Z side (south) is behind them. At most 256 blocks per side and 2 million blocks.
- **Blockbench `.bbmodel`** (embedded textures) or **`.json`** exported as "Java Block/Item": cubes with element and group rotations are supported; free meshes are skipped. 16 pixels are one block and the model origin is the player's feet. `.json` textures are looked up next to the file, then in the loaded resource packs.

Ctrl + wheel over the preview changes the **player size** (25 % to 400 %) without resizing the scene; the wheel alone zooms from 8 % to 300 %. Middle-button drag pans the view.

#### Placement

The **Placement** button opens a settings page with three targets (**Target** button):

- **Player**: X / Y / Z position in blocks (X to the right, Y up, Z away from the viewer), rotation and size. The view follows the player, so the scene shifts instead. For example, to seat the player on a throne, raise them (Y) and move them back (Z) onto the seat, then pick the **sitting** pose.
- **Scene**: position, rotation and scale of the 3D set.
- **View**: view offset, zoom, rotation and tilt.

**Step** sets the distance per click (1/16, 1/4, 1/2 or 1 block). On this page the arrow keys move along X and Z, and Page Up/Down along Y. **Reset** clears the selected target.

#### Player pose

The **Pose** button (next to the motion) opens the pose editor, like an armor stand. Pick a ready-made pose (neutral, sitting, wave, T-pose, victory, point, thinker, proud, frozen run) or set each part: head, body, arms and legs, on X, Y and Z (5° per click, 1° with Shift, 15° with Ctrl). **Mirror** copies an arm or leg to the opposite side. Armor and cosmetics that follow the player model take the same pose. The **animation** pose hands the limbs back to the selected motion.

#### World mode and shaderpacks

The **Studio / World** button (top of the panel) picks the renderer. Iris/Oculus shaderpacks never apply to GUI screens; in **World** mode the real game camera orbits the player (same rotation, zoom and size) and the world render, shaderpack included, becomes the photo background. The 3D scene is then placed in the world at your feet, turned to match where you face. Pick an open spot: the set can intersect terrain. The facing is fixed when World mode starts, from where you look.

**Studio mode with a shaderpack**: while an Iris/Oculus shaderpack is active, Studio mode also goes through the world render so the shader applies. Terrain, sky, clouds, weather, the world's chests and other animated blocks, and other creatures (except your pets) are hidden; your color or PNG becomes a backdrop behind the scene. Shader lighting can slightly shift the backdrop tint. Without a shaderpack, Studio mode is drawn as before. Item mode does not use shaders.

Imported scenes are copied to `config/nokhframe/scenes` (125 MB maximum). Blocks with special renderers (chests, signs, heads, liquids) are not shown yet. Scenes are not used in Item mode.

### Servers and permissions

In singleplayer, and for the host of a LAN world, the studio is always available, even with cheats off.

When the mod is installed on the server, `/nokhframe` becomes a server command guarded by the **`nokhframe.use`** permission. Players without it do not see the command in auto-completion and cannot run it.

- **No permission mod**: only operators (level 2 or higher) have access.
- **LuckPerms** (or any handler using the NeoForge permission API): `/lp group default permission set nokhframe.use true` grants access to everyone; `/lp user <player> permission set nokhframe.use false` removes it, even from an operator. When the node is unset, the operator rule applies. Make sure `permissionHandler` is `luckperms:permission_handler` in `config/neoforge-server.toml`.

A server without the mod cannot check permissions, so there the command is limited to operators, based on the level the server sends to the client.

### License and community

Nokh Frame is published by NokhXyr under a [proprietary, all-rights-reserved license](LICENSE). Official, unmodified JARs may be included and redistributed in modpacks and launchers, including public and monetized packs, under the license terms. See the [contribution guide](.github/CONTRIBUTING.md), [Code of Conduct](.github/CODE_OF_CONDUCT.md), and [Security Policy](.github/SECURITY.md).

### Development

Build with `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS. Find the JAR in `build/libs`.

Run the headless NeoForge GameTests with `gradlew.bat runGameTestServer` or `./gradlew runGameTestServer`. No JUnit tests are used. Rendering, name tags, cosmetics and screenshots require a Minecraft client and are outside the server GameTest scope.
