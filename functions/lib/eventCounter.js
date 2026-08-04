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
exports.onEventWritten = void 0;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-functions/v2/firestore");
const db = admin.firestore();
/**
 * Počet živých akcí uživatele v `users/{uid}.activeEventCount`.
 *
 * Pravidla Firestore neumí počítat dokumenty — žádná agregace v nich není —
 * takže „nejvýš jedna akce zdarma" se pravidlem napsat nedá. Číslo proto
 * udržuje tenhle trigger a pravidlo se pak ptá jen na hotovou hodnotu.
 *
 * Pole píše výhradně tahle funkce pod admin právy; klientovi ho pravidla
 * zakazují stejně jako tarif, jinak by si limit přepsal sám.
 *
 * Záměrně se přepočítává dotazem, ne přičítáním a odečítáním. Přírůstkové
 * počítadlo se při každém zameškaném spuštění rozejde s realitou a už se
 * nikdy nesrovná; přepočet je dražší, ale po každém běhu sedí.
 */
async function recount(uid) {
    if (!uid)
        return;
    const snap = await db.collection("events").where("createdBy", "==", uid).get();
    const active = snap.docs.filter((d) => {
        var _a;
        const status = (_a = d.data()) === null || _a === void 0 ? void 0 : _a.status;
        return status !== "COMPLETED" && status !== "CANCELLED";
    }).length;
    await db.collection("users").doc(uid).set({ activeEventCount: active }, { merge: true });
}
/**
 * Musí to být funkce 2. generace: databáze leží v multiregionu `eur3`, který
 * Firestore triggery 1. generace neumí obsloužit ani v evropském regionu.
 * Starší triggery v projektu běží v us-central1 ještě z doby před migrací.
 */
exports.onEventWritten = (0, firestore_1.onDocumentWritten)({ document: "events/{eventId}", region: "europe-west1" }, async (event) => {
    var _a, _b, _c, _d, _e, _f, _g;
    const before = (_a = event.data) === null || _a === void 0 ? void 0 : _a.before;
    const after = (_b = event.data) === null || _b === void 0 ? void 0 : _b.after;
    // Přepočítávat po každé úpravě akce by bylo zbytečně drahé — na počet
    // má vliv jen vznik, zánik a změna stavu.
    const statusChanged = ((_c = before === null || before === void 0 ? void 0 : before.data()) === null || _c === void 0 ? void 0 : _c.status) !== ((_d = after === null || after === void 0 ? void 0 : after.data()) === null || _d === void 0 ? void 0 : _d.status);
    const existenceChanged = ((_e = before === null || before === void 0 ? void 0 : before.exists) !== null && _e !== void 0 ? _e : false) !== ((_f = after === null || after === void 0 ? void 0 : after.exists) !== null && _f !== void 0 ? _f : false);
    if (!statusChanged && !existenceChanged)
        return;
    // Majitel se nepřevádí, ale při smazání zůstane jen ta předchozí verze
    const owners = new Set();
    for (const snap of [before, after]) {
        const uid = (_g = snap === null || snap === void 0 ? void 0 : snap.data()) === null || _g === void 0 ? void 0 : _g.createdBy;
        if (typeof uid === "string")
            owners.add(uid);
    }
    await Promise.all([...owners].map(recount));
});
//# sourceMappingURL=eventCounter.js.map