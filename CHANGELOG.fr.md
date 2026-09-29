# Journal des versions — Nokh Frame

La [version anglaise](CHANGELOG.md) contient les mêmes changements.

## 0.5.9 — 29 septembre 2026

- Nouvelle page **Placement** : déplacer, tourner et redimensionner le joueur et la scène 3D, et décaler, zoomer, tourner ou incliner la vue, par pas de 1/16 à 1 bloc (flèches et Page préc./suiv. au clavier). La vue suit maintenant le joueur. Le clic molette glissé déplace la vue.
- Nouvel éditeur de **Pose** : dix poses toutes faites (neutre, assis, salut, T-pose, victoire, pointer, penseur, fier, course figée) et angles X/Y/Z réglables pour la tête, le corps, les bras et les jambes, avec miroir. Fonctionne en modes Studio et Monde.
- Les fichiers de scène peuvent atteindre 125 Mo (256 blocs de côté, 2 millions de blocs). Le mode Studio dessine les scènes depuis des tampons GPU construits une fois, pour rester fluide avec les grandes scènes.
- Les boutons Mouvement et Pose partagent une ligne ; les boutons Scène 3D et Placement remplacent les flèches de rotation (la rotation se trouve dans Placement, cible Vue).

## 0.5.8 — 29 septembre 2026

- Nouveau mode de rendu **Monde** : la vraie caméra du jeu tourne autour du joueur, pour que les shaderpacks Iris/Oculus s'appliquent à l'aperçu et à la photo. Pose, skin, taille et scène 3D sont appliqués dans le monde ; le HUD et le contour de bloc sont masqués.
- Taille du joueur réglable avec Ctrl + molette, de 25 % à 400 %, les pieds restant posés sur la scène.
- Le zoom descend maintenant jusqu'à 8 % (35 % avant).
- Le bouton Joueur/Item est plus court et partage la première ligne avec le choix Studio/Monde.

## 0.5.7 — 29 septembre 2026

- Scènes 3D : les structures Minecraft (`.nbt`) et les modèles Blockbench (`.bbmodel`, `.json` Java) sont rendus autour du joueur et tournent avec la caméra, devant le fond couleur ou PNG. Bibliothèque dans `config/nokhframe/scenes`, import par chemin ou glisser-déposer.
- Correction du mouvement Attaque : la main principale frappe d'un coup net puis marque une courte pause. Avant, c'était la main secondaire qui bougeait tant que le joueur n'avait pas frappé depuis sa connexion.
- Les boutons de rotation deviennent des flèches, avec le bouton **Scène 3D** entre les deux.

## 0.5.6 — 29 septembre 2026

- `/nokhframe` devient une commande serveur protégée par la permission `nokhframe.use` (LuckPerms et autres gestionnaires de permissions NeoForge). Un joueur sans cette permission n'a pas la commande en auto-complétion.
- Sans mod de permissions, seuls les opérateurs (niveau 2 ou plus) peuvent ouvrir le studio sur un serveur. Le solo et l'hôte LAN gardent toujours l'accès.
- Sur un serveur sans le mod, la commande côté client est réservée aux opérateurs.

## 0.5.5 — 29 septembre 2026

- Version minimale de NeoForge pour Minecraft 1.21.1 abaissée de 21.1.250 à 21.1.1.
- JAR compilé avec NeoForge 21.1.1 pour éviter de dépendre d'API plus récentes.
- Sept GameTests et un test client automatisé d'ouverture et de rendu du studio réussis sur 21.1.1 et 21.1.252.

## 0.5.4 — 29 septembre 2026

- Bibliothèque de fonds PNG pour les photos de joueur ou d'item, avec import par chemin ou glisser-déposer. Les images sont copiées dans `config/nokhframe/backgrounds`.
- Limites de taille pour les fonds (4096 × 4096 pixels et 16 Mo) et textes d'interface en français et en anglais.
- Pose des bras et jambes pour la marche, la course et l'accroupissement appliquée au moment du rendu du modèle joueur, après la préparation du modèle.
- Nouveau GameTest sans interface pour les dimensions des fonds. Sept GameTests réussis.

## 0.5.3 — 28 septembre 2026

- Licence propriétaire clarifiée : les JAR officiels non modifiés peuvent être redistribués dans les modpacks et launchers sous les conditions indiquées.

## 0.5.2 — 28 septembre 2026

- Commande client `/nokhframe` à la place du raccourci clavier.
- Amélioration des effets de particules natifs des items et de la simulation du mouvement dans l'aperçu.
- Ajout du code de conduite, des règles de contribution et de la politique de sécurité.

## 0.5.1 — 28 septembre 2026

- Simulation du déplacement client et de l'item tenu pour les effets déclenchés par les mods.
- Particules natives des items et ajustements de rotation et de zoom.

## 0.5.0 — 28 septembre 2026

- Aperçu de certains cosmétiques du monde, particules et pets possédés utilisant les chemins de rendu pris en charge.
- Contrôle du zoom.

## 0.4.1 — 28 septembre 2026

- Remplacement d'une intégration cosmétique propre à un mod par un rendu générique des particules.

## 0.4.0 — 28 septembre 2026

- Rotation à la souris sur trois axes, aperçu des items, recherche par `@mod` et sélection depuis l'inventaire.

## 0.3.0 — 28 septembre 2026

- Première version de Nokh Frame : photos du joueur et des items, import de skins, fonds colorés, pseudo natif et traductions française et anglaise.
