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
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
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
exports.onEventWritten = functions.firestore
    .document("events/{eventId}")
    .onWrite(async (change) => {
    const before = change.before.data();
    const after = change.after.data();
    // Majitel se nepřevádí, ale při smazání zůstane jen ta předchozí verze
    const owners = new Set();
    if (typeof (before === null || before === void 0 ? void 0 : before.createdBy) === "string")
        owners.add(before.createdBy);
    if (typeof (after === null || after === void 0 ? void 0 : after.createdBy) === "string")
        owners.add(after.createdBy);
    // Přepočítávat po každé úpravě akce by bylo zbytečně drahé — na počet
    // má vliv jen vznik, zánik a změna stavu.
    const statusChanged = (before === null || before === void 0 ? void 0 : before.status) !== (after === null || after === void 0 ? void 0 : after.status);
    const existenceChanged = change.before.exists !== change.after.exists;
    if (!statusChanged && !existenceChanged)
        return;
    await Promise.all([...owners].map(recount));
});
//# sourceMappingURL=eventCounter.js.map