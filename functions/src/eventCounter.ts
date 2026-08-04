import * as admin from "firebase-admin";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

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

/**
 * Musí to být funkce 2. generace: databáze leží v multiregionu `eur3`, který
 * Firestore triggery 1. generace neumí obsloužit ani v evropském regionu.
 * Starší triggery v projektu běží v us-central1 ještě z doby před migrací.
 */
export const onEventWritten = onDocumentWritten(
  { document: "events/{eventId}", region: "europe-west1" },
  async (event) => {
    const before = event.data?.before;
    const after = event.data?.after;

    // Přepočítávat po každé úpravě akce by bylo zbytečně drahé — na počet
    // má vliv jen vznik, zánik a změna stavu.
    const statusChanged = before?.data()?.status !== after?.data()?.status;
    const existenceChanged = (before?.exists ?? false) !== (after?.exists ?? false);
    if (!statusChanged && !existenceChanged) return;

    // Majitel se nepřevádí, ale při smazání zůstane jen ta předchozí verze
    const owners = new Set<string>();
    for (const snap of [before, after]) {
      const uid = snap?.data()?.createdBy;
      if (typeof uid === "string") owners.add(uid);
    }

    await Promise.all([...owners].map(recount));
  }
);
