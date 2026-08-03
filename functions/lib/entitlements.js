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
exports.expireStaleTiers = exports.revenueCatWebhook = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
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
function tierFrom(entitlementIds) {
    if (entitlementIds.includes(ENTITLEMENT_BUSINESS))
        return "BUSINESS";
    if (entitlementIds.includes(ENTITLEMENT_PRO))
        return "PRO";
    return "FREE";
}
exports.revenueCatWebhook = functions.https.onRequest(async (request, response) => {
    var _a, _b, _c, _d;
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
    const event = (_a = request.body) === null || _a === void 0 ? void 0 : _a.event;
    if (!event) {
        response.status(400).send("missing event");
        return;
    }
    const uid = event.app_user_id;
    if (!uid || uid.startsWith("$RCAnonymousID")) {
        // Anonymní uživatel ještě nemá účet, není co komu přiřadit
        response.status(200).send("ignored");
        return;
    }
    const type = (_b = event.type) !== null && _b !== void 0 ? _b : "";
    const entitlementIds = (_c = event.entitlement_ids) !== null && _c !== void 0 ? _c : [];
    let tier;
    if (GRANTING.has(type)) {
        tier = tierFrom(entitlementIds);
    }
    else if (REVOKING.has(type)) {
        tier = "FREE";
    }
    else {
        // CANCELLATION, BILLING_ISSUE a spol. přístup nemění
        response.status(200).send("no change");
        return;
    }
    // `expiration_at_ms` chybí u doživotního nákupu — tam je null správně
    // a znamená „bez expirace", ne „nevím".
    const expiresAt = (_d = event.expiration_at_ms) !== null && _d !== void 0 ? _d : null;
    await db.collection("users").doc(uid).set({
        tier,
        tierExpiresAt: expiresAt,
        tierUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        tierSource: type,
    }, { merge: true });
    console.log(`tarif ${uid} → ${tier} (${type})`);
    response.status(200).send("ok");
});
/**
 * Vyprší-li předplatné, RevenueCat pošle EXPIRATION. Kdyby se webhook ztratil,
 * zůstal by uživateli tarif napořád — tohle jednou denně dorovná ty, kterým
 * datum platnosti uplynulo.
 */
exports.expireStaleTiers = functions.pubsub
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
//# sourceMappingURL=entitlements.js.map