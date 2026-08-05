import * as admin from "firebase-admin";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

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
function prefsOf(snap: FirebaseFirestore.DocumentSnapshot | undefined): string[] {
  const raw = snap?.data()?.dietaryPreferences;
  return Array.isArray(raw) ? raw.filter((x): x is string => typeof x === "string") : [];
}

const same = (a: string[], b: string[]): boolean =>
  a.length === b.length && a.every((x, i) => x === b[i]);

export const onProfileDietaryWritten = onDocumentWritten(
  { document: "users/{uid}", region: "europe-west1" },
  async (event) => {
    const uid = event.params.uid;
    const before = prefsOf(event.data?.before);
    const after = prefsOf(event.data?.after);

    // Profil se ukládá při každé maličkosti — jméno, avatar, tarif. Rozesílat
    // kopie po celé databázi kvůli změně avataru by byla zbytečná práce.
    if (same(before, after)) return;

    const snap = await db
      .collectionGroup("participants")
      .where("userId", "==", uid)
      .get();

    if (snap.empty) return;

    // Dávka zvládne 500 zápisů; víc akcí na jednoho člověka je nepravděpodobné,
    // ale rozdělení stojí tři řádky a ušetří pád při hromadném importu.
    for (let i = 0; i < snap.docs.length; i += 400) {
      const batch = db.batch();
      for (const d of snap.docs.slice(i, i + 400)) {
        batch.set(d.ref, { dietaryPreferences: after }, { merge: true });
      }
      await batch.commit();
    }
  }
);
