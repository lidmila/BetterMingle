import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Tarif z RevenueCatu do Firestore.
 *
 * Do teď hlídala limity jen aplikace. Upravený klient je obešel, protože
 * o zaplaceném tarifu server nic nevěděl. Webhook ho zapíše do
 * `users/{uid}.tier` a pravidla Firestore pak limity vynutí i proti klientovi,
 * kterému nelze věřit.
 *
 * RevenueCat posílá `app_user_id`, což je Firebase UID — aplikace ho nastavuje
 * přes `identifyUser()` hned po přihlášení.
 */

const ENTITLEMENT_BUSINESS = "business";
const ENTITLEMENT_PRO = "pro";

/** Události, po kterých má uživatel mít přístup. */
const GRANTING = new Set([
  "INITIAL_PURCHASE",
  "RENEWAL",
  "UNCANCELLATION",
  "NON_RENEWING_PURCHASE",
  "PRODUCT_CHANGE",
  "SUBSCRIPTION_EXTENDED",
  "TRANSFER",
]);

/** Události, po kterých přístup končí. Zrušení samo o sobě mezi ně nepatří —
 *  po zrušení předplatné běží dál až do konce zaplaceného období. */
const REVOKING = new Set(["EXPIRATION", "REFUND", "SUBSCRIPTION_PAUSED"]);

type Tier = "FREE" | "PRO" | "BUSINESS";

function tierFrom(entitlementIds: string[]): Tier {
  if (entitlementIds.includes(ENTITLEMENT_BUSINESS)) return "BUSINESS";
  if (entitlementIds.includes(ENTITLEMENT_PRO)) return "PRO";
  return "FREE";
}

export const revenueCatWebhook = functions.https.onRequest(async (request, response) => {
  // RevenueCat posílá sdílené tajemství v hlavičce Authorization. Bez ověření
  // by tarif mohl nastavit kdokoli, kdo zná adresu funkce.
  const expected = process.env.REVENUECAT_WEBHOOK_SECRET;
  if (!expected) {
    console.error("REVENUECAT_WEBHOOK_SECRET není nastavené");
    response.status(500).send("not configured");
    return;
  }
  if (request.get("Authorization") !== `Bearer ${expected}`) {
    response.status(401).send("unauthorized");
    return;
  }

  const event = request.body?.event;
  if (!event) {
    response.status(400).send("missing event");
    return;
  }

  const uid: string | undefined = event.app_user_id;
  if (!uid || uid.startsWith("$RCAnonymousID")) {
    // Anonymní uživatel ještě nemá účet, není co komu přiřadit
    response.status(200).send("ignored");
    return;
  }

  const type: string = event.type ?? "";
  const entitlementIds: string[] = event.entitlement_ids ?? [];

  let tier: Tier;
  if (GRANTING.has(type)) {
    tier = tierFrom(entitlementIds);
  } else if (REVOKING.has(type)) {
    tier = "FREE";
  } else {
    // CANCELLATION, BILLING_ISSUE a spol. přístup nemění
    response.status(200).send("no change");
    return;
  }

  // `expiration_at_ms` chybí u doživotního nákupu — tam je null správně
  // a znamená „bez expirace", ne „nevím".
  const expiresAt: number | null = event.expiration_at_ms ?? null;

  await db.collection("users").doc(uid).set(
    {
      tier,
      tierExpiresAt: expiresAt,
      tierUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
      tierSource: type,
    },
    { merge: true }
  );

  console.log(`tarif ${uid} → ${tier} (${type})`);
  response.status(200).send("ok");
});

/**
 * Vyprší-li předplatné, RevenueCat pošle EXPIRATION. Kdyby se webhook ztratil,
 * zůstal by uživateli tarif napořád — tohle jednou denně dorovná ty, kterým
 * datum platnosti uplynulo.
 */
export const expireStaleTiers = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async () => {
    const now = Date.now();
    const stale = await db
      .collection("users")
      .where("tier", "in", ["PRO", "BUSINESS"])
      .where("tierExpiresAt", "<", now)
      .get();

    for (const doc of stale.docs) {
      await doc.ref.set({ tier: "FREE", tierSource: "EXPIRED_SWEEP" }, { merge: true });
      console.log(`tarif ${doc.id} → FREE (uplynulá platnost)`);
    }

    return null;
  });
