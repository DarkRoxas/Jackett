# CinéPass — billets de cinéma sur Android + Google Wallet

Application Android (Kotlin / Jetpack Compose) qui archive vos billets de cinéma,
les réaffiche avec un code-barres plein écran **en luminosité maximale**, et les
enregistre comme billets électroniques dans **Google Wallet (Google Pay)**.

> Ce dossier est autonome : il n'a aucun lien avec le reste du dépôt Jackett.
> Ouvrez `android-cine-ticket/` directement dans Android Studio.

## Fonctionnalités

| | |
|---|---|
| **Archivage local** | Base Room chiffrée par le stockage privé de l'app : film, année, cinéma, date/heure, salle, sièges, référence, notes, affiche. |
| **Configuration dans l'app** | Écran de bienvenue au premier lancement puis écran de réglages : clé TMDB et paramètres Google Wallet se saisissent sur l'appareil, sans recompiler. |
| **Recherche de films** | Recherche TMDB depuis le formulaire : le titre, l'année et l'affiche sélectionnés remplissent la fiche, et l'affiche est téléchargée en local pour rester consultable hors ligne. |
| **Choix de l'affiche** | L'affiche retenue est la française la mieux notée de la galerie TMDB — celle exploitée en salle. « Autre affiche du film » ouvre la galerie complète si ce n'est pas la bonne. |
| **Haute définition** | La sélection écarte tout ce qui fait moins de 1000 px de large, affiche la résolution sous chaque vignette et enregistre le fichier en résolution d'origine. |
| **Affiches animées** | Un GIF ou WebP animé choisi dans la galerie s'anime sur la fiche et en plein écran (décodeurs Coil). TMDB ne diffusant que des images fixes, la source doit venir de vous. |
| **Cinémas proposés** | Liste déroulante alimentée par [`KnownCinemas`](app/src/main/java/fr/cinepass/ticket/data/KnownCinemas.kt) et par les salles déjà saisies ; le champ reste libre. |
| **Code-barres facultatif** | Un billet sans code s'enregistre normalement : aucun bloc n'est alors affiché, ni sur la fiche ni dans le pass Wallet. |
| **Année de sortie** | Affichée partout sous la forme « Dune (2021) », y compris dans le pass Wallet : deux films homonymes restent distinguables. |
| **Onglets À venir / Archives** | Un billet bascule automatiquement dans les archives 4 h après le début de la séance ; l'archivage manuel reste possible. |
| **Mise en page façon Wallet** | Informations de séance, code-barres compact, puis l'affiche entière — jamais rognée — sur toute la largeur. |
| **Plein écran** | Un appui sur l'affiche ou sur le code l'ouvre seul à l'écran : barres système masquées, luminosité au maximum, fond noir pour l'affiche et blanc pour le code. |
| **Code-barres** | Rendu local via ZXing : QR Code, Aztec, PDF417, Data Matrix, Code 128, Code 39, EAN-13, ITF. Toujours affiché noir sur blanc, quel que soit le thème. |
| **Luminosité maximale** | À l'ouverture d'un billet, l'écran passe au maximum et la mise en veille est bloquée, pour que le scanner de la salle lise le code même en plein jour. Réglage système inchangé, valeur restaurée en quittant l'écran. |
| **Google Wallet** | Bouton « Ajouter à Google Wallet » : génère un pass `EventTicket` (film, cinéma, date, salle, siège, code-barres) et l'enregistre via `PayClient.savePassesJwt`. |
| **Affiche du film** | Sélecteur de médias Android (aucune permission stockage requise), copiée dans le stockage interne de l'app. |

## Prérequis

- Android Studio Ladybug ou plus récent
- JDK 17
- SDK Android 35 (`compileSdk = 35`, `minSdk = 26`)

```bash
cd android-cine-ticket
./gradlew testDebugUnitTest   # tests JVM : pass Wallet, signature JWT, dates
./gradlew :app:assembleDebug  # APK -> app/build/outputs/apk/debug/
```

Le workflow `.github/workflows/android-cine-ticket.yml` exécute ces deux commandes
à chaque push touchant ce dossier et publie l'APK debug en artefact — pratique
pour récupérer un build sans installer le SDK.

## Configuration depuis l'application

Au premier lancement, un écran de bienvenue propose de renseigner les deux
intégrations facultatives ; on peut aussi passer et le faire plus tard via
l'icône engrenage de la liste des billets. Les valeurs sont conservées sur
l'appareil (DataStore, stockage privé de l'app) et relues à chaque utilisation :
aucune recompilation n'est nécessaire.

| Réglage | À quoi il sert | Où le trouver |
|---|---|---|
| Clé d'API TMDB | Recherche de films (titre, année, affiche) | <https://www.themoviedb.org/settings/api>, clé v3, gratuite |
| Identifiant émetteur | Rattache le pass à votre compte Google Wallet | <https://pay.google.com/business/console> |
| Adresse du service de signature | Signe le pass côté serveur | votre backend, voir [`backend-sample/`](backend-sample/) |
| Compte de service + clé privée | Signature sur l'appareil, pour dépanner | clé JSON du compte de service |

