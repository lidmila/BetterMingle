"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.reconcileTiers = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
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
async function rc(path) {
    const key = process.env.REVENUECAT_SECRET_KEY;
    if (!key)
        throw new Error("REVENUECAT_SECRET_KEY není nastavené");
    const res = await fetch(`${RC_BASE}${path}`, {
        headers: { Authorization: `Bearer ${key}`, Accept: "application/json" },
    });
    if (!res.ok) {
        throw new Error(`RevenueCat ${res.status}: ${(await res.text()).slice(0, 200)}`);
    }
    return res.json();
}
/** Mapa id entitlementu na jeho lookup_key (pro, business). */
async function entitlementKeys() {
    var _a;
    const data = await rc(`/projects/${PROJECT_ID}/entitlements?limit=100`);
    return new Map(((_a = data.items) !== null && _a !== void 0 ? _a : []).map((e) => [e.id, e.lookup_key]));
}
exports.reconcileTiers = functions
    .runWith({ timeoutSeconds: 540 })
    .pubsub.schedule("every 60 minutes")
    .onRun(async () => {
    var _a, _b, _c, _d, _e, _f;
    const keys = await entitlementKeys();
    let startingAfter;
    let checked = 0;
    let changed = 0;
    do {
        const query = new URLSearchParams({ limit: "100" });
        if (startingAfter)
            query.set("starting_after", startingAfter);
        const page = await rc(`/projects/${PROJECT_ID}/customers?${query}`);
        const customers = (_a = page.items) !== null && _a !== void 0 ? _a : [];
        for (const customer of customers) {
            const uid = customer.id;
            // Anonymní zákazník ještě nemá účet, není co s čím spojit
            if (!uid || uid.startsWith("$RCAnonymousID"))
                continue;
            checked += 1;
            const active = await rc(`/projects/${PROJECT_ID}/customers/${encodeURIComponent(uid)}/active_entitlements`);
            const items = (_b = active.items) !== null && _b !== void 0 ? _b : [];
            const lookups = items.map((i) => keys.get(i.entitlement_id)).filter(Boolean);
            let tier = "FREE";
            if (lookups.includes("business"))
                tier = "BUSINESS";
            else if (lookups.includes("pro"))
                tier = "PRO";
            // Nejzazší expirace z aktivních nároků; rok 2100 znamená doživotní,
            // což se ukládá jako null — jinak by ho denní úklid jednou shodil.
            const expirations = items.map((i) => { var _a; return Number((_a = i.expires_at) !== null && _a !== void 0 ? _a : 0); }).filter(Boolean);
            const maxExpiry = expirations.length > 0 ? Math.max(...expirations) : null;
            const tierExpiresAt = maxExpiry === null || maxExpiry >= NEVER_EXPIRES_FROM ? null : maxExpiry;
            const ref = db.collection("users").doc(uid);
            const snap = await ref.get();
            const current = (_c = snap.data()) !== null && _c !== void 0 ? _c : {};
            if (current.tier === tier && ((_d = current.tierExpiresAt) !== null && _d !== void 0 ? _d : null) === tierExpiresAt)
                continue;
            await ref.set({
                tier,
                tierExpiresAt,
                tierUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
                tierSource: "REVENUECAT_RECONCILE",
            }, { merge: true });
            changed += 1;
            console.log(`tarif ${uid}: ${(_e = current.tier) !== null && _e !== void 0 ? _e : "—"} → ${tier}`);
        }
        startingAfter = page.next_page ? (_f = customers[customers.length - 1]) === null || _f === void 0 ? void 0 : _f.id : undefined;
    } while (startingAfter);
    console.log(`zkontrolováno ${checked} zákazníků, změněno ${changed}`);
    return null;
});
//# sourceMappingURL=reconcileTiers.js.map