import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Kolik účastníků pustí tarif pořadatele. Musí odpovídat `maxParticipants()`
 * v aplikaci (src/core/tiers.ts na webu, src/tiers.ts v mobilu) — kdyby se to
 * rozešlo, uživatel narazí na strop, o kterém se nikde nedočetl.
 *
 * Tarif se čte z `users/{uid}.tier`, kam ho píše jen webhook z RevenueCatu
 * pod admin právy. Chybějící hodnota znamená FREE, což je bezpečný výchozí
 * stav pro každého, kdo si nic nekoupil.
 */
async function participantCapOf(ownerUid: string): Promise<number> {
  if (!ownerUid) return 20;
  const snap = await db.collection("users").doc(ownerUid).get();
  switch (snap.data()?.tier) {
  case "BUSINESS":
    return Number.MAX_SAFE_INTEGER;
  case "PRO":
    return 100;
  default:
    return 20;
  }
}

/**
 * Generate a shareable invite link for an event.
 * Called by the app when user wants to share an event.
 */
export const generateInviteLink = functions.https.onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Musíš být přihlášen/a.");

  const eventId = request.data?.eventId;
  if (!eventId) throw new functions.https.HttpsError("invalid-argument", "Chybí eventId.");

  // Verify user is organizer
  const event = await db.collection("events").doc(eventId).get();
  if (!event.exists) throw new functions.https.HttpsError("not-found", "Akce nenalezena.");

  const eventData = event.data()!;
  const isOrganizer = eventData.createdBy === uid;
  const isParticipant = await db
    .collection("events")
    .doc(eventId)
    .collection("participants")
    .doc(uid)
    .get()
    .then((doc) => doc.exists);

  if (!isOrganizer && !isParticipant) {
    throw new functions.https.HttpsError("permission-denied", "Nejsi účastníkem této akce.");
  }

  let inviteCode = eventData.inviteCode;

  // Generate new code if none exists
  if (!inviteCode) {
    inviteCode = generateCode();
    await db.collection("events").doc(eventId).update({ inviteCode });
  }

  return {
    inviteCode,
    inviteLink: `https://bettermingle.app/invite/${inviteCode}`,
    eventName: eventData.name,
  };
});

/**
 * Join an event using an invite code.
 * Handles both regular and security-protected events.
 */
