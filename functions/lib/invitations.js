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
exports.joinByInviteCode = exports.generateInviteLink = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const db = admin.firestore();
/**
 * Generate a shareable invite link for an event.
 * Called by the app when user wants to share an event.
 */
exports.generateInviteLink = functions.https.onCall(async (request) => {
    var _a, _b;
    const uid = (_a = request.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!uid)
        throw new functions.https.HttpsError("unauthenticated", "Musíš být přihlášen/a.");
    const eventId = (_b = request.data) === null || _b === void 0 ? void 0 : _b.eventId;
    if (!eventId)
        throw new functions.https.HttpsError("invalid-argument", "Chybí eventId.");
    // Verify user is organizer
    const event = await db.collection("events").doc(eventId).get();
    if (!event.exists)
        throw new functions.https.HttpsError("not-found", "Akce nenalezena.");
    const eventData = event.data();
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
exports.joinByInviteCode = functions.https.onCall(async (request) => {
    var _a, _b, _c, _d, _e, _f;
    const uid = (_a = request.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!uid)
        throw new functions.https.HttpsError("unauthenticated", "Musíš být přihlášen/a.");
    const inviteCode = (_b = request.data) === null || _b === void 0 ? void 0 : _b.inviteCode;
    const pin = (_c = request.data) === null || _c === void 0 ? void 0 : _c.pin;
    if (!inviteCode)
        throw new functions.https.HttpsError("invalid-argument", "Chybí kód pozvánky.");
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
            const userName = ((_d = userDoc.data()) === null || _d === void 0 ? void 0 : _d.displayName) || "";
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
    const userName = ((_e = userDoc.data()) === null || _e === void 0 ? void 0 : _e.displayName) || "";
    const userAvatar = ((_f = userDoc.data()) === null || _f === void 0 ? void 0 : _f.avatarUrl) || "";
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
function generateCode() {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    let code = "";
    for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
}
//# sourceMappingURL=invitations.js.map