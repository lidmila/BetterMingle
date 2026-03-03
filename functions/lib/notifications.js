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
exports.onEventUpdated = exports.onNewExpense = exports.onNewPoll = exports.onNewMessage = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const db = admin.firestore();
const messaging = admin.messaging();
/**
 * Send push notification when a new message is posted in event chat.
 */
exports.onNewMessage = functions.firestore
    .document("events/{eventId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
    var _a;
    const { eventId } = context.params;
    const message = snapshot.data();
    const senderName = message.userName || "Někdo";
    const content = message.content || "";
    const event = await db.collection("events").doc(eventId).get();
    const eventName = ((_a = event.data()) === null || _a === void 0 ? void 0 : _a.name) || "Akce";
    // Get all participants except sender
    const participants = await db
        .collection("events")
        .doc(eventId)
        .collection("participants")
        .get();
    const tokens = await getTokensForParticipants(participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid) => uid !== message.userId));
    if (tokens.length === 0)
        return;
    const payload = {
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
exports.onNewPoll = functions.firestore
    .document("events/{eventId}/polls/{pollId}")
    .onCreate(async (snapshot, context) => {
    var _a;
    const { eventId } = context.params;
    const poll = snapshot.data();
    const event = await db.collection("events").doc(eventId).get();
    const eventName = ((_a = event.data()) === null || _a === void 0 ? void 0 : _a.name) || "Akce";
    const participants = await db
        .collection("events")
        .doc(eventId)
        .collection("participants")
        .get();
    const tokens = await getTokensForParticipants(participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid) => uid !== poll.createdBy));
    if (tokens.length === 0)
        return;
    const payload = {
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
exports.onNewExpense = functions.firestore
    .document("events/{eventId}/expenses/{expenseId}")
    .onCreate(async (snapshot, context) => {
    const { eventId } = context.params;
    const expense = snapshot.data();
    // Skip notification if event hides financials
    const event = await db.collection("events").doc(eventId).get();
    const eventData = event.data();
    if ((eventData === null || eventData === void 0 ? void 0 : eventData.securityEnabled) && (eventData === null || eventData === void 0 ? void 0 : eventData.hideFinancials))
        return;
    const eventName = (eventData === null || eventData === void 0 ? void 0 : eventData.name) || "Akce";
    const amount = expense.amount || 0;
    const currency = expense.currency || "CZK";
    const description = expense.description || "Výdaj";
    const participants = await db
        .collection("events")
        .doc(eventId)
        .collection("participants")
        .get();
    const tokens = await getTokensForParticipants(participants.docs
        .map((doc) => doc.data().userId)
        .filter((uid) => uid !== expense.paidBy));
    if (tokens.length === 0)
        return;
    const payload = {
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
exports.onEventUpdated = functions.firestore
    .document("events/{eventId}")
    .onUpdate(async (change, context) => {
    const { eventId } = context.params;
    const before = change.before.data();
    const after = change.after.data();
    // Only notify on meaningful changes
    const changed = [];
    if (before.name !== after.name)
        changed.push("název");
    if (before.startDate !== after.startDate)
        changed.push("datum");
    if (before.locationName !== after.locationName)
        changed.push("místo");
    if (before.status !== after.status)
        changed.push("stav");
    if (changed.length === 0)
        return;
    const participants = await db
        .collection("events")
        .doc(eventId)
        .collection("participants")
        .get();
    const tokens = await getTokensForParticipants(participants.docs.map((doc) => doc.data().userId));
    if (tokens.length === 0)
        return;
    const payload = {
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
async function getTokensForParticipants(userIds) {
    var _a;
    const tokens = [];
    for (const uid of userIds) {
        try {
            const userDoc = await db.collection("users").doc(uid).get();
            const fcmToken = (_a = userDoc.data()) === null || _a === void 0 ? void 0 : _a.fcmToken;
            if (fcmToken)
                tokens.push(fcmToken);
        }
        catch (_) {
            // Skip users without tokens
        }
    }
    return tokens;
}
//# sourceMappingURL=notifications.js.map