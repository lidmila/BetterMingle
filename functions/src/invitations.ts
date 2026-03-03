import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

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

  // Check max participants
  if (eventData.maxParticipants > 0) {
    const participantCount = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .count()
      .get();

    if (participantCount.data().count >= eventData.maxParticipants) {
      throw new functions.https.HttpsError("resource-exhausted", "Akce je plná.");
    }
  }

  // Security checks
  if (eventData.securityEnabled) {
    // PIN verification
    if (eventData.eventPin && eventData.eventPin !== pin) {
      throw new functions.https.HttpsError("permission-denied", "Nesprávný PIN.");
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
    });

  return { eventId, status: "joined", eventName: eventData.name };
});

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
