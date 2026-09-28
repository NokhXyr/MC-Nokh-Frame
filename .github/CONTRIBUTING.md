# Contributing to Nokh Frame / Contribuer à Nokh Frame

Nokh Frame is a client-side Minecraft 1.21.1 mod for NeoForge. Contributions
to its original code, French and English translations, and documentation are
welcome. The project is source-visible and **All rights reserved**; consult
the [license](../LICENSE) before making a fork or submitting work.

Nokh Frame est un mod client Minecraft 1.21.1 pour NeoForge. Les contributions
au code original, aux traductions françaises et anglaises et à la
documentation sont bienvenues. Le projet est visible publiquement mais reste
**tous droits réservés** ; consultez la [licence](../LICENSE).

## Prepare a contribution / Préparer une contribution

1. Open an issue describing the bug or proposed change, then create a focused
   branch or a fork solely for a pull request to this repository.
2. Use Java 21. Keep Java identifiers and comments clear. Add or update both
   `en_us.json` and `fr_fr.json` when changing visible text.
3. Preserve the mod's generic Minecraft and NeoForge compatibility. Do not
   copy code or assets from other mods without permission.
4. Run `gradlew.bat build` and `gradlew.bat runGameTestServer` on Windows, or
   `./gradlew build` and `./gradlew runGameTestServer` elsewhere. The project
   uses headless NeoForge GameTests, not JUnit.
5. Describe any visual checks separately: headless GameTests cannot verify
   rendered skins, cosmetics, particles, pets, or screenshots.
6. Send a pull request to `main` explaining the change, test results, and any
   remaining limitation. Keep one logical change per pull request.

Ouvrez une issue pour décrire le problème ou la proposition. Créez une branche
ou un fork uniquement pour soumettre une pull request à ce dépôt. Utilisez
Java 21, mettez à jour les deux langues pour tout texte visible, évitez le
code ou les ressources tiers sans autorisation, puis lancez `build` et les
GameTests NeoForge. Indiquez séparément les vérifications visuelles, car les
GameTests sans interface ne peuvent pas contrôler les images. Ciblez `main`.

## Rights and conduct / Droits et conduite

Contributions must be your own or submitted with the necessary rights. By
submitting, you grant NokhXyr the contribution license described in
[Section 4 of the license](../LICENSE). Contributions do not change the
proprietary license of Nokh Frame. Follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

Vos contributions doivent être originales ou soumises avec les autorisations
nécessaires. En les soumettant, vous accordez à NokhXyr les droits décrits à
la section 4 de la [licence](../LICENSE). Respectez le
[Code de conduite](CODE_OF_CONDUCT.md).
