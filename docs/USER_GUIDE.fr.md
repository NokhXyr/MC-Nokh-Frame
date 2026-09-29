# Guide d'utilisation — Nokh Frame 0.5.10

## Installer et ouvrir

Nokh Frame est un mod **client** pour **Minecraft 1.21.1**. La version 0.5.10 exige **NeoForge 21.1.1 minimum** (branche 21.1.x) ; les versions antérieures ne chargent pas le mod. Le JAR est compilé avec NeoForge 21.1.1. Placez `nokhframe-0.5.10.jar` dans le dossier `mods` de l'instance Minecraft, puis démarrez le jeu. Aucun mod cosmétique n'est obligatoire pour ouvrir le studio. Installé aussi sur un serveur, il réserve `/nokhframe` aux opérateurs ou à la permission `nokhframe.use` (LuckPerms) ; voir le README.

Les tests client (ouverture et rendu du studio) réussissaient sur NeoForge 21.1.1 et 21.1.252 pour la 0.5.5. Pour la 0.5.10, les 15 GameTests réussissent sur 21.1.1 et le studio a été vérifié en jeu sur 21.1.1 (mode Monde avec Iris sur 21.1.252). Les versions intermédiaires sont couvertes par la plage déclarée, mais n'ont pas été testées individuellement.

Entrez dans un monde, puis saisissez `/nokhframe` dans le chat. La commande ouvre le studio sur le personnage local. Il n'y a pas de raccourci clavier.

## Faire une photo du personnage

1. Équipez dans le jeu les objets, cosmétiques et compagnons à montrer.
2. Ouvrez le studio avec `/nokhframe` et laissez le mode **Joueur** sélectionné.
3. Gardez votre skin actuel ou ouvrez la bibliothèque de skins. Importez un PNG par glisser-déposer dans la fenêtre ou collez son chemin et cliquez sur **Importer ce PNG**.
4. Choisissez le mouvement : repos, marche, course, accroupissement ou attaque.
5. Faites glisser la souris dans l'aperçu pour tourner le personnage. Glissement gauche : axes horizontal et vertical. Glissement droit ou Maj + glissement : troisième axe et axe vertical. La molette règle le zoom.
6. Choisissez un fond avec **Couleur**, appliquez `#RRGGBB` avec **OK**, ou cliquez sur **Image PNG** pour importer et sélectionner un fond dans la bibliothèque.
   **Scène 3D** ajoute un décor en relief devant ce fond : une structure `.nbt` enregistrée avec un bloc de structure, ou un modèle Blockbench `.bbmodel` / `.json` Java. Voir le README pour le placement.
7. Cliquez sur **Prendre la photo**. La capture est enregistrée dans le dossier `screenshots` de l'instance.

Le pseudo au-dessus du personnage est celui du jeu, affiché avec le nametag natif de Minecraft. Un skin choisi dans le studio ne modifie pas le skin de votre compte pour les autres joueurs.

## Faire une photo d'item

Passez en mode **Item**, puis ouvrez le catalogue. Cherchez par nom, identifiant (`minecraft:diamond_sword`) ou filtre de mod (`@minecraft`). Le bouton **Depuis l'inventaire** copie la pile choisie avec ses composants : utilisez-le pour photographier un item personnalisé déjà présent dans votre inventaire. Tournez et zoomez l'item avec la souris comme pour le personnage, choisissez un fond, puis prenez la photo.

## Fichiers importés

| Fichier | Format | Dossier de copie | Limite |
| --- | --- | --- | --- |
| Skin | PNG | `config/nokhframe/skins` | 64 × 64 pixels |
| Fond plat | PNG | `config/nokhframe/backgrounds` | 4096 × 4096 pixels et 16 Mo |

Pour les skins, `_slim` dans le nom du fichier sélectionne les bras fins ; `_wide` les bras classiques. Sans suffixe, le modèle de votre joueur est conservé. Une image de fond est agrandie pour couvrir la photo ; ses bords peuvent être recadrés.

## Compatibilité et limites

Le studio rend le vrai joueur local et emprunte les chemins de rendu de Minecraft et NeoForge pour les couches cosmétiques, les items et certains effets de particules et de monde. L'apparition d'un effet dépend de la façon dont le mod tiers le dessine ; les effets propres à des rendus ou shaders particuliers peuvent ne pas apparaître. Les compagnons sont visibles quand leur mod les dessine dans ces chemins ou qu'il s'agit d'entités proches possédées par le joueur. Le studio ne change pas l'inventaire ni la position réelle du joueur.

Les mouvements et les effets visuels doivent être vérifiés dans le client Minecraft. Les GameTests sans interface vérifient les règles du mod, mais ne valident pas l'aspect d'une capture.

## Aide

- **Le studio ne s'ouvre pas :** entrez d'abord dans un monde puis saisissez exactement `/nokhframe`.
- **Le skin est refusé :** vérifiez qu'il s'agit d'un PNG de 64 × 64 pixels.
- **Le fond est refusé :** vérifiez le format PNG, la limite de 16 Mo et les dimensions maximales.
- **Un effet cosmétique manque :** vérifiez qu'il est actif sur le joueur ou l'item réel et que le mod tiers utilise un chemin de rendu pris en charge.
- **La capture est introuvable :** ouvrez `screenshots` dans le dossier de l'instance utilisée, pas dans le dossier du mod.

Nokh Frame est publié par NokhXyr sous une [licence propriétaire](../LICENSE). Les JAR officiels non modifiés peuvent être inclus dans les modpacks et launchers aux conditions de cette licence. Pour signaler un bug, utilisez les [issues GitHub](https://github.com/NokhXyr/Nokh-Frame/issues).
