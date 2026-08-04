import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Pravidelné srovnání tarifů podle RevenueCatu.
 *
 * Webhook je rychlejší, ale nastavit se dá jen v konzoli RevenueCatu — přes
 * jejich API nejde (`/webhooks` vrací 404). Aby vydání neviselo na ručním
 * kroku, tohle si tarify obchází samo. Webhook zůstává zapojený a když ho
 * někdo nastaví, propíše změnu okamžitě; tohle je záchranná síť, která
 * funguje i bez něj.
 *
 * Zdrojem pravdy je RevenueCat, ne klient — proto se čte přes jejich API
 * secret klíčem, ne z toho, co pošle aplikace.
 */

const RC_BASE = "https://api.revenuecat.com/v2";
const PROJECT_ID = "proj0ace5db7";

/** Jak si RevenueCat značí „nevyprší" — rok 2100. */
const NEVER_EXPIRES_FROM = Date.parse("2090-01-01T00:00:00Z");

type Tier = "FREE" | "PRO" | "BUSINESS";

async function rc(path: string): Promise<any> {
  const key = process.env.REVENUECAT_SECRET_KEY;
  if (!key) throw new Error("REVENUECAT_SECRET_KEY není nastavené");

  const res = await fetch(`${RC_BASE}${path}`, {
    headers: { Authorization: `Bearer ${key}`, Accept: "application/json" },
  });
  if (!res.ok) {
    throw new Error(`RevenueCat ${res.status}: ${(await res.text()).slice(0, 200)}`);
  }
  return res.json();
}

/** Mapa id entitlementu na jeho lookup_key (pro, business). */
async function entitlementKeys(): Promise<Map<string, string>> {
  const data = await rc(`/projects/${PROJECT_ID}/entitlements?limit=100`);
  return new Map((data.items ?? []).map((e: any) => [e.id, e.lookup_key]));
}

export const reconcileTiers = functions
  .runWith({ timeoutSeconds: 540 })
  .pubsub.schedule("every 60 minutes")
  .onRun(async () => {
    const keys = await entitlementKeys();

    let startingAfter: string | undefined;
    let checked = 0;
    let changed = 0;

    do {
      const query = new URLSearchParams({ limit: "100" });
      if (startingAfter) query.set("starting_after", startingAfter);
      const page = await rc(`/projects/${PROJECT_ID}/customers?${query}`);

      const customers: any[] = page.items ?? [];
      for (const customer of customers) {
        const uid: string = customer.id;
        // Anonymní zákazník ještě nemá účet, není co s čím spojit
        if (!uid || uid.startsWith("$RCAnonymousID")) continue;

        checked += 1;

        const active = await rc(
          `/projects/${PROJECT_ID}/customers/${encodeURIComponent(uid)}/active_entitlements`
        );
        const items: any[] = active.items ?? [];
        const lookups = items.map((i) => keys.get(i.entitlement_id)).filter(Boolean);

        let tier: Tier = "FREE";
        if (lookups.includes("business")) tier = "BUSINESS";
        else if (lookups.includes("pro")) tier = "PRO";

        // Nejzazší expirace z aktivních nároků; rok 2100 znamená doživotní,
        // což se ukládá jako null — jinak by ho denní úklid jednou shodil.
        const expirations = items.map((i) => Number(i.expires_at ?? 0)).filter(Boolean);
        const maxExpiry = expirations.length > 0 ? Math.max(...expirations) : null;
        const tierExpiresAt =
          maxExpiry === null || maxExpiry >= NEVER_EXPIRES_FROM ? null : maxExpiry;

        const ref = db.collection("users").doc(uid);
        const snap = await ref.get();
        const current = snap.data() ?? {};

        if (current.tier === tier && (current.tierExpiresAt ?? null) === tierExpiresAt) continue;

        await ref.set(
          {
            tier,
            tierExpiresAt,
            tierUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
            tierSource: "REVENUECAT_RECONCILE",
          },
          { merge: true }
        );
        changed += 1;
        console.log(`tarif ${uid}: ${current.tier ?? "—"} → ${tier}`);
      }

      startingAfter = page.next_page ? customers[customers.length - 1]?.id : undefined;
    } while (startingAfter);

    console.log(`zkontrolováno ${checked} zákazníků, změněno ${changed}`);
    return null;
  });