export const joinByInviteCode = functions.https.onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Musíš být přihlášen/a.");

  const inviteCode = request.data?.inviteCode;
  const pin = request.data?.pin;

  if (!inviteCode) throw new functions.https.HttpsError("invalid-argument", "Chybí kód pozvánky.");

  // Find event by invite code
  const eventsSnapshot = await db
    .collection("events")
    .where("inviteCode", "==", inviteCode)
    .limit(1)
    .get();

  if (eventsSnapshot.empty) {
    throw new functions.https.HttpsError("not-found", "Neplatný kód pozvánky.");
  }

  const eventDoc = eventsSnapshot.docs[0];
  const eventData = eventDoc.data();
  const eventId = eventDoc.id;

  // Check if already a participant
  const existingParticipant = await db
    .collection("events")
    .doc(eventId)
    .collection("participants")
    .where("userId", "==", uid)
    .limit(1)
    .get();

  if (!existingParticipant.empty) {
    return { eventId, status: "already_joined", eventName: eventData.name };
  }

  /**
   * Kapacita akce. Pole `maxParticipants` si nastavuje klient, takže se na něj
   * samo o sobě spolehnout nedá — upravená aplikace by si tam napsala cokoli
   * a limit tarifu obešla. Rozhoduje proto přísnější ze dvou čísel: přání
   * pořadatele a strop jeho tarifu.
   */
  const tierCap = await participantCapOf(eventData.createdBy);
  const wanted = typeof eventData.maxParticipants === "number" && eventData.maxParticipants > 0
    ? eventData.maxParticipants
    : Number.MAX_SAFE_INTEGER;
  const cap = Math.min(wanted, tierCap);

  if (cap < Number.MAX_SAFE_INTEGER) {
    const participantCount = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .count()
      .get();

    if (participantCount.data().count >= cap) {
      throw new functions.https.HttpsError("resource-exhausted", "Akce je plná.");
    }
  }

  // Security checks
  if (eventData.securityEnabled) {
    // PIN se čte z private/security — v dokumentu akce by ho viděl každý
    // účastník. Starší akce ho ještě mají u sebe, proto ta záloha.
    const secretDoc = await db
      .collection("events")
      .doc(eventId)
      .collection("private")
      .doc("security")
      .get();
    const eventPin = secretDoc.data()?.pin ?? eventData.eventPin ?? "";

    if (eventPin) {
      await assertNotRateLimited(uid, eventId);

      if (eventPin !== pin) {
        await recordFailedPin(uid, eventId);
        throw new functions.https.HttpsError("permission-denied", "Nesprávný PIN.");
      }
      await clearFailedPins(uid, eventId);
    }

    // Approval required
    if (eventData.requireApproval) {
      // Create a join request instead of adding directly
      const userDoc = await db.collection("users").doc(uid).get();
      const userName = userDoc.data()?.displayName || "";

      await db.collection("joinRequests").add({
        eventId,
        userId: uid,
        displayName: userName,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { eventId, status: "pending_approval", eventName: eventData.name };
    }
  }

  // Add as participant
  const userDoc = await db.collection("users").doc(uid).get();
  const userName = userDoc.data()?.displayName || "";
  const userAvatar = userDoc.data()?.avatarUrl || "";

  await db
    .collection("events")
    .doc(eventId)
    .collection("participants")
    .doc(uid)
    .set({
      userId: uid,
      displayName: userName,
      avatarUrl: userAvatar,
      role: "PARTICIPANT",
      rsvp: "ACCEPTED",
      joinedAt: admin.firestore.FieldValue.serverTimestamp(),
      // Dopisujeme i tahle dvě pole, ať mají účastníci jednotný tvar bez
      // ohledu na to, jestli je přidal pořadatel, nebo přišli přes pozvánku.
      isManual: false,
      linkedUserId: null,
    });

  return { eventId, status: "joined", eventName: eventData.name };
});

/**
 * Ochrana proti hádání PINu. Čtyřmístný PIN má jen 10 000 kombinací, takže
 * bez omezení by ho stačilo zkoušet dost dlouho. Po pěti chybách je dvojice
 * uživatel–akce na 15 minut zablokovaná, počítáno od posledního pokusu.
 */
const MAX_PIN_ATTEMPTS = 5;
const PIN_LOCKOUT_MS = 15 * 60 * 1000;

const attemptRef = (uid: string, eventId: string) =>
  db.collection("pinAttempts").doc(`${uid}_${eventId}`);

async function assertNotRateLimited(uid: string, eventId: string): Promise<void> {
  const snap = await attemptRef(uid, eventId).get();
  const data = snap.data();
  if (!data) return;

  const lastAttempt = data.lastAttemptAt?.toMillis?.() ?? 0;
  // Okno se počítá od posledního pokusu, takže hádání nejde protahovat
  const expired = Date.now() - lastAttempt > PIN_LOCKOUT_MS;
  if (expired) {
    await attemptRef(uid, eventId).delete();
    return;
  }

  if ((data.count ?? 0) >= MAX_PIN_ATTEMPTS) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "Příliš mnoho pokusů o PIN. Zkus to znovu za 15 minut."
    );
  }
}

async function recordFailedPin(uid: string, eventId: string): Promise<void> {
  await attemptRef(uid, eventId).set(
    {
      count: admin.firestore.FieldValue.increment(1),
      lastAttemptAt: admin.firestore.FieldValue.serverTimestamp(),
      uid,
      eventId,
    },
    { merge: true }
  );
}

async function clearFailedPins(uid: string, eventId: string): Promise<void> {
  await attemptRef(uid, eventId).delete().catch(() => undefined);
}

/**
 * Generate a random 6-character alphanumeric invite code.
 */
function generateCode(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}