Tout champ laissé vide retombe sur la valeur de compilation issue de
`local.properties` (voir plus bas), ce qui laisse le mode développement intact.
Sans configuration du tout, l'app fonctionne : seuls la recherche et le bouton
Wallet affichent un message expliquant ce qui manque.

## Configuration au build (développement)

L'enregistrement d'un pass exige un **compte émetteur** Google Wallet
(<https://pay.google.com/business/console>) et un JWT `savetowallet` signé par un
compte de service RS256. Rien n'est nécessaire pour le reste de l'app : sans
configuration, tout fonctionne sauf le bouton Wallet, qui affiche un message
explicite.

Copiez `local.properties.example` en `local.properties` (déjà ignoré par git) :

```properties
sdk.dir=/chemin/vers/Android/sdk

# Identifiant émetteur de la console Google Wallet
WALLET_ISSUER_ID=3388000000012345678
WALLET_ISSUER_NAME=CinePass
WALLET_CLASS_SUFFIX=cinepass_event_class
```

Puis choisissez **une** des deux sources de JWT.

### 1. Backend signataire — recommandé (et seul mode valable en production)

```properties
WALLET_JWT_ENDPOINT=https://mon-backend.example/wallet/jwt
```

L'app envoie en POST un JSON décrivant le billet :

```json
{
  "ticketId": "…", "movieTitle": "…", "cinemaName": "…",
  "screeningAt": 1767200400000, "room": "3", "seats": "H12, H13",
  "bookingReference": "ABC123", "barcodeValue": "…",
  "barcodeFormat": "QR_CODE", "objectId": "3388000000012345678.abcdef"
}
```

Le backend construit l'`EventTicketObject`, signe le JWT avec la clé du compte de
service et répond `{"jwt": "..."}` (ou le JWT en texte brut). La clé privée ne
quitte jamais le serveur.

Une implémentation de référence prête à lancer se trouve dans
[`backend-sample/`](backend-sample/) : ~150 lignes de Node, sans dépendance npm,
produisant exactement la même charge utile que `WalletPassBuilder.kt`.

### 2. Signature sur l'appareil — développement uniquement

```properties
WALLET_SA_EMAIL=wallet@mon-projet.iam.gserviceaccount.com
WALLET_SA_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvQ…\n-----END PRIVATE KEY-----\n
```

Ces valeurs ne sont injectées que dans le build **debug** ; la variante release
les force à vide. Une clé privée embarquée dans un APK est extractible en
quelques minutes : ne publiez jamais une application configurée ainsi.

Les mêmes clés peuvent être fournies par variables d'environnement (utile en CI)
plutôt que par `local.properties`.

## Architecture

```
app/src/main/java/fr/cinepass/ticket/
├── CinePassApplication.kt      conteneur d'injection minimaliste
├── MainActivity.kt             hôte Compose + retour de PayClient (onActivityResult)
├── data/                       entité Ticket, DAO, base Room, dépôt (+ import d'affiche),
│                               recherche TMDB
├── wallet/
│   ├── WalletConfig.kt         configuration issue de BuildConfig
│   ├── WalletPassBuilder.kt    charge utile EventTicketClass / EventTicketObject
│   ├── WalletJwt.kt            signature RS256 locale (debug)
│   ├── WalletRepository.kt     disponibilité Wallet, obtention du JWT, savePassesJwt
│   └── WalletResultBus.kt      relais onActivityResult → ViewModel
├── ui/
│   ├── CinePassNavHost.kt      liste → détail → formulaire
│   ├── components/
│   │   ├── BarcodeView.kt      rendu ZXing
│   │   └── MaxBrightness.kt    override de luminosité limité à la fenêtre de l'app
│   ├── screens/                liste, détail, formulaire, recherche de films,
│   │                           vue plein écran (+ ViewModels)
│   └── theme/
└── util/DateTimeFormat.kt      formats français, conversions UTC du DatePicker
```

## Signature de debug

`keystore/debug.keystore` est versionné et utilisé par la variante debug. Sans
lui, chaque poste et chaque run de CI génèrent leur propre clé, et Android
refuse d'installer un APK par-dessus le précédent (« signatures différentes »),
ce qui oblige à désinstaller — et donc à perdre les billets enregistrés.

C'est la clé de debug standard (`androiddebugkey` / `android`) : elle ne permet
de publier nulle part et ne protège rien. Une release doit être signée avec une
clé qui, elle, n'a rien à faire dans le dépôt.

## Notes

- Aucune donnée ne sort de l'appareil, hormis l'appel au backend Wallet si vous
  en configurez un et la recherche TMDB si vous l'activez.
- La permission `INTERNET` n'est utilisée que pour ce backend.
- La classe Wallet est envoyée en ligne dans le JWT avec
  `reviewStatus: UNDER_REVIEW`, ce qui suffit aux tests. Pour une diffusion
  publique, créez la classe dans la console et passez-la en `APPROVED`.
