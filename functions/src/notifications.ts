import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Send push notification when a new message is posted in event chat.
 */
export const onNewMessage = functions.firestore
  .document("events/{eventId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const { eventId } = context.params;
    const message = snapshot.data();
    const senderName = message.userName || "Někdo";
    const content = message.content || "";

    const event = await db.collection("events").doc(eventId).get();
    const eventName = event.data()?.name || "Akce";

    // Get all participants except sender
    const participants = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .get();

    const tokens = await getTokensForParticipants(
      participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid: string) => uid !== message.userId)
    );

    if (tokens.length === 0) return;

    const payload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title: `${eventName} – ${senderName}`,
        body: content.length > 100 ? content.substring(0, 100) + "..." : content,
      },
      data: {
        type: "chat",
        eventId,
        messageId: context.params.messageId,
      },
      android: {
        notification: {
          channelId: "chat_messages",
        },
      },
    };

    await messaging.sendEachForMulticast(payload);
  });

/**
 * Send push notification when a new poll is created.
 */
export const onNewPoll = functions.firestore
  .document("events/{eventId}/polls/{pollId}")
  .onCreate(async (snapshot, context) => {
    const { eventId } = context.params;
    const poll = snapshot.data();

    const event = await db.collection("events").doc(eventId).get();
    const eventName = event.data()?.name || "Akce";

    const participants = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .get();

    const tokens = await getTokensForParticipants(
      participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid: string) => uid !== poll.createdBy)
    );

    if (tokens.length === 0) return;

    const payload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title: `${eventName} – Nová anketa`,
        body: poll.title || "Hlasuj v nové anketě",
      },
      data: {
        type: "poll",
        eventId,
        pollId: context.params.pollId,
      },
      android: {
        notification: {
          channelId: "polls",
        },
      },
    };

    await messaging.sendEachForMulticast(payload);
  });

/**
 * Send push notification when a new expense is added.
 */
export const onNewExpense = functions.firestore
  .document("events/{eventId}/expenses/{expenseId}")
  .onCreate(async (snapshot, context) => {
    const { eventId } = context.params;
    const expense = snapshot.data();

    // Skip notification if event hides financials
    const event = await db.collection("events").doc(eventId).get();
    const eventData = event.data();
    if (eventData?.securityEnabled && eventData?.hideFinancials) return;

    const eventName = eventData?.name || "Akce";
    const amount = expense.amount || 0;
    const currency = expense.currency || "CZK";
    const description = expense.description || "Výdaj";

    const participants = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .get();

    const tokens = await getTokensForParticipants(
      participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid: string) => uid !== expense.paidBy)
    );

    if (tokens.length === 0) return;

    const payload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title: `${eventName} – Nový výdaj`,
        body: `${description}: ${amount} ${currency}`,
      },
      data: {
        type: "expense",
        eventId,
      },
      android: {
        notification: {
          channelId: "expenses",
        },
      },
    };

    await messaging.sendEachForMulticast(payload);
  });

/**
 * Send push notification when event details are updated.
 */
export const onEventUpdated = functions.firestore
  .document("events/{eventId}")
  .onUpdate(async (change, context) => {
    const { eventId } = context.params;
    const before = change.before.data();
    const after = change.after.data();

    // Only notify on meaningful changes
    const changed: string[] = [];
    if (before.name !== after.name) changed.push("název");
    if (before.startDate !== after.startDate) changed.push("datum");
    if (before.locationName !== after.locationName) changed.push("místo");
    if (before.status !== after.status) changed.push("stav");

    if (changed.length === 0) return;

    const participants = await db
      .collection("events")
      .doc(eventId)
      .collection("participants")
      .get();

    const tokens = await getTokensForParticipants(
      participants.docs.map((doc) => doc.data().userId)
    );

    if (tokens.length === 0) return;

    const payload: admin.messaging.MulticastMessage = {
      tokens,
      notification: {
        title: `${after.name} – Aktualizace`,
        body: `Změněno: ${changed.join(", ")}`,
      },
      data: {
        type: "event_update",
        eventId,
      },
      android: {
        notification: {
          channelId: "event_updates",
        },
      },
    };

    await messaging.sendEachForMulticast(payload);
  });

/**
 * Get FCM tokens for a list of user IDs.
 */
async function getTokensForParticipants(userIds: string[]): Promise<string[]> {
  const tokens: string[] = [];

  for (const uid of userIds) {
    try {
      const userDoc = await db.collection("users").doc(uid).get();
      const fcmToken = userDoc.data()?.fcmToken;
      if (fcmToken) tokens.push(fcmToken);
    } catch (_) {
      // Skip users without tokens
    }
  }

  return tokens;
}
