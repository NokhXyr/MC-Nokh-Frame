# Nokh Frame

**Français** · [English](#english)

Nokh Frame est un mod client pour Minecraft **1.21.1** et NeoForge **21.1.250**. Il crée des aperçus photo de votre personnage avec ses cosmétiques actifs, ou de n'importe quel item enregistré par le jeu et les mods.

## Utilisation

1. Installer `nokhframe-0.4.0.jar` dans le dossier `mods`, puis lancer Minecraft.
2. Entrer dans un monde et activer les cosmétiques souhaités.
3. Appuyer sur **F8** pour ouvrir le studio. La touche se modifie dans les contrôles du jeu.
4. Choisir **Joueur** ou **Item**. Le catalogue des items permet une recherche par nom ou identifiant (`modid:item`). Saisir `@nomdumod` pour filtrer par mod, éventuellement avec un nom d'item (`@minecraft épée`). Le bouton **Depuis l'inventaire** copie l'item choisi avec ses composants.
5. Pour le joueur, sélectionner un skin dans le catalogue ou importer un PNG par glisser-déposer ou par chemin. Choisir ensuite un mouvement : repos, marche, course, accroupissement ou attaque.
6. Dans l'aperçu du joueur ou de l'item, glisser avec le bouton gauche pour tourner sur les axes horizontal et vertical. Glisser avec le bouton droit pour tourner sur le troisième axe et l'axe vertical.
7. Choisir un fond prédéfini ou saisir une couleur `#RRGGBB` et cliquer sur **OK**.
8. Cliquer sur **Prendre la photo**. Le PNG est enregistré dans le dossier `screenshots` de l'instance.

Le studio utilise le vrai personnage local, avec ses équipements et couches cosmétiques. Le pseudo au-dessus du joueur utilise le nametag natif de Minecraft. Le skin sélectionné dans le studio ne modifie pas le skin du compte visible par les autres joueurs.

Si `arcadia_prestige` est installé, le studio lit son effet actif et intègre les compagnons en blocs et les particules visibles autour du joueur. Cette intégration est facultative et liée à l'API interne d'Arcadia Prestige 1.3.x.

Les skins importés sont copiés dans `config/nokhframe/skins`. Seuls les PNG **64 × 64 pixels** sont acceptés. Un nom contenant `_slim` utilise les bras fins ; `_wide` utilise les bras classiques. Sans suffixe, la forme actuelle du joueur est conservée.

## Développement

Construire avec `gradlew.bat build` (Windows) ou `./gradlew build` (Linux/macOS). Le JAR se trouve dans `build/libs`.

Exécuter les GameTests NeoForge sans interface graphique avec `gradlew.bat runGameTestServer` ou `./gradlew runGameTestServer`. Aucun test JUnit n'est nécessaire. Le rendu graphique, le nametag, l'intégration avec Arcadia Prestige et la capture d'image nécessitent un client Minecraft et ne sont pas couverts par le serveur GameTest.

## English

Nokh Frame is a client-side mod for Minecraft **1.21.1** and NeoForge **21.1.250**. It makes photo previews of your character with active cosmetics, or of any item registered by Minecraft and installed mods.

### How to use

1. Put `nokhframe-0.4.0.jar` in the `mods` folder and start Minecraft.
2. Join a world and enable the cosmetics you want to show.
3. Press **F8** to open the studio. You can rebind this key in Minecraft controls.
4. Choose **Player** or **Item**. Search the item catalog by display name or registry ID (`modid:item`). Type `@modname` to filter by mod, optionally followed by an item name (`@minecraft sword`). **From inventory** copies the selected stack with its data components.
5. For player previews, choose a skin from the library or import a PNG by dragging it into the window or entering its file path. Select idle, walking, running, sneaking or attacking motion.
6. In either preview, left drag to rotate horizontally and vertically. Right drag to rotate around the third axis and vertically.
7. Choose a preset background or enter a `#RRGGBB` color and click **Apply**.
8. Click **Take photo**. The PNG is saved in the instance's `screenshots` folder.

The studio renders your actual local player, including equipment and cosmetic render layers. The name above the player uses Minecraft's native name tag. Studio skin selection does not change your account skin or the skin seen by other players.

When `arcadia_prestige` is installed, the studio reads its active effect and includes block companions and nearby particles. This optional integration uses Arcadia Prestige 1.3.x internal APIs.

Imported skins are copied to `config/nokhframe/skins`. Only **64 × 64 PNG** files are accepted. Filenames containing `_slim` use slim arms; `_wide` uses classic arms. Other filenames keep your current player model.

### Development

Build with `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS. Find the JAR in `build/libs`.

Run the headless NeoForge GameTests with `gradlew.bat runGameTestServer` or `./gradlew runGameTestServer`. No JUnit tests are used. Rendering, name tags, Arcadia Prestige integration and screenshots require a Minecraft client and are outside the server GameTest scope.
