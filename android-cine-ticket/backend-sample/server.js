#!/usr/bin/env node
/**
 * Backend d'exemple pour CinéPass : signe le JWT « Save to Google Wallet ».
 *
 * C'est le mode recommandé : la clé privée du compte de service reste ici et ne
 * se retrouve jamais dans l'APK. Aucune dépendance npm — uniquement Node ≥ 18.
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=./wallet-service-account.json \
 *   WALLET_ISSUER_ID=3388000000012345678 \
 *   node server.js
 *
 * Puis dans android-cine-ticket/local.properties :
 *   WALLET_JWT_ENDPOINT=http://10.0.2.2:8080/wallet/jwt   (10.0.2.2 = hôte vu de l'émulateur)
 */

const http = require('node:http');
const crypto = require('node:crypto');
const fs = require('node:fs');

const PORT = Number(process.env.PORT || 8080);
const ISSUER_ID = process.env.WALLET_ISSUER_ID;
const CLASS_SUFFIX = process.env.WALLET_CLASS_SUFFIX || 'cinepass_event_class';
const ISSUER_NAME = process.env.WALLET_ISSUER_NAME || 'CinePass';
const CREDENTIALS_PATH =
  process.env.GOOGLE_APPLICATION_CREDENTIALS || './wallet-service-account.json';

if (!ISSUER_ID) {
  console.error('WALLET_ISSUER_ID est obligatoire.');
  process.exit(1);
}

const credentials = JSON.parse(fs.readFileSync(CREDENTIALS_PATH, 'utf8'));

const base64url = (input) =>
  Buffer.from(input).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

const localized = (value, language = 'fr') => ({
  defaultValue: { language, value },
});

/** Reproduit côté serveur la charge utile construite par WalletPassBuilder.kt. */
function buildPayload(ticket) {
  const classId = `${ISSUER_ID}.${CLASS_SUFFIX}`;
  // Date locale ISO sans fuseau, comme attendu par l'API Wallet.
  const start = new Date(ticket.screeningAt).toISOString().replace(/\.\d{3}Z$/, '');

  const eventTicketClass = {
    id: classId,
    issuerName: ISSUER_NAME,
    reviewStatus: 'UNDER_REVIEW',
    // displayTitle porte l'année quand l'app la connaît : « Dune (2021) ».
    eventName: localized(ticket.displayTitle || ticket.movieTitle),
    venue: { name: localized(ticket.cinemaName), address: localized(ticket.cinemaName) },
    dateTime: { start },
    hexBackgroundColor: '#1b1033',
  };

  const eventTicketObject = {
    id: ticket.objectId || `${ISSUER_ID}.${String(ticket.ticketId).replace(/-/g, '')}`,
    classId,
    state: ticket.screeningAt + 4 * 60 * 60 * 1000 > Date.now() ? 'ACTIVE' : 'EXPIRED',
    barcode: {
      type: ticket.barcodeFormat || 'QR_CODE',
      value: ticket.barcodeValue,
      ...(ticket.bookingReference ? { alternateText: ticket.bookingReference } : {}),
    },
    ...(ticket.bookingReference ? { ticketNumber: ticket.bookingReference } : {}),
    ...(ticket.seats || ticket.room
      ? {
          seatInfo: {
            ...(ticket.seats ? { seat: localized(ticket.seats) } : {}),
            ...(ticket.room ? { section: localized(ticket.room) } : {}),
          },
        }
      : {}),
    textModulesData: [
      { id: 'cinema', header: 'Cinéma', body: ticket.cinemaName },
      ...(ticket.room ? [{ id: 'room', header: 'Salle', body: ticket.room }] : []),
      ...(ticket.seats ? [{ id: 'seats', header: 'Sièges', body: ticket.seats }] : []),
    ],
  };

  return { eventTicketClasses: [eventTicketClass], eventTicketObjects: [eventTicketObject] };
}

function signJwt(payload) {
  const header = { alg: 'RS256', typ: 'JWT' };
  const claims = {
    iss: credentials.client_email,
    aud: 'google',
    typ: 'savetowallet',
    iat: Math.floor(Date.now() / 1000),
    origins: [],
    payload,
  };

  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(claims))}`;
  const signature = crypto
    .createSign('RSA-SHA256')
    .update(signingInput)
    .sign(credentials.private_key);

  return `${signingInput}.${base64url(signature)}`;
}

const server = http.createServer((req, res) => {
  if (req.method !== 'POST' || !req.url.startsWith('/wallet/jwt')) {
    res.writeHead(404).end('Not found');
    return;
  }

  let body = '';
  req.on('data', (chunk) => {
    body += chunk;
    // Un billet fait quelques centaines d'octets : au-delà, on coupe.
    if (body.length > 64 * 1024) req.destroy();
  });

  req.on('end', () => {
    try {
      const ticket = JSON.parse(body);
      if (!ticket.barcodeValue) throw new Error('barcodeValue manquant');

      const jwt = signJwt(buildPayload(ticket));
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ jwt }));
    } catch (error) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: error.message }));
    }
  });
});

server.listen(PORT, () => {
  console.log(`Signataire Wallet à l'écoute sur http://localhost:${PORT}/wallet/jwt`);
});
