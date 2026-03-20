const admin = require("firebase-admin");
const path = require("path");

const serviceAccount = require(path.join(process.env.USERPROFILE || process.env.HOME, "Documents/GitHub/bettermingle-firebase-adminsdk-fbsvc-2a0f1fb62e.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: "bettermingle",
});

const db = admin.firestore();

const SUBCOLLECTIONS = [
  "participants",
  "messages",
  "polls",
  "expenses",
  "carpoolRides",
  "rooms",
  "schedule",
  "budgetCategories",
  "packingItems",
  "ratings",
  "photos",
  "activity",
  "wishlistItems",
  "tasks",
  "lastSeen",
];

async function deleteCollection(ref) {
  const snapshot = await ref.get();
  if (snapshot.empty) return 0;
  const batch = db.batch();
  snapshot.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  return snapshot.size;
}

async function deleteSubcollectionsRecursive(docRef) {
  for (const sub of SUBCOLLECTIONS) {
    const subRef = docRef.collection(sub);
    const subSnap = await subRef.get();
    for (const subDoc of subSnap.docs) {
      // delete nested subcollections (e.g. polls/options/votes, expenses/splits)
      const nestedColls = await subDoc.ref.listCollections();
      for (const nested of nestedColls) {
        const count = await deleteCollection(nested);
        if (count > 0) console.log(`  Smazáno ${count} docs z ${nested.path}`);
      }
    }
    const count = await deleteCollection(subRef);
    if (count > 0) console.log(`  Smazáno ${count} docs z ${subRef.path}`);
  }
}

async function main() {
  console.log("Připojuji se k Firestore (bettermingle)...\n");

  const eventsSnap = await db.collection("events").get();
  console.log(`Nalezeno ${eventsSnap.size} eventů.\n`);

  if (eventsSnap.empty) {
    console.log("Žádné eventy ke smazání.");
    return;
  }

  for (const eventDoc of eventsSnap.docs) {
    console.log(`Mažu event: ${eventDoc.id} (${eventDoc.data().name || "bez názvu"})`);
    await deleteSubcollectionsRecursive(eventDoc.ref);
    await eventDoc.ref.delete();
    console.log(`  ✓ Event smazán.\n`);
  }

  // Also delete joinRequests related to events
  const joinRequestsSnap = await db.collection("joinRequests").get();
  if (!joinRequestsSnap.empty) {
    console.log(`Mažu ${joinRequestsSnap.size} joinRequests...`);
    const batch = db.batch();
    joinRequestsSnap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    console.log("✓ joinRequests smazány.\n");
  }

  console.log("Hotovo! Všechny eventy a jejich data byly smazány.");
}

main().catch(console.error);
