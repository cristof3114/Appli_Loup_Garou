# Appli Loup-Garou

Application Android qui remplace le maître du jeu des *Loups-Garous de Thiercelieux* :
un seul appareil posé au centre de la table appelle les rôles la nuit, applique les pouvoirs,
annonce les morts et déclare les vainqueurs.

## Organisation

- `engine/` : le moteur de règles, en Kotlin pur, sans dépendance Android. Il est couvert par des
  tests (`./gradlew :engine:test`).
- `app/` : l'application Android (Jetpack Compose), qui affiche les étapes du moteur.

## Récupérer l'APK

Chaque envoi de code lance l'action GitHub « Tests et APK ». L'APK se télécharge dans l'onglet
*Actions*, en bas de la page de la dernière exécution, sous *Artifacts* (`loup-garou-apk`).

## Construire en local

Ouvrir le dossier dans Android Studio, ou lancer `./gradlew :app:assembleDebug` avec le SDK Android
installé. Sans SDK, seul le module `engine` est inclus dans le build.
