const admin = require("firebase-admin");
const path = require("path");

const serviceAccount = require(path.join(process.env.USERPROFILE || process.env.HOME, "Documents/GitHub/bettermingle-firebase-adminsdk-fbsvc-2a0f1fb62e.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: "bettermingle",
});

const db = admin.firestore();

const PRIMARY_USER = "REId2X8lUGXQIHImzlfJnx2poYu2";
const now = Date.now();
const day = 86400000;
const hr = 3600000;

async function seed() {
  console.log("Připojuji se k Firestore (bettermingle)...\n");

  // ═══ USERS ═══
  const users = {
    [PRIMARY_USER]: { displayName: "Lída Maršálková", email: "lidmilamarsalkova@gmail.com", avatarUrl: "https://lh3.googleusercontent.com/a/ACg8ocLm1SYlmeisUKobjBm5eQjk1REFoNhi3boZiVr8sRMnoz1ysySM=s96-c", phone: "+420 777 123 456", dietaryPreferences: ["vegetarián"], isPremium: true, premiumTier: "business", premiumUntil: admin.firestore.Timestamp.fromMillis(now + 365 * day), createdAt: admin.firestore.Timestamp.fromMillis(now - 60 * day) },
    mock_user_01: { displayName: "Jan Novák", email: "jan.novak@firma.cz", avatarUrl: "", phone: "+420 602 111 222", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 45 * day) },
    mock_user_02: { displayName: "Petra Svobodová", email: "petra.svobodova@firma.cz", avatarUrl: "", phone: "+420 603 333 444", dietaryPreferences: ["bezlepkové"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 40 * day) },
    mock_user_03: { displayName: "Martin Černý", email: "martin.cerny@firma.cz", avatarUrl: "", phone: "+420 604 555 666", dietaryPreferences: ["vegan"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 38 * day) },
    mock_user_04: { displayName: "Tereza Králová", email: "tereza.kralova@firma.cz", avatarUrl: "", phone: "+420 605 777 888", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 35 * day) },
    mock_user_05: { displayName: "Lukáš Procházka", email: "lukas.prochazka@firma.cz", avatarUrl: "", phone: "+420 606 999 000", dietaryPreferences: ["bezlaktózové"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 30 * day) },
    mock_user_06: { displayName: "Karolína Veselá", email: "karolina.vesela@firma.cz", avatarUrl: "", phone: "+420 607 111 333", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 28 * day) },
    mock_user_07: { displayName: "David Kučera", email: "david.kucera@firma.cz", avatarUrl: "", phone: "+420 608 444 555", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 25 * day) },
  };
  const allUsers = Object.keys(users);
  const mockUserIds = allUsers.filter(id => id !== PRIMARY_USER);

  for (const [uid, data] of Object.entries(users)) {
    await db.collection("users").doc(uid).set(data, { merge: true });
  }
  console.log("✅ 8 uživatelů");

  // ═══ EVENTS ═══
  const event1Id = "event_teambuilding_2026";
  const event1 = {
    createdBy: PRIMARY_USER, name: "Firemní teambuilding 2026",
    description: "Třídenní teambuilding v Českém ráji. Sport, workshopy a večerní program.",
    theme: "teambuilding", templateSlug: "teambuilding",
    locationName: "Hotel Nová Perla, Český ráj", locationLat: 50.5167, locationLng: 15.1833,
    locationAddress: "Turnov 511 01, Český ráj",
    startDate: now + 14 * day, endDate: now + 16 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "TB2026", maxParticipants: 50, status: "CONFIRMED",
    enabledModules: ["VOTING","EXPENSES","CARPOOL","ROOMS","PACKING","CHAT","SCHEDULE","GALLERY"],
    securityEnabled: true, eventPin: "", hideFinancials: false, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: false, createdAt: now - 20 * day, updatedAt: now - day,
  };

  const event2Id = "event_narozeniny_petra";
  const event2 = {
    createdBy: PRIMARY_USER, name: "Narozeniny Petry",
    description: "Překvapení pro Petru! Střecha baru Výtopna v centru Prahy.",
    theme: "oslava", templateSlug: "oslava",
    locationName: "Bar Výtopna, Praha 1", locationLat: 50.0833, locationLng: 14.4167,
    locationAddress: "Na příkopě 15, Praha 1",
    startDate: now + 7 * day, endDate: now + 7 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "PB30TH", maxParticipants: 20, status: "CONFIRMED",
    enabledModules: ["VOTING","EXPENSES","CHAT","GALLERY","PACKING"],
    securityEnabled: false, createdAt: now - 10 * day, updatedAt: now - 2 * day,
  };

  const event3Id = "event_vikend_chata";
  const event3 = {
    createdBy: "mock_user_01", name: "Víkend na chatě",
    description: "Relax, grilovačka a výlet do přírody.",
    theme: "výlet", templateSlug: "vylet",
    locationName: "Chata U Lesa, Šumava", locationLat: 49.0, locationLng: 13.5,
    locationAddress: "Kašperské Hory, Šumava",
    startDate: now - 10 * day, endDate: now - 8 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "CHATA1", maxParticipants: 10, status: "COMPLETED",
    enabledModules: ["EXPENSES","CARPOOL","ROOMS","CHAT","GALLERY"],
    securityEnabled: false, createdAt: now - 30 * day, updatedAt: now - 8 * day,
  };

  // ═══ NEW EVENTS (7 more) ═══
  const event4Id = "event_svatba_karolina_david";
  const event4 = {
    createdBy: PRIMARY_USER, name: "Svatba Karolíny a Davida",
    description: "Svatební obřad a hostina v zámeckém areálu. Dresscode: společenský.",
    theme: "svatba", templateSlug: "svatba",
    locationName: "Zámek Karlštejn", locationLat: 49.9392, locationLng: 14.1883,
    locationAddress: "Karlštejn 172, 267 18 Karlštejn",
    startDate: now + 60 * day, endDate: now + 60 * day + 12*hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "SVATBA", maxParticipants: 80, status: "CONFIRMED",
    enabledModules: ["VOTING","EXPENSES","CARPOOL","ROOMS","PACKING","CHAT","SCHEDULE","GALLERY"],
    securityEnabled: true, eventPin: "", hideFinancials: true, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: true, createdAt: now - 45 * day, updatedAt: now - 2 * day,
  };

  const event5Id = "event_firemni_vanocni_vecirek";
  const event5 = {
    createdBy: PRIMARY_USER, name: "Firemní vánoční večírek",
    description: "Vánoční večírek s programem, tombola a živá hudba.",
    theme: "firemní", templateSlug: "firemni",
    locationName: "Restaurace U Fleků, Praha", locationLat: 50.0792, locationLng: 14.4181,
    locationAddress: "Křemencova 11, 110 00 Praha 1",
    startDate: now + 30 * day, endDate: now + 30 * day + 6*hr, datesFinalized: false,
    coverImageUrl: "", inviteCode: "XMAS26", maxParticipants: 40, status: "PLANNING",
    enabledModules: ["VOTING","EXPENSES","CHAT","GALLERY"],
    securityEnabled: false, createdAt: now - 5 * day, updatedAt: now - day,
  };

  const event6Id = "event_roadtrip_chorvatsko";
  const event6 = {
    createdBy: "mock_user_03", name: "Road trip Chorvatsko",
    description: "Týdenní road trip po chorvatském pobřeží. Split → Dubrovník → ostrovy.",
    theme: "výlet", templateSlug: "vylet",
    locationName: "Split, Chorvatsko", locationLat: 43.5081, locationLng: 16.4402,
    locationAddress: "Split, Splitsko-dalmatská župa, Chorvatsko",
    startDate: now + 21 * day, endDate: now + 28 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "CRTRIP", maxParticipants: 8, status: "CONFIRMED",
    enabledModules: ["EXPENSES","CARPOOL","CHAT","GALLERY","SCHEDULE","PACKING"],
    securityEnabled: false, createdAt: now - 15 * day, updatedAt: now - 3 * day,
  };

  const event7Id = "event_lukasovy_30_narozeniny";
  const event7 = {
    createdBy: "mock_user_05", name: "Lukášovy 30. narozeniny",
    description: "Velká třicítka! Bowlingové centrum + afterparty v baru.",
    theme: "oslava", templateSlug: "oslava",
    locationName: "Bowling Praha Chodov", locationLat: 50.0310, locationLng: 14.4925,
    locationAddress: "Roztylská 2321/19, 148 00 Praha 11",
    startDate: now + 10 * day, endDate: now + 10 * day + 8*hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "LUK30", maxParticipants: 25, status: "CONFIRMED",
    enabledModules: ["VOTING","EXPENSES","CHAT","GALLERY"],
    securityEnabled: false, createdAt: now - 12 * day, updatedAt: now - 4 * day,
  };

  const event8Id = "event_piknik_stromovka";
  const event8 = {
    createdBy: "mock_user_02", name: "Piknik ve Stromovce",
    description: "Pohodový piknik v parku. Každý přinese něco k jídlu a pití.",
    theme: "piknik", templateSlug: "piknik",
    locationName: "Stromovka, Praha 7", locationLat: 50.1050, locationLng: 14.4167,
    locationAddress: "Královská obora, 170 00 Praha 7",
    startDate: now + 3 * day, endDate: now + 3 * day + 5*hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "PIKNIK", maxParticipants: 15, status: "CONFIRMED",
    enabledModules: ["CHAT","GALLERY","PACKING"],
    securityEnabled: false, createdAt: now - 7 * day, updatedAt: now - 2 * day,
  };

  const event9Id = "event_rozlucka_se_svobodou";
  const event9 = {
    createdBy: "mock_user_06", name: "Rozlučka se svobodou – Karolína",
    description: "Surprise rozlučka pro nevěstu! Wellness + večerní program.",
    theme: "rozlučka", templateSlug: "rozlucka",
    locationName: "Wellness Hotel Svornost, Harrachov", locationLat: 50.7731, locationLng: 15.4317,
    locationAddress: "Harrachov 446, 512 46 Harrachov",
    startDate: now + 35 * day, endDate: now + 36 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "ROZLUC", maxParticipants: 12, status: "CONFIRMED",
    enabledModules: ["VOTING","EXPENSES","CARPOOL","ROOMS","CHAT","SCHEDULE","GALLERY"],
    securityEnabled: true, eventPin: "1234", hideFinancials: false, screenshotProtection: true,
    autoDeleteDays: 30, requireApproval: true, createdAt: now - 20 * day, updatedAt: now - 5 * day,
  };

  const event10Id = "event_grilovacka_novaku";
  const event10 = {
    createdBy: "mock_user_01", name: "Grilovačka u Nováků",
    description: "Letní grilovačka na zahradě. Steaky, klobásy, pivo.",
    theme: "grilovačka", templateSlug: "grilovacka",
    locationName: "Zahrada Novákových, Říčany", locationLat: 49.9908, locationLng: 14.6564,
    locationAddress: "Zahradní 15, 251 01 Říčany",
    startDate: now - 30 * day, endDate: now - 30 * day + 8*hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "GRIL26", maxParticipants: 20, status: "COMPLETED",
    enabledModules: ["EXPENSES","CHAT","GALLERY","PACKING"],
    securityEnabled: false, createdAt: now - 50 * day, updatedAt: now - 29 * day,
  };

  await db.collection("events").doc(event1Id).set(event1);
  await db.collection("events").doc(event2Id).set(event2);
  await db.collection("events").doc(event3Id).set(event3);
  await db.collection("events").doc(event4Id).set(event4);
  await db.collection("events").doc(event5Id).set(event5);
  await db.collection("events").doc(event6Id).set(event6);
  await db.collection("events").doc(event7Id).set(event7);
  await db.collection("events").doc(event8Id).set(event8);
  await db.collection("events").doc(event9Id).set(event9);
  await db.collection("events").doc(event10Id).set(event10);
  console.log("✅ 10 akcí");

  // ═══ PARTICIPANTS ═══
  for (const uid of allUsers) {
    await db.collection("events").doc(event1Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 18 * day,
    });
  }
  const e2users = [PRIMARY_USER, "mock_user_01", "mock_user_03", "mock_user_05", "mock_user_06"];
  for (const uid of e2users) {
    await db.collection("events").doc(event2Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: uid === "mock_user_03" ? "maybe" : "accepted", joinedAt: now - 8 * day,
    });
  }
  const e3users = [PRIMARY_USER, "mock_user_01", "mock_user_03", "mock_user_05"];
  for (const uid of e3users) {
    await db.collection("events").doc(event3Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_01" ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 25 * day,
    });
  }
  // Event 4 – Svatba (all users)
  for (const uid of allUsers) {
    await db.collection("events").doc(event4Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: uid === "mock_user_04" ? "maybe" : "accepted", joinedAt: now - 40 * day,
    });
  }
  // Event 5 – Vánoční večírek
  const e5users = [PRIMARY_USER, "mock_user_01", "mock_user_02", "mock_user_04", "mock_user_05", "mock_user_07"];
  for (const uid of e5users) {
    await db.collection("events").doc(event5Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: uid === "mock_user_07" ? "pending" : "accepted", joinedAt: now - 4 * day,
    });
  }
  // Event 6 – Road trip Chorvatsko
  const e6users = [PRIMARY_USER, "mock_user_03", "mock_user_01", "mock_user_06"];
  for (const uid of e6users) {
    await db.collection("events").doc(event6Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_03" ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 12 * day,
    });
  }
  // Event 7 – Lukášovy 30. narozeniny
  const e7users = [PRIMARY_USER, "mock_user_05", "mock_user_01", "mock_user_02", "mock_user_03", "mock_user_06", "mock_user_07"];
  for (const uid of e7users) {
    await db.collection("events").doc(event7Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_05" ? "organizer" : "participant",
      rsvp: uid === "mock_user_07" ? "maybe" : "accepted", joinedAt: now - 10 * day,
    });
  }
  // Event 8 – Piknik ve Stromovce
  const e8users = [PRIMARY_USER, "mock_user_02", "mock_user_04", "mock_user_05", "mock_user_06"];
  for (const uid of e8users) {
    await db.collection("events").doc(event8Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_02" ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 5 * day,
    });
  }
  // Event 9 – Rozlučka se svobodou
  const e9users = [PRIMARY_USER, "mock_user_06", "mock_user_02", "mock_user_04"];
  for (const uid of e9users) {
    await db.collection("events").doc(event9Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_06" ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 18 * day,
    });
  }
  // Event 10 – Grilovačka u Nováků
  const e10users = [PRIMARY_USER, "mock_user_01", "mock_user_02", "mock_user_03", "mock_user_05"];
  for (const uid of e10users) {
    await db.collection("events").doc(event10Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === "mock_user_01" ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 45 * day,
    });
  }
  console.log("✅ Účastníci (všechny akce)");

  // ═══ POLLS ═══
  await db.collection("events").doc(event1Id).collection("polls").doc("poll_activity").set({
    createdBy: PRIMARY_USER, title: "Jakou hlavní aktivitu chcete?", pollType: "activity",
    allowMultiple: true, isAnonymous: false, deadline: now + 7 * day, isClosed: false, createdAt: now - 15 * day,
  });
  const opts1 = [
    { id: "opt_a1", label: "Rafting na Jizeře", description: "3h jízda na raftu", sortOrder: 0 },
    { id: "opt_a2", label: "Via ferrata Český ráj", description: "Zajištěná cesta po skalách", sortOrder: 1 },
    { id: "opt_a3", label: "Paintball", description: "Týmová střílečka v lese", sortOrder: 2 },
    { id: "opt_a4", label: "Workshop: týmová komunikace", description: "2h facilitovaný workshop", sortOrder: 3 },
  ];
  for (const o of opts1) await db.collection("events").doc(event1Id).collection("polls").doc("poll_activity").collection("options").doc(o.id).set(o);
  const votes1 = [
    ["opt_a1","v1",PRIMARY_USER],["opt_a2","v2",PRIMARY_USER],["opt_a1","v3","mock_user_01"],
    ["opt_a3","v4","mock_user_01"],["opt_a2","v5","mock_user_02"],["opt_a4","v6","mock_user_02"],
    ["opt_a1","v7","mock_user_03"],["opt_a2","v8","mock_user_04"],["opt_a3","v9","mock_user_05"],
  ];
  for (const [optId,vId,uid] of votes1) await db.collection("events").doc(event1Id).collection("polls").doc("poll_activity").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });

  await db.collection("events").doc(event1Id).collection("polls").doc("poll_food").set({
    createdBy: "mock_user_02", title: "Večeře první den – co preferujete?", pollType: "custom",
    allowMultiple: false, isAnonymous: false, deadline: now + 5 * day, isClosed: false, createdAt: now - 10 * day,
  });
  const opts2 = [
    { id: "opt_f1", label: "Grilování venku", description: "Steaky, klobásky, zelenina", sortOrder: 0 },
    { id: "opt_f2", label: "Cateringový bufet", description: "Teplý i studený bufet", sortOrder: 1 },
    { id: "opt_f3", label: "Pizza party", description: "Objednávka z místní pizzerie", sortOrder: 2 },
  ];
  for (const o of opts2) await db.collection("events").doc(event1Id).collection("polls").doc("poll_food").collection("options").doc(o.id).set(o);
  for (const [optId,vId,uid] of [["opt_f1","vf1",PRIMARY_USER],["opt_f1","vf2","mock_user_01"],["opt_f2","vf3","mock_user_03"],["opt_f1","vf4","mock_user_04"],["opt_f3","vf5","mock_user_05"],["opt_f1","vf6","mock_user_06"]]) {
    await db.collection("events").doc(event1Id).collection("polls").doc("poll_food").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }
  console.log("✅ 2 ankety + hlasy");

  // ═══ EXPENSES ═══
  const expenses = [
    { eid: event1Id, id: "exp_01", d: { paidBy: PRIMARY_USER, description: "Záloha hotel (3 noci)", amount: 45000, currency: "CZK", category: "ubytování", receiptUrl: "", createdAt: now - 15 * day }, users: allUsers, perPerson: 5625 },
    { eid: event1Id, id: "exp_02", d: { paidBy: "mock_user_01", description: "Nákup na grilování", amount: 3200, currency: "CZK", category: "jídlo", receiptUrl: "", createdAt: now - 5 * day }, users: allUsers, perPerson: 400 },
    { eid: event1Id, id: "exp_03", d: { paidBy: "mock_user_03", description: "Rafting – vstupné", amount: 6400, currency: "CZK", category: "aktivity", receiptUrl: "", createdAt: now - 3 * day }, users: allUsers, perPerson: 800 },
    { eid: event2Id, id: "exp_04", d: { paidBy: PRIMARY_USER, description: "Rezervace VIP zóny", amount: 5000, currency: "CZK", category: "pronájem", receiptUrl: "", createdAt: now - 5 * day }, users: e2users, perPerson: 1000 },
    { eid: event2Id, id: "exp_05", d: { paidBy: "mock_user_05", description: "Dort a dekorace", amount: 1800, currency: "CZK", category: "jídlo", receiptUrl: "", createdAt: now - 3 * day }, users: e2users, perPerson: 360 },
  ];
  for (const e of expenses) {
    await db.collection("events").doc(e.eid).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e.users.length; i++) {
      await db.collection("events").doc(e.eid).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e.users[i], amount: e.perPerson, isSettled: false });
    }
  }
  console.log("✅ 5 výdajů + splity");

  // ═══ CARPOOL ═══
  const rides = [
    { id: "ride_01", d: { driverId: PRIMARY_USER, departureLocation: "Praha, Chodov", departureLat: 50.03, departureLng: 14.49, departureTime: event1.startDate - 3*hr, availableSeats: 4, notes: "Odjezd v 7:00, kufr velký" }, pass: [{ id:"p01", userId:"mock_user_01", status:"approved", pickupLocation:"Praha, Florenc" }, { id:"p02", userId:"mock_user_02", status:"approved", pickupLocation:"Praha, Černý Most" }] },
    { id: "ride_02", d: { driverId: "mock_user_03", departureLocation: "Brno, centrum", departureLat: 49.20, departureLng: 16.61, departureTime: event1.startDate - 4*hr, availableSeats: 3, notes: "Jedu přes Hradec Králové" }, pass: [{ id:"p03", userId:"mock_user_04", status:"approved", pickupLocation:"Brno, Královo Pole" }, { id:"p04", userId:"mock_user_07", status:"pending", pickupLocation:"Hradec Králové, nádraží" }] },
    { id: "ride_03", d: { driverId: "mock_user_05", departureLocation: "Liberec, centrum", departureLat: 50.77, departureLng: 15.06, departureTime: event1.startDate - 2*hr, availableSeats: 2, notes: "Blízko, jedu na poslední chvíli" }, pass: [{ id:"p05", userId:"mock_user_06", status:"approved", pickupLocation:"Liberec, Perštýn" }] },
  ];
  for (const r of rides) {
    await db.collection("events").doc(event1Id).collection("carpoolRides").doc(r.id).set(r.d);
    for (const p of r.pass) await db.collection("events").doc(event1Id).collection("carpoolRides").doc(r.id).collection("passengers").doc(p.id).set({ userId: p.userId, status: p.status, pickupLocation: p.pickupLocation });
  }
  console.log("✅ 3 spolujízdy");

  // ═══ ROOMS ═══
  const rooms = [
    { id: "room_01", name: "Pokoj 101 – Dvojlůžkový", capacity: 2, notes: "Výhled na skály, balkón", assignments: [PRIMARY_USER, "mock_user_01"] },
    { id: "room_02", name: "Pokoj 102 – Dvojlůžkový", capacity: 2, notes: "Přízemí, bezbariérový", assignments: ["mock_user_02", "mock_user_04"] },
    { id: "room_03", name: "Pokoj 103 – Trojlůžkový", capacity: 3, notes: "Velký pokoj s přistýlkou", assignments: ["mock_user_03", "mock_user_05", "mock_user_06"] },
    { id: "room_04", name: "Pokoj 104 – Jednolůžkový", capacity: 1, notes: "Tichý pokoj v podkroví", assignments: ["mock_user_07"] },
  ];
  for (const r of rooms) await db.collection("events").doc(event1Id).collection("rooms").doc(r.id).set(r);
  console.log("✅ 4 pokoje");

  // ═══ SCHEDULE ═══
  const schedule = [
    { id: "sch_01", title: "Příjezd a ubytování", description: "Check-in na recepci", startTime: event1.startDate, endTime: event1.startDate + 2*hr, location: "Recepce hotelu" },
    { id: "sch_02", title: "Oběd", description: "Společný oběd v restauraci", startTime: event1.startDate + 3*hr, endTime: event1.startDate + 4*hr, location: "Restaurace" },
    { id: "sch_03", title: "Rafting na Jizeře", description: "Skupinový rafting, vybavení na místě", startTime: event1.startDate + 5*hr, endTime: event1.startDate + 8*hr, location: "Jizera – start u mostu" },
    { id: "sch_04", title: "Grilování a večerní program", description: "Gril, hudba, táborák", startTime: event1.startDate + 10*hr, endTime: event1.startDate + 14*hr, location: "Hotelová zahrada" },
    { id: "sch_05", title: "Workshop: týmová komunikace", description: "Facilitovaný workshop", startTime: event1.startDate + day + 3*hr, endTime: event1.startDate + day + 5*hr, location: "Konferenční sál" },
    { id: "sch_06", title: "Via ferrata Český ráj", description: "Zajištěná cesta, helmy k zapůjčení", startTime: event1.startDate + day + 6*hr, endTime: event1.startDate + day + 9*hr, location: "Hruboskalsko" },
  ];
  for (const s of schedule) await db.collection("events").doc(event1Id).collection("schedule").doc(s.id).set(s);
  console.log("✅ 6 bodů harmonogramu");

  // ═══ PACKING ═══
  const packing = [
    { id: "pack_01", name: "Plavky", isChecked: true, userId: null, addedBy: PRIMARY_USER },
    { id: "pack_02", name: "Opalovací krém", isChecked: false, userId: null, addedBy: PRIMARY_USER },
    { id: "pack_03", name: "Trekové boty", isChecked: true, userId: null, addedBy: "mock_user_03" },
    { id: "pack_04", name: "Karimatka", isChecked: false, userId: null, addedBy: "mock_user_01" },
    { id: "pack_05", name: "Přezůvky do hotelu", isChecked: false, userId: null, addedBy: PRIMARY_USER },
    { id: "pack_06", name: "Repelent", isChecked: true, userId: null, addedBy: "mock_user_02" },
    { id: "pack_07", name: "Dobrá nálada", isChecked: true, userId: null, addedBy: "mock_user_06" },
    { id: "pack_08", name: "Powerbank", isChecked: false, userId: PRIMARY_USER, addedBy: PRIMARY_USER },
    { id: "pack_09", name: "Foťák", isChecked: true, userId: PRIMARY_USER, addedBy: PRIMARY_USER },
    { id: "pack_10", name: "Spacák", isChecked: false, userId: "mock_user_01", addedBy: "mock_user_01" },
    { id: "pack_11", name: "Svítilna", isChecked: true, userId: "mock_user_05", addedBy: "mock_user_05" },
    { id: "pack_12", name: "Kytara", isChecked: false, userId: "mock_user_07", addedBy: "mock_user_07" },
  ];
  for (const p of packing) await db.collection("events").doc(event1Id).collection("packingItems").doc(p.id).set(p);
  console.log("✅ 12 balicích položek");

  // ═══ CHAT ═══
  const msgs = [
    { id: "msg_01", userId: PRIMARY_USER, content: "Ahoj všichni! Teambuilding se blíží. Nezapomeňte hlasovat v anketě.", replyTo: null, createdAt: now - 14*day },
    { id: "msg_02", userId: "mock_user_01", content: "Super, už se těším! Jedeme rafting?", replyTo: null, createdAt: now - 14*day + 1800000 },
    { id: "msg_03", userId: "mock_user_02", content: "Já bych radši feratu, ale rafting taky ok", replyTo: "msg_02", createdAt: now - 14*day + 2700000 },
    { id: "msg_04", userId: "mock_user_03", content: "Zaplatil jsem rafting za všechny, hodil jsem to do výdajů", replyTo: null, createdAt: now - 3*day },
    { id: "msg_05", userId: PRIMARY_USER, content: "Díky Martine!", replyTo: "msg_04", createdAt: now - 3*day + 600000 },
    { id: "msg_06", userId: "mock_user_05", content: "Kdo jede z Liberce? Mám 2 volná místa v autě", replyTo: null, createdAt: now - 2*day },
    { id: "msg_07", userId: "mock_user_06", content: "Já! Přidej mě prosím", replyTo: "msg_06", createdAt: now - 2*day + 900000 },
    { id: "msg_08", userId: "mock_user_04", content: "Nezapomeňte na trekové boty, budeme hodně v terénu", replyTo: null, createdAt: now - day },
    { id: "msg_09", userId: "mock_user_07", content: "Vezmu kytaru na večer, někdo další hraje?", replyTo: null, createdAt: now - 12*hr },
    { id: "msg_10", userId: PRIMARY_USER, content: "Aktualizovala jsem harmonogram, mrknětě na něj", replyTo: null, createdAt: now - 6*hr },
  ];
  for (const m of msgs) await db.collection("events").doc(event1Id).collection("messages").doc(m.id).set(m);
  console.log("✅ 10 zpráv");

  // ═══ POLLS for new events ═══
  // Event 4 – Svatba: menu preference
  await db.collection("events").doc(event4Id).collection("polls").doc("poll_svatba_menu").set({
    createdBy: PRIMARY_USER, title: "Jaké hlavní jídlo preferujete?", pollType: "custom",
    allowMultiple: false, isAnonymous: false, deadline: now + 30 * day, isClosed: false, createdAt: now - 30 * day,
  });
  const optsMenu = [
    { id: "opt_m1", label: "Svíčková na smetaně", description: "Klasická česká", sortOrder: 0 },
    { id: "opt_m2", label: "Kuřecí supreme", description: "S grilovanou zeleninou", sortOrder: 1 },
    { id: "opt_m3", label: "Vegetariánské risotto", description: "Houbové risotto s parmezánem", sortOrder: 2 },
  ];
  for (const o of optsMenu) await db.collection("events").doc(event4Id).collection("polls").doc("poll_svatba_menu").collection("options").doc(o.id).set(o);
  for (const [optId,vId,uid] of [["opt_m1","vm1",PRIMARY_USER],["opt_m2","vm2","mock_user_01"],["opt_m1","vm3","mock_user_02"],["opt_m3","vm4","mock_user_03"],["opt_m2","vm5","mock_user_05"]]) {
    await db.collection("events").doc(event4Id).collection("polls").doc("poll_svatba_menu").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // Event 5 – Vánoční večírek: místo konání
  await db.collection("events").doc(event5Id).collection("polls").doc("poll_xmas_venue").set({
    createdBy: PRIMARY_USER, title: "Kam na vánoční večírek?", pollType: "custom",
    allowMultiple: false, isAnonymous: false, deadline: now + 15 * day, isClosed: false, createdAt: now - 4 * day,
  });
  const optsXmas = [
    { id: "opt_x1", label: "U Fleků", description: "Klasická restaurace, velký sál", sortOrder: 0 },
    { id: "opt_x2", label: "Manifesto Market", description: "Moderní food hall s bary", sortOrder: 1 },
    { id: "opt_x3", label: "Střecha Lucerny", description: "Rooftop bar s výhledem", sortOrder: 2 },
  ];
  for (const o of optsXmas) await db.collection("events").doc(event5Id).collection("polls").doc("poll_xmas_venue").collection("options").doc(o.id).set(o);

  // Event 7 – Lukášovy narozeniny: dárek
  await db.collection("events").doc(event7Id).collection("polls").doc("poll_lukas_gift").set({
    createdBy: "mock_user_01", title: "Společný dárek pro Lukáše?", pollType: "custom",
    allowMultiple: true, isAnonymous: false, deadline: now + 8 * day, isClosed: false, createdAt: now - 8 * day,
  });
  const optsGift = [
    { id: "opt_g1", label: "Zážitkový let balonem", description: "cca 3000 Kč/os", sortOrder: 0 },
    { id: "opt_g2", label: "PlayStation 5", description: "Složit se na konzoli", sortOrder: 1 },
    { id: "opt_g3", label: "Whisky degustace", description: "Privátní degustace pro skupinu", sortOrder: 2 },
  ];
  for (const o of optsGift) await db.collection("events").doc(event7Id).collection("polls").doc("poll_lukas_gift").collection("options").doc(o.id).set(o);
  for (const [optId,vId,uid] of [["opt_g2","vg1",PRIMARY_USER],["opt_g2","vg2","mock_user_01"],["opt_g3","vg3","mock_user_02"],["opt_g2","vg4","mock_user_06"]]) {
    await db.collection("events").doc(event7Id).collection("polls").doc("poll_lukas_gift").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }
  console.log("✅ 3 nové ankety + hlasy");

  // ═══ EXPENSES for new events ═══
  const newExpenses = [
    { eid: event4Id, id: "exp_06", d: { paidBy: PRIMARY_USER, description: "Záloha – svatební hostina", amount: 85000, currency: "CZK", category: "catering", receiptUrl: "", createdAt: now - 35 * day }, users: [PRIMARY_USER, "mock_user_07"], perPerson: 42500 },
    { eid: event4Id, id: "exp_07", d: { paidBy: PRIMARY_USER, description: "Květinová výzdoba", amount: 12000, currency: "CZK", category: "dekorace", receiptUrl: "", createdAt: now - 20 * day }, users: [PRIMARY_USER, "mock_user_07"], perPerson: 6000 },
    { eid: event6Id, id: "exp_08", d: { paidBy: "mock_user_03", description: "Pronájem auta (7 dní)", amount: 8400, currency: "CZK", category: "doprava", receiptUrl: "", createdAt: now - 10 * day }, users: e6users, perPerson: 2100 },
    { eid: event6Id, id: "exp_09", d: { paidBy: PRIMARY_USER, description: "Airbnb Split (2 noci)", amount: 4800, currency: "CZK", category: "ubytování", receiptUrl: "", createdAt: now - 8 * day }, users: e6users, perPerson: 1200 },
    { eid: event7Id, id: "exp_10", d: { paidBy: "mock_user_01", description: "Dárek – PS5", amount: 14990, currency: "CZK", category: "dárek", receiptUrl: "", createdAt: now - 5 * day }, users: [PRIMARY_USER,"mock_user_01","mock_user_02","mock_user_03","mock_user_06","mock_user_07"], perPerson: 2498 },
    { eid: event9Id, id: "exp_11", d: { paidBy: "mock_user_06", description: "Wellness balíček (4 osoby)", amount: 9600, currency: "CZK", category: "aktivity", receiptUrl: "", createdAt: now - 15 * day }, users: e9users, perPerson: 2400 },
    { eid: event10Id, id: "exp_12", d: { paidBy: "mock_user_01", description: "Maso a klobásy na gril", amount: 2800, currency: "CZK", category: "jídlo", receiptUrl: "", createdAt: now - 35 * day }, users: e10users, perPerson: 560 },
    { eid: event10Id, id: "exp_13", d: { paidBy: "mock_user_05", description: "Nápoje a pivo", amount: 1500, currency: "CZK", category: "nápoje", receiptUrl: "", createdAt: now - 33 * day }, users: e10users, perPerson: 300 },
  ];
  for (const e of newExpenses) {
    await db.collection("events").doc(e.eid).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e.users.length; i++) {
      await db.collection("events").doc(e.eid).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e.users[i], amount: e.perPerson, isSettled: e.eid === event10Id });
    }
  }
  console.log("✅ 8 nových výdajů + splity");

  // ═══ MESSAGES for new events ═══
  const newMsgs = [
    // Event 4 – Svatba
    { eid: event4Id, id: "msg_s01", userId: PRIMARY_USER, content: "Ahoj všichni! Děkujeme za potvrzení účasti na svatbě.", replyTo: null, createdAt: now - 30*day },
    { eid: event4Id, id: "msg_s02", userId: "mock_user_06", content: "Nemůžu se dočkat! Bude to krásné ❤️", replyTo: null, createdAt: now - 29*day },
    { eid: event4Id, id: "msg_s03", userId: "mock_user_01", content: "Potřebuji poradit s ubytováním, je tam něco blízko?", replyTo: null, createdAt: now - 25*day },
    // Event 6 – Road trip
    { eid: event6Id, id: "msg_r01", userId: "mock_user_03", content: "Trasa: Split → Hvar → Korčula → Dubrovník. Co říkáte?", replyTo: null, createdAt: now - 12*day },
    { eid: event6Id, id: "msg_r02", userId: PRIMARY_USER, content: "Super! Já bych přidala Brač, je tam krásná pláž Zlatni rat", replyTo: "msg_r01", createdAt: now - 11*day },
    { eid: event6Id, id: "msg_r03", userId: "mock_user_01", content: "Pronájem auta je zařízen, mám Škodu Octavia", replyTo: null, createdAt: now - 8*day },
    // Event 7 – Narozeniny
    { eid: event7Id, id: "msg_n01", userId: "mock_user_01", content: "Lukáš nic netuší, držte to prosím v tajnosti!", replyTo: null, createdAt: now - 9*day },
    { eid: event7Id, id: "msg_n02", userId: PRIMARY_USER, content: "Jasně! Rezervace bowlingu je na 18:00", replyTo: null, createdAt: now - 8*day },
    // Event 8 – Piknik
    { eid: event8Id, id: "msg_p01", userId: "mock_user_02", content: "Přinesu deku a hummus, kdo přinese víno?", replyTo: null, createdAt: now - 4*day },
    { eid: event8Id, id: "msg_p02", userId: PRIMARY_USER, content: "Já vezmu prosecco a ovoce 🍓", replyTo: "msg_p01", createdAt: now - 3*day },
    // Event 9 – Rozlučka
    { eid: event9Id, id: "msg_rz01", userId: "mock_user_06", content: "Karolína nesmí nic vědět! Komunikujte jen tady.", replyTo: null, createdAt: now - 17*day },
    { eid: event9Id, id: "msg_rz02", userId: PRIMARY_USER, content: "Mám pro ni korunku a šerpu, bude to super", replyTo: null, createdAt: now - 15*day },
  ];
  for (const m of newMsgs) {
    await db.collection("events").doc(m.eid).collection("messages").doc(m.id).set({ userId: m.userId, content: m.content, replyTo: m.replyTo, createdAt: m.createdAt });
  }
  console.log("✅ 12 nových zpráv");

  // ═══ SCHEDULE for event 4 (svatba) ═══
  const svatbaSchedule = [
    { id: "sch_s01", title: "Obřad", description: "Svatební obřad v zámecké kapli", startTime: event4.startDate, endTime: event4.startDate + hr, location: "Zámecká kaple" },
    { id: "sch_s02", title: "Focení", description: "Skupinové a párové fotky v zahradě", startTime: event4.startDate + hr, endTime: event4.startDate + 2*hr, location: "Zámecká zahrada" },
    { id: "sch_s03", title: "Přípitek a předkrmy", description: "Šampaňské a kanapky", startTime: event4.startDate + 2*hr, endTime: event4.startDate + 3*hr, location: "Terasový sál" },
    { id: "sch_s04", title: "Svatební hostina", description: "Hlavní chod + dezert", startTime: event4.startDate + 3*hr, endTime: event4.startDate + 6*hr, location: "Hlavní sál" },
    { id: "sch_s05", title: "První tanec a party", description: "Živá kapela + DJ", startTime: event4.startDate + 6*hr, endTime: event4.startDate + 12*hr, location: "Taneční sál" },
  ];
  for (const s of svatbaSchedule) await db.collection("events").doc(event4Id).collection("schedule").doc(s.id).set(s);

  // ═══ SCHEDULE for event 9 (rozlučka) ═══
  const rozluckaSchedule = [
    { id: "sch_rz01", title: "Příjezd a ubytování", description: "Check-in do hotelu", startTime: event9.startDate, endTime: event9.startDate + hr, location: "Recepce" },
    { id: "sch_rz02", title: "Wellness a bazén", description: "Masáže, sauna, whirlpool", startTime: event9.startDate + 2*hr, endTime: event9.startDate + 5*hr, location: "Wellness centrum" },
    { id: "sch_rz03", title: "Večeře a hry", description: "Společná večeře + rozlučkové hry", startTime: event9.startDate + 7*hr, endTime: event9.startDate + 11*hr, location: "Privátní salónek" },
  ];
  for (const s of rozluckaSchedule) await db.collection("events").doc(event9Id).collection("schedule").doc(s.id).set(s);
  console.log("✅ 8 nových bodů harmonogramu");

  // ═══ PACKING for event 6 (road trip) ═══
  const roadtripPacking = [
    { id: "pack_rt01", name: "Pas", isChecked: false, userId: null, addedBy: "mock_user_03" },
    { id: "pack_rt02", name: "Plavky", isChecked: true, userId: null, addedBy: "mock_user_03" },
    { id: "pack_rt03", name: "Sluneční brýle", isChecked: false, userId: null, addedBy: PRIMARY_USER },
    { id: "pack_rt04", name: "Opalovací krém SPF50", isChecked: false, userId: null, addedBy: PRIMARY_USER },
    { id: "pack_rt05", name: "Šnorchl a maska", isChecked: false, userId: "mock_user_01", addedBy: "mock_user_01" },
    { id: "pack_rt06", name: "Nabíječka do auta", isChecked: true, userId: "mock_user_03", addedBy: "mock_user_03" },
  ];
  for (const p of roadtripPacking) await db.collection("events").doc(event6Id).collection("packingItems").doc(p.id).set(p);

  // ═══ CARPOOL for event 9 (rozlučka) ═══
  await db.collection("events").doc(event9Id).collection("carpoolRides").doc("ride_rz01").set({
    driverId: PRIMARY_USER, departureLocation: "Praha, Chodov", departureLat: 50.03, departureLng: 14.49,
    departureTime: event9.startDate - 3*hr, availableSeats: 3, notes: "Odjezd ráno, klidná jízda",
  });
  await db.collection("events").doc(event9Id).collection("carpoolRides").doc("ride_rz01").collection("passengers").doc("p_rz01").set({ userId: "mock_user_02", status: "approved", pickupLocation: "Praha, Anděl" });
  await db.collection("events").doc(event9Id).collection("carpoolRides").doc("ride_rz01").collection("passengers").doc("p_rz02").set({ userId: "mock_user_04", status: "approved", pickupLocation: "Praha, Zličín" });
  console.log("✅ Packing, spolujízdy pro nové akce");

  // ═══ RATINGS (event 3 + event 10) ═══
  const ratings = [
    { id: "rat_01", userId: PRIMARY_USER, overallRating: 5, comment: "Skvělý víkend, děkuji všem!" },
    { id: "rat_02", userId: "mock_user_01", overallRating: 4, comment: "Moc fajn, jen jídla mohlo být víc" },
    { id: "rat_03", userId: "mock_user_03", overallRating: 5, comment: "Nejlepší výlet roku!" },
    { id: "rat_04", userId: "mock_user_05", overallRating: 4, comment: "Pěkné místo, dobrá parta." },
  ];
  for (const r of ratings) await db.collection("events").doc(event3Id).collection("ratings").doc(r.id).set(r);

  // Ratings for event 10 (grilovačka)
  const ratingsGril = [
    { id: "rat_g01", userId: PRIMARY_USER, overallRating: 4, comment: "Super grilovačka, maso bylo výborné!" },
    { id: "rat_g02", userId: "mock_user_01", overallRating: 5, comment: "Nejlepší grilovačka sezóny!" },
    { id: "rat_g03", userId: "mock_user_03", overallRating: 4, comment: "Fajn, jen příště víc veggie opcí" },
  ];
  for (const r of ratingsGril) await db.collection("events").doc(event10Id).collection("ratings").doc(r.id).set(r);
  console.log("✅ 7 hodnocení celkem");

  console.log("\n🎉 Vše hotovo!");
  console.log(`   ${PRIMARY_USER} má Business plán do ${new Date(now + 365 * day).toLocaleDateString("cs-CZ")}`);
  console.log("   10 akcí, 5 anket, 13 výdajů, 4 spolujízdy, 4 pokoje, 14 bodů harmonogramu, 18 balicích položek, 22 zpráv, 7 hodnocení");
  process.exit(0);
}

seed().catch(err => {
  console.error("❌ Chyba:", err.message);
  process.exit(1);
});
