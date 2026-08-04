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
exports.reconcileTiers = exports.expireStaleTiers = exports.revenueCatWebhook = exports.joinByInviteCode = exports.generateInviteLink = exports.onEventUpdated = exports.onNewExpense = exports.onNewPoll = exports.onNewMessage = void 0;
const admin = __importStar(require("firebase-admin"));
admin.initializeApp();
var notifications_1 = require("./notifications");
Object.defineProperty(exports, "onNewMessage", { enumerable: true, get: function () { return notifications_1.onNewMessage; } });
Object.defineProperty(exports, "onNewPoll", { enumerable: true, get: function () { return notifications_1.onNewPoll; } });
Object.defineProperty(exports, "onNewExpense", { enumerable: true, get: function () { return notifications_1.onNewExpense; } });
Object.defineProperty(exports, "onEventUpdated", { enumerable: true, get: function () { return notifications_1.onEventUpdated; } });
var invitations_1 = require("./invitations");
Object.defineProperty(exports, "generateInviteLink", { enumerable: true, get: function () { return invitations_1.generateInviteLink; } });
Object.defineProperty(exports, "joinByInviteCode", { enumerable: true, get: function () { return invitations_1.joinByInviteCode; } });
var entitlements_1 = require("./entitlements");
Object.defineProperty(exports, "revenueCatWebhook", { enumerable: true, get: function () { return entitlements_1.revenueCatWebhook; } });
Object.defineProperty(exports, "expireStaleTiers", { enumerable: true, get: function () { return entitlements_1.expireStaleTiers; } });
var reconcileTiers_1 = require("./reconcileTiers");
Object.defineProperty(exports, "reconcileTiers", { enumerable: true, get: function () { return reconcileTiers_1.reconcileTiers; } });
//# sourceMappingURL=index.js.map