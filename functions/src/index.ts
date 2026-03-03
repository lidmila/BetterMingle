import * as admin from "firebase-admin";

admin.initializeApp();

export { onNewMessage, onNewPoll, onNewExpense, onEventUpdated } from "./notifications";
export { generateInviteLink, joinByInviteCode } from "./invitations";
