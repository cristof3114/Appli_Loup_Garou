# Appli Loup-Garou

Application Android qui remplace le maître du jeu des *Loups-Garous de Thiercelieux* :
un seul appareil posé au centre de la table appelle les rôles la nuit, applique les pouvoirs,
annonce les morts et déclare les vainqueurs.

## Organisation

- `engine/` : le moteur de règles, en Kotlin pur, sans dépendance Android. Il est couvert par des
  tests (`./gradlew :engine:test`).
- `app/` : l'application Android (Jetpack Compose), qui affiche les étapes du moteur.

## Récupérer l'APK

Le plus simple : sur le téléphone, ouvrir la page *Releases* du dépôt (publication « Dernière version ») et télécharger `loup-garou.apk`. Elle est mise à jour à chaque envoi sur `main`.

Chaque envoi de code lance aussi l'action GitHub « Tests et APK ». L'APK se trouve également dans l'onglet
*Actions*, en bas de la page de la dernière exécution, sous *Artifacts* (`loup-garou-apk`).

## Construire en local

Ouvrir le dossier dans Android Studio, ou lancer `./gradlew :app:assembleDebug` avec le SDK Android
installé. Sans SDK, seul le module `engine` est inclus dans le build.
