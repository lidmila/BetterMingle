const admin = require("firebase-admin");
const path = require("path");

const serviceAccount = require(path.join(process.env.USERPROFILE || process.env.HOME, "Documents/GitHub/bettermingle-firebase-adminsdk-fbsvc-2a0f1fb62e.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: "bettermingle",
});

const db = admin.firestore();

async function setBusinessTier() {
  const email = "lidmilamarsalkova@gmail.com";
  console.log(`Hledám uživatele s e-mailem: ${email}...`);

  const snapshot = await db.collection("users").where("email", "==", email).get();

  if (snapshot.empty) {
    console.log("Uživatel nenalezen v Firestore.");
    // Try Firebase Auth
    try {
      const authUser = await admin.auth().getUserByEmail(email);
      console.log(`Uživatel nalezen ve Firebase Auth: ${authUser.uid} (${authUser.displayName})`);
      console.log("Ale nemá dokument v kolekci users. Vytvářím...");

      const now = Date.now();
      const oneYearFromNow = now + 365 * 24 * 60 * 60 * 1000;

      await db.collection("users").doc(authUser.uid).set({
        email: email,
        displayName: authUser.displayName || "",
        isPremium: true,
        premiumTier: "BUSINESS",
        premiumUntil: admin.firestore.Timestamp.fromMillis(oneYearFromNow),
      }, { merge: true });

      console.log(`Vytvořen dokument a nastaven BUSINESS tier pro ${authUser.uid}`);
    } catch (e) {
      console.error("Uživatel nenalezen ani ve Firebase Auth:", e.message);
    }
    return;
  }

  snapshot.forEach(async (doc) => {
    const data = doc.data();
    console.log(`Nalezen uživatel: ${doc.id}`);
    console.log(`  Jméno: ${data.displayName}`);
    console.log(`  Aktuální tier: ${data.premiumTier || "FREE"}`);
    console.log(`  isPremium: ${data.isPremium || false}`);

    const now = Date.now();
    const oneYearFromNow = now + 365 * 24 * 60 * 60 * 1000;

    await db.collection("users").doc(doc.id).update({
      isPremium: true,
      premiumTier: "BUSINESS",
      premiumUntil: admin.firestore.Timestamp.fromMillis(oneYearFromNow),
    });

    console.log(`\nAktualizováno:`);
    console.log(`  isPremium: true`);
    console.log(`  premiumTier: BUSINESS`);
    console.log(`  premiumUntil: ${new Date(oneYearFromNow).toISOString()}`);
  });
}

setBusinessTier().then(() => {
  console.log("\nHotovo!");
  process.exit(0);
}).catch((err) => {
  console.error("Chyba:", err);
  process.exit(1);
});
