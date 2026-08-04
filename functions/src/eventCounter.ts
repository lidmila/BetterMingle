import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

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
async function recount(uid: string): Promise<void> {
  if (!uid) return;

  const snap = await db.collection("events").where("createdBy", "==", uid).get();
  const active = snap.docs.filter((d) => {
    const status = d.data()?.status;
    return status !== "COMPLETED" && status !== "CANCELLED";
  }).length;

  await db.collection("users").doc(uid).set({ activeEventCount: active }, { merge: true });
}

export const onEventWritten = functions.firestore
  .document("events/{eventId}")
  .onWrite(async (change) => {
    const before = change.before.data();
    const after = change.after.data();

    // Majitel se nepřevádí, ale při smazání zůstane jen ta předchozí verze
    const owners = new Set<string>();
    if (typeof before?.createdBy === "string") owners.add(before.createdBy);
    if (typeof after?.createdBy === "string") owners.add(after.createdBy);

    // Přepočítávat po každé úpravě akce by bylo zbytečně drahé — na počet
    // má vliv jen vznik, zánik a změna stavu.
    const statusChanged = before?.status !== after?.status;
    const existenceChanged = change.before.exists !== change.after.exists;
    if (!statusChanged && !existenceChanged) return;

    await Promise.all([...owners].map(recount));
  });
