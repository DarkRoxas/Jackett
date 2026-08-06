# Signataire JWT Google Wallet — exemple

Petit serveur Node (aucune dépendance npm, Node ≥ 18) qui signe les pass demandés
par l'app. C'est le mode à utiliser dès que l'app quitte le poste de développement :
la clé privée du compte de service reste sur le serveur.

## Mise en place

1. Dans la [console Google Wallet](https://pay.google.com/business/console), récupérez
   votre **identifiant émetteur** et créez un **compte de service** avec le rôle
   *Wallet Object Issuer*, puis téléchargez sa clé JSON.
2. Placez le fichier ici sous le nom `wallet-service-account.json` (déjà ignoré par git).
3. Lancez le serveur :

```bash
WALLET_ISSUER_ID=3388000000012345678 node server.js
```

Variables reconnues : `PORT` (8080), `WALLET_ISSUER_ID` (obligatoire),
`WALLET_CLASS_SUFFIX`, `WALLET_ISSUER_NAME`, `GOOGLE_APPLICATION_CREDENTIALS`.

## Côté application

Dans `android-cine-ticket/local.properties` :

```properties
WALLET_JWT_ENDPOINT=http://10.0.2.2:8080/wallet/jwt
```

`10.0.2.2` est l'adresse de la machine hôte vue depuis l'émulateur Android. Sur un
téléphone physique, utilisez l'IP de votre PC sur le réseau local.

> En HTTP clair, le trafic n'est autorisé que si vous ajoutez une configuration
> `networkSecurityConfig` de debug. Pour un déploiement réel, servez l'endpoint en HTTPS.

## Contrat

`POST /wallet/jwt` reçoit le billet et répond `{"jwt": "..."}` :

```bash
curl -X POST http://localhost:8080/wallet/jwt \
  -H 'Content-Type: application/json' \
  -d '{
        "ticketId": "3f1c6d0e-1234-4a5b-8c9d-0123456789ab",
        "movieTitle": "Dune : Deuxième partie",
        "cinemaName": "UGC Ciné Cité Les Halles",
        "screeningAt": 1773500400000,
        "room": "12", "seats": "H12, H13",
        "bookingReference": "REF-42",
        "barcodeValue": "TICKET-0001",
        "barcodeFormat": "QR_CODE"
      }'
```

La charge utile produite est identique à celle de `WalletPassBuilder.kt`, afin que
les deux modes (backend / signature locale de debug) donnent le même pass.

Ce serveur est un exemple : ajoutez-y l'authentification de vos utilisateurs avant
tout usage réel, sinon n'importe qui peut faire signer un pass à votre émetteur.
