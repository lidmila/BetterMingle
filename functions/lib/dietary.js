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
exports.onProfileDietaryWritten = void 0;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-functions/v2/firestore");
const db = admin.firestore();
/**
 * Rozkopírování stravovacích preferencí z profilu k účastníkovi akce.
 *
 * Totéž dělá `syncDietaryPreferences()` v mobilní aplikaci, jenže jen ta nová.
 * Starší build v telefonu o poli u účastníka neví a zapíše preference pouze do
 * profilu — a jakmile profil zavřeme jen pro vlastníka, catering by u takového
 * člověka zůstal navždy prázdný. Tady na verzi aplikace nezáleží: zápis do
 * profilu spustí trigger, ať přišel odkudkoli.
 *
 * Kopie není duplicita pro pohodlí, ale kvůli rozsahu viditelnosti. V profilu
 * jsou preference zdravotní údaj vedle telefonu a e-mailu; u účastníka je vidí
 * jen lidé z té jedné akce, což je přesně tolik, kolik catering potřebuje.
 */
function prefsOf(snap) {
    var _a;
    const raw = (_a = snap === null || snap === void 0 ? void 0 : snap.data()) === null || _a === void 0 ? void 0 : _a.dietaryPreferences;
    return Array.isArray(raw) ? raw.filter((x) => typeof x === "string") : [];
}
const same = (a, b) => a.length === b.length && a.every((x, i) => x === b[i]);
exports.onProfileDietaryWritten = (0, firestore_1.onDocumentWritten)({ document: "users/{uid}", region: "europe-west1" }, async (event) => {
    var _a, _b;
    const uid = event.params.uid;
    const before = prefsOf((_a = event.data) === null || _a === void 0 ? void 0 : _a.before);
    const after = prefsOf((_b = event.data) === null || _b === void 0 ? void 0 : _b.after);
    // Profil se ukládá při každé maličkosti — jméno, avatar, tarif. Rozesílat
    // kopie po celé databázi kvůli změně avataru by byla zbytečná práce.
    if (same(before, after))
        return;
    const snap = await db
        .collectionGroup("participants")
        .where("userId", "==", uid)
        .get();
    if (snap.empty)
        return;
    // Dávka zvládne 500 zápisů; víc akcí na jednoho člověka je nepravděpodobné,
    // ale rozdělení stojí tři řádky a ušetří pád při hromadném importu.
    for (let i = 0; i < snap.docs.length; i += 400) {
        const batch = db.batch();
        for (const d of snap.docs.slice(i, i + 400)) {
            batch.set(d.ref, { dietaryPreferences: after }, { merge: true });
        }
        await batch.commit();
    }
});
//# sourceMappingURL=dietary.js.map