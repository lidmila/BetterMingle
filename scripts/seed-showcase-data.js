const admin = require("firebase-admin");
const path = require("path");

const serviceAccount = require(path.join(process.env.USERPROFILE || process.env.HOME, "Documents/GitHub/bettermingle-firebase-adminsdk-fbsvc-2a0f1fb62e.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: "bettermingle",
});

const db = admin.firestore();

const PRIMARY_USER = "jZNd4RZMisbUHUZeOqH9PEbmay93";
const now = Date.now();
const day = 86400000;
const hr = 3600000;

// ═══ USER IDS ═══
const emily = "showcase_emily_chen";
const marcus = "showcase_marcus_wright";
const sophia = "showcase_sophia_martinez";
const james = "showcase_james_anderson";
const olivia = "showcase_olivia_thompson";
const daniel = "showcase_daniel_kim";
const rachel = "showcase_rachel_parker";
const nathan = "showcase_nathan_brooks";
const hannah = "showcase_hannah_ross";
const alex = "showcase_alex_rivera";
const jessica = "showcase_jessica_lee";
const ryan = "showcase_ryan_campbell";

async function seed() {
  console.log("Connecting to Firestore (bettermingle)...\n");

  // ═══ USERS ═══
  const users = {
    [PRIMARY_USER]: { isPremium: true, premiumTier: "business", premiumUntil: admin.firestore.Timestamp.fromMillis(now + 365 * day), createdAt: admin.firestore.Timestamp.fromMillis(now - 90 * day) },
    [emily]: { displayName: "Emily Chen", email: "emily.chen@novahorizon.com", avatarUrl: "", phone: "", dietaryPreferences: ["vegetarian"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 80 * day) },
    [marcus]: { displayName: "Marcus Wright", email: "marcus.wright@steelbridge.co", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 75 * day) },
    [sophia]: { displayName: "Sophia Martinez", email: "sophia.martinez@lunadesign.io", avatarUrl: "", phone: "", dietaryPreferences: ["gluten-free"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 70 * day) },
    [james]: { displayName: "James Anderson", email: "james.anderson@peakventures.com", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 65 * day) },
    [olivia]: { displayName: "Olivia Thompson", email: "olivia.thompson@brightspark.org", avatarUrl: "", phone: "", dietaryPreferences: ["vegan"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 60 * day) },
    [daniel]: { displayName: "Daniel Kim", email: "daniel.kim@crestwave.co", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 55 * day) },
    [rachel]: { displayName: "Rachel Parker", email: "rachel.parker@mapleleaf.net", avatarUrl: "", phone: "", dietaryPreferences: ["dairy-free"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 50 * day) },
    [nathan]: { displayName: "Nathan Brooks", email: "nathan.brooks@ironridge.io", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 45 * day) },
    [hannah]: { displayName: "Hannah Ross", email: "hannah.ross@coastline.co", avatarUrl: "", phone: "", dietaryPreferences: ["nut-free"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 40 * day) },
    [alex]: { displayName: "Alex Rivera", email: "alex.rivera@summitlabs.com", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 35 * day) },
    [jessica]: { displayName: "Jessica Lee", email: "jessica.lee@crystalclear.co", avatarUrl: "", phone: "", dietaryPreferences: ["pescatarian"], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 30 * day) },
    [ryan]: { displayName: "Ryan Campbell", email: "ryan.campbell@northstar.net", avatarUrl: "", phone: "", dietaryPreferences: [], isPremium: false, createdAt: admin.firestore.Timestamp.fromMillis(now - 25 * day) },
  };

  for (const [uid, data] of Object.entries(users)) {
    await db.collection("users").doc(uid).set(data, { merge: true });
  }
  console.log("✅ 13 users");

  // ═══════════════════════════════════════════
  // EVENT 1: Horizon Summit 2026 (ALL 11 modules)
  // ═══════════════════════════════════════════
  const e1Id = "showcase_horizon_summit";
  const e1Start = now + 21 * day;
  const e1 = {
    createdBy: PRIMARY_USER, name: "Horizon Summit 2026",
    description: "Annual corporate offsite — two days of strategy sessions, team challenges, and networking. Dress code: smart casual.",
    theme: "teambuilding", templateSlug: "teambuilding",
    locationName: "The Grand Conference Center, Austin TX", locationLat: 30.2672, locationLng: -97.7431,
    locationAddress: "500 E 4th St, Austin, TX 78701",
    startDate: e1Start, endDate: e1Start + 2 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "HRZN26", maxParticipants: 50, status: "CONFIRMED",
    enabledModules: ["VOTING", "EXPENSES", "CARPOOL", "ROOMS", "CHAT", "SCHEDULE", "TASKS", "PACKING_LIST", "WISHLIST", "CATERING", "BUDGET"],
    securityEnabled: false, eventPin: "", hideFinancials: false, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: false,
    moduleColors: {
      VOTING: "#5B5FEF", EXPENSES: "#E879B8", CARPOOL: "#34D399",
      SCHEDULE: "#F5A623", TASKS: "#8B5CF6", PACKING_LIST: "#14B8A6",
    },
    createdAt: now - 30 * day, updatedAt: now - day,
  };
  await db.collection("events").doc(e1Id).set(e1);

  // E1 participants (10): primary + emily, marcus, sophia, james, olivia, daniel, rachel, nathan, hannah
  const e1users = [PRIMARY_USER, emily, marcus, sophia, james, olivia, daniel, rachel, nathan, hannah];
  for (const uid of e1users) {
    await db.collection("events").doc(e1Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 25 * day,
    });
  }

  // E1 Polls
  await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_activity").set({
    createdBy: PRIMARY_USER, title: "Team Activity Preference", pollType: "ACTIVITY",
    allowMultiple: true, isAnonymous: false, deadline: e1Start - 3 * day, isClosed: false, createdAt: now - 20 * day,
  });
  const e1actOpts = [
    { id: "opt_e1a1", label: "Outdoor ropes course", description: "High-wire team challenge at Lake Travis", sortOrder: 0 },
    { id: "opt_e1a2", label: "Escape room tournament", description: "Three themed rooms, competing in teams", sortOrder: 1 },
    { id: "opt_e1a3", label: "Kayak relay race", description: "Tandem kayaks on Lady Bird Lake", sortOrder: 2 },
  ];
  for (const o of e1actOpts) await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_activity").collection("options").doc(o.id).set(o);
  const e1actVotes = [
    ["opt_e1a1", "v1", PRIMARY_USER], ["opt_e1a2", "v2", emily], ["opt_e1a1", "v3", marcus],
    ["opt_e1a3", "v4", sophia], ["opt_e1a1", "v5", james], ["opt_e1a2", "v6", olivia],
    ["opt_e1a1", "v7", daniel], ["opt_e1a3", "v8", rachel],
  ];
  for (const [optId, vId, uid] of e1actVotes) await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_activity").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });

  await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_lunch").set({
    createdBy: emily, title: "Lunch Catering Choice", pollType: "CUSTOM",
    allowMultiple: false, isAnonymous: false, deadline: e1Start - 5 * day, isClosed: false, createdAt: now - 18 * day,
  });
  const e1lunchOpts = [
    { id: "opt_e1l1", label: "Texas BBQ spread", description: "Brisket, ribs, coleslaw, cornbread", sortOrder: 0 },
    { id: "opt_e1l2", label: "Mediterranean buffet", description: "Grilled chicken, falafel, hummus, salads", sortOrder: 1 },
    { id: "opt_e1l3", label: "Build-your-own taco bar", description: "Protein options, toppings, fresh tortillas", sortOrder: 2 },
  ];
  for (const o of e1lunchOpts) await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_lunch").collection("options").doc(o.id).set(o);
  const e1lunchVotes = [
    ["opt_e1l1", "vl1", PRIMARY_USER], ["opt_e1l2", "vl2", emily], ["opt_e1l3", "vl3", marcus],
    ["opt_e1l1", "vl4", james], ["opt_e1l2", "vl5", olivia], ["opt_e1l3", "vl6", nathan],
    ["opt_e1l1", "vl7", hannah],
  ];
  for (const [optId, vId, uid] of e1lunchVotes) await db.collection("events").doc(e1Id).collection("polls").doc("poll_e1_lunch").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  console.log("✅ E1 polls + votes");

  // E1 Expenses (3)
  const e1expenses = [
    { id: "exp_e1_01", d: { paidBy: PRIMARY_USER, description: "Conference center deposit (2 days)", amount: 4500, currency: "USD", category: "pronájem", receiptUrl: "", createdAt: now - 20 * day }, perPerson: 450 },
    { id: "exp_e1_02", d: { paidBy: marcus, description: "AV equipment rental", amount: 1200, currency: "USD", category: "aktivity", receiptUrl: "", createdAt: now - 10 * day }, perPerson: 120 },
    { id: "exp_e1_03", d: { paidBy: emily, description: "Welcome dinner at Salt Lick BBQ", amount: 850, currency: "USD", category: "jídlo", receiptUrl: "", createdAt: now - 5 * day }, perPerson: 85 },
  ];
  for (const e of e1expenses) {
    await db.collection("events").doc(e1Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e1users.length; i++) {
      await db.collection("events").doc(e1Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e1users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E1 Carpool (2)
  await db.collection("events").doc(e1Id).collection("carpoolRides").doc("ride_e1_01").set({
    driverId: PRIMARY_USER, driverName: "Organizer", departureLocation: "Downtown Austin Hilton", departureLat: 30.264, departureLng: -97.739,
    departureTime: e1Start - 2 * hr, availableSeats: 4, notes: "Picking up from hotel lobby at 7:30 AM", type: "OFFER", isClosed: false, createdAt: now - 10 * day,
  });
  for (const p of [
    { id: "p_e1_01", userId: emily, displayName: "Emily Chen", status: "APPROVED", pickupLocation: "Hilton lobby" },
    { id: "p_e1_02", userId: sophia, displayName: "Sophia Martinez", status: "APPROVED", pickupLocation: "Hilton lobby" },
  ]) await db.collection("events").doc(e1Id).collection("carpoolRides").doc("ride_e1_01").collection("passengers").doc(p.id).set(p);

  await db.collection("events").doc(e1Id).collection("carpoolRides").doc("ride_e1_02").set({
    driverId: james, driverName: "James Anderson", departureLocation: "Austin-Bergstrom Airport", departureLat: 30.197, departureLng: -97.666,
    departureTime: e1Start - 3 * hr, availableSeats: 3, notes: "Renting an SUV, happy to detour", type: "OFFER", isClosed: false, createdAt: now - 8 * day,
  });
  for (const p of [
    { id: "p_e1_03", userId: daniel, displayName: "Daniel Kim", status: "APPROVED", pickupLocation: "Terminal arrivals" },
    { id: "p_e1_04", userId: nathan, displayName: "Nathan Brooks", status: "PENDING", pickupLocation: "Terminal arrivals" },
  ]) await db.collection("events").doc(e1Id).collection("carpoolRides").doc("ride_e1_02").collection("passengers").doc(p.id).set(p);

  // E1 Rooms (3)
  const e1rooms = [
    { id: "room_e1_01", name: "Suite 401 — Executive", capacity: 3, notes: "Corner suite, two queen beds + sofa", assignments: [PRIMARY_USER, emily, sophia] },
    { id: "room_e1_02", name: "Suite 402 — Lakeview", capacity: 3, notes: "Lake view, balcony", assignments: [marcus, james, daniel] },
    { id: "room_e1_03", name: "Suite 403 — Garden", capacity: 4, notes: "Ground floor, garden access", assignments: [olivia, rachel, nathan, hannah] },
  ];
  for (const r of e1rooms) await db.collection("events").doc(e1Id).collection("rooms").doc(r.id).set(r);

  // E1 Schedule (6 blocks — 2-day agenda)
  const e1sched = [
    { id: "sch_e1_01", title: "Registration & Welcome Coffee", description: "Badge pickup, networking, barista station", startTime: e1Start, endTime: e1Start + hr, location: "Main Lobby" },
    { id: "sch_e1_02", title: "Keynote: Vision 2027", description: "CEO presents company direction and Q&A", startTime: e1Start + hr, endTime: e1Start + 3 * hr, location: "Grand Ballroom" },
    { id: "sch_e1_03", title: "Breakout Sessions", description: "Three parallel tracks: Product, Engineering, Go-to-Market", startTime: e1Start + 4 * hr, endTime: e1Start + 6 * hr, location: "Conference Rooms A-C" },
    { id: "sch_e1_04", title: "Team Challenge: Outdoor Ropes Course", description: "Trust falls, zip line, team relay", startTime: e1Start + 7 * hr, endTime: e1Start + 10 * hr, location: "Lake Travis Adventure Park" },
    { id: "sch_e1_05", title: "Day 2 — Innovation Workshop", description: "Design sprint: prototype a new feature in 3 hours", startTime: e1Start + day + 2 * hr, endTime: e1Start + day + 5 * hr, location: "Workshop Hall" },
    { id: "sch_e1_06", title: "Closing Awards & Happy Hour", description: "Team awards, raffle, open bar", startTime: e1Start + day + 6 * hr, endTime: e1Start + day + 9 * hr, location: "Rooftop Terrace" },
  ];
  for (const s of e1sched) await db.collection("events").doc(e1Id).collection("schedule").doc(s.id).set(s);

  // E1 Tasks (5)
  const e1tasks = [
    { id: "task_e1_01", name: "Book AV equipment", color: "Modrá", assignedTo: [marcus], deadline: e1Start - 7 * day, isCompleted: true, createdAt: now - 25 * day },
    { id: "task_e1_02", name: "Finalize catering menu", color: "Růžová", assignedTo: [emily, sophia], deadline: e1Start - 5 * day, isCompleted: true, createdAt: now - 22 * day },
    { id: "task_e1_03", name: "Print name badges", color: "Oranžová", assignedTo: [PRIMARY_USER], deadline: e1Start - 2 * day, isCompleted: false, createdAt: now - 15 * day },
    { id: "task_e1_04", name: "Prepare keynote slides", color: "Zlatá", assignedTo: [PRIMARY_USER, james], deadline: e1Start - 3 * day, isCompleted: false, createdAt: now - 20 * day },
    { id: "task_e1_05", name: "Arrange airport transfers", color: "Zelená", assignedTo: [daniel], deadline: e1Start - 4 * day, isCompleted: true, createdAt: now - 18 * day },
  ];
  for (const t of e1tasks) await db.collection("events").doc(e1Id).collection("tasks").doc(t.id).set(t);

  // E1 Packing (8)
  const e1packing = [
    { id: "pack_e1_01", name: "Laptop & charger", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e1Id, createdAt: now - 15 * day },
    { id: "pack_e1_02", name: "Business cards", isChecked: false, userId: null, addedBy: PRIMARY_USER, eventId: e1Id, createdAt: now - 15 * day },
    { id: "pack_e1_03", name: "Comfortable walking shoes", isChecked: true, userId: null, addedBy: emily, eventId: e1Id, createdAt: now - 12 * day },
    { id: "pack_e1_04", name: "Sunscreen SPF 50", isChecked: false, userId: null, addedBy: sophia, eventId: e1Id, createdAt: now - 10 * day },
    { id: "pack_e1_05", name: "Reusable water bottle", isChecked: true, userId: null, addedBy: olivia, eventId: e1Id, createdAt: now - 10 * day },
    { id: "pack_e1_06", name: "Notebook & pen", isChecked: false, userId: PRIMARY_USER, addedBy: PRIMARY_USER, eventId: e1Id, createdAt: now - 8 * day },
    { id: "pack_e1_07", name: "Portable phone charger", isChecked: false, userId: marcus, addedBy: marcus, eventId: e1Id, createdAt: now - 7 * day },
    { id: "pack_e1_08", name: "Light jacket for evenings", isChecked: true, userId: null, addedBy: rachel, eventId: e1Id, createdAt: now - 6 * day },
  ];
  for (const p of e1packing) await db.collection("events").doc(e1Id).collection("packingItems").doc(p.id).set(p);

  // E1 Budget (3 categories with sub-expenses)
  const e1budgetCats = [
    { id: "bcat_e1_01", name: "Venue & Logistics", planned: 6000, actualAmount: 5700, fromTemplate: false, createdAt: now - 28 * day },
    { id: "bcat_e1_02", name: "Food & Beverages", planned: 3000, actualAmount: 2450, fromTemplate: false, createdAt: now - 28 * day },
    { id: "bcat_e1_03", name: "Activities & Entertainment", planned: 2500, actualAmount: 1200, fromTemplate: false, createdAt: now - 28 * day },
  ];
  for (const c of e1budgetCats) await db.collection("events").doc(e1Id).collection("budgetCategories").doc(c.id).set(c);
  const e1budgetExps = [
    { catId: "bcat_e1_01", id: "bexp_e1_01", amount: 4500, note: "Conference center deposit", addedByName: "Organizer", createdAt: now - 20 * day },
    { catId: "bcat_e1_01", id: "bexp_e1_02", amount: 1200, note: "AV equipment rental", addedByName: "Marcus Wright", createdAt: now - 10 * day },
    { catId: "bcat_e1_02", id: "bexp_e1_03", amount: 850, note: "Welcome dinner", addedByName: "Emily Chen", createdAt: now - 5 * day },
    { catId: "bcat_e1_02", id: "bexp_e1_04", amount: 1600, note: "Day 1 & 2 catering (estimated)", addedByName: "Organizer", createdAt: now - 3 * day },
    { catId: "bcat_e1_03", id: "bexp_e1_05", amount: 1200, note: "Ropes course group booking", addedByName: "Organizer", createdAt: now - 8 * day },
  ];
  for (const e of e1budgetExps) await db.collection("events").doc(e1Id).collection("budgetCategories").doc(e.catId).collection("expenses").doc(e.id).set({ id: e.id, amount: e.amount, note: e.note, addedByName: e.addedByName, createdAt: e.createdAt });

  // E1 Wishlist (4)
  const e1wishlist = [
    { id: "wish_e1_01", name: "Portable Bluetooth speaker", price: "$45", productUrl: "", description: "For outdoor sessions", status: "BOUGHT", claimedBy: marcus, claimedByName: "Marcus Wright", addedBy: PRIMARY_USER, eventId: e1Id, createdAt: now - 15 * day },
    { id: "wish_e1_02", name: "Team photo frame set", price: "$30", productUrl: "", description: "Matching frames for group photo", status: "RESERVED", claimedBy: emily, claimedByName: "Emily Chen", addedBy: sophia, eventId: e1Id, createdAt: now - 12 * day },
    { id: "wish_e1_03", name: "Custom team t-shirts", price: "$250", productUrl: "", description: "10 shirts with summit logo", status: "FREE", claimedBy: null, claimedByName: null, addedBy: PRIMARY_USER, eventId: e1Id, createdAt: now - 10 * day },
    { id: "wish_e1_04", name: "Raffle prizes gift basket", price: "$100", productUrl: "", description: "Snacks, gadgets, gift cards", status: "FREE", claimedBy: null, claimedByName: null, addedBy: james, eventId: e1Id, createdAt: now - 8 * day },
  ];
  for (const w of e1wishlist) await db.collection("events").doc(e1Id).collection("wishlistItems").doc(w.id).set(w);

  // E1 Chat (10 messages)
  const e1msgs = [
    { id: "msg_e1_01", userId: PRIMARY_USER, userName: "Organizer", content: "Hey everyone! Excited to announce the Horizon Summit is a go. Check the schedule tab for the full agenda.", replyTo: null, createdAt: now - 20 * day },
    { id: "msg_e1_02", userId: emily, userName: "Emily Chen", content: "This looks amazing! Quick question — is the ropes course beginner-friendly?", replyTo: null, createdAt: now - 19 * day },
    { id: "msg_e1_03", userId: james, userName: "James Anderson", content: "Yes, they have different difficulty levels. I did it last year, totally safe.", replyTo: "msg_e1_02", createdAt: now - 19 * day + hr },
    { id: "msg_e1_04", userId: sophia, userName: "Sophia Martinez", content: "Can we make sure there are gluten-free options for lunch? I updated my dietary info.", replyTo: null, createdAt: now - 15 * day },
    { id: "msg_e1_05", userId: PRIMARY_USER, userName: "Organizer", content: "Absolutely, Sophia. The catering team has everyone's preferences on file.", replyTo: "msg_e1_04", createdAt: now - 15 * day + 2 * hr },
    { id: "msg_e1_06", userId: marcus, userName: "Marcus Wright", content: "AV equipment is booked. We'll have a projector, wireless mics, and a PA system.", replyTo: null, createdAt: now - 10 * day },
    { id: "msg_e1_07", userId: olivia, userName: "Olivia Thompson", content: "Who else is flying in? I land at 3 PM the day before.", replyTo: null, createdAt: now - 8 * day },
    { id: "msg_e1_08", userId: daniel, userName: "Daniel Kim", content: "Same flight as me! James is offering rides from the airport.", replyTo: "msg_e1_07", createdAt: now - 8 * day + hr },
    { id: "msg_e1_09", userId: rachel, userName: "Rachel Parker", content: "Don't forget to vote in the activity poll — outdoor ropes course is winning!", replyTo: null, createdAt: now - 5 * day },
    { id: "msg_e1_10", userId: PRIMARY_USER, userName: "Organizer", content: "One week out! Final headcount is 10. See you all in Austin.", replyTo: null, createdAt: now - 2 * day },
  ];
  for (const m of e1msgs) await db.collection("events").doc(e1Id).collection("messages").doc(m.id).set(m);

  // E1 Activity log (4 entries)
  const e1activity = [
    { id: "act_e1_01", actorId: PRIMARY_USER, actorName: "Organizer", type: "settings", description: "Created the event and set the schedule", timestamp: now - 30 * day, readBy: [PRIMARY_USER] },
    { id: "act_e1_02", actorId: marcus, actorName: "Marcus Wright", type: "task", description: "Completed task: Book AV equipment", timestamp: now - 10 * day, readBy: [PRIMARY_USER, marcus] },
    { id: "act_e1_03", actorId: emily, actorName: "Emily Chen", type: "task", description: "Completed task: Finalize catering menu", timestamp: now - 6 * day, readBy: [PRIMARY_USER] },
    { id: "act_e1_04", actorId: PRIMARY_USER, actorName: "Organizer", type: "settings", description: "Updated the schedule with Day 2 sessions", timestamp: now - 2 * day, readBy: [] },
  ];
  for (const a of e1activity) await db.collection("events").doc(e1Id).collection("activity").doc(a.id).set(a);
  console.log("✅ E1: Horizon Summit 2026 — all 11 modules");

  // ═══════════════════════════════════════════
  // EVENT 2: Jake's Last Ride (Bachelor Party)
  // ═══════════════════════════════════════════
  const e2Id = "showcase_jakes_last_ride";
  const e2Start = now + 14 * day;
  const e2 = {
    createdBy: PRIMARY_USER, name: "Jake's Last Ride",
    description: "Bachelor party weekend in Vegas. What happens here, stays here.",
    theme: "rozlučka", templateSlug: "rozlucka",
    locationName: "The Venetian Resort, Las Vegas NV", locationLat: 36.1215, locationLng: -115.1739,
    locationAddress: "3355 S Las Vegas Blvd, Las Vegas, NV 89109",
    startDate: e2Start, endDate: e2Start + 2 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "JKLR26", maxParticipants: 10, status: "CONFIRMED",
    enabledModules: ["VOTING", "EXPENSES", "CARPOOL", "ROOMS", "CHAT", "SCHEDULE", "PACKING_LIST"],
    securityEnabled: true, eventPin: "7742", hideFinancials: true, screenshotProtection: true,
    autoDeleteDays: 14, requireApproval: true,
    createdAt: now - 25 * day, updatedAt: now - 2 * day,
  };
  await db.collection("events").doc(e2Id).set(e2);

  const e2users = [PRIMARY_USER, marcus, james, daniel, nathan, ryan, alex];
  for (const uid of e2users) {
    await db.collection("events").doc(e2Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 20 * day,
    });
  }

  // E2 Poll (1)
  await db.collection("events").doc(e2Id).collection("polls").doc("poll_e2_nightout").set({
    createdBy: PRIMARY_USER, title: "Saturday Night Plan", pollType: "ACTIVITY",
    allowMultiple: false, isAnonymous: false, deadline: e2Start - 2 * day, isClosed: false, createdAt: now - 15 * day,
  });
  const e2opts = [
    { id: "opt_e2n1", label: "VIP table at Omnia", description: "Bottle service, DJ set", sortOrder: 0 },
    { id: "opt_e2n2", label: "Poker tournament at Bellagio", description: "Private table, buy-in $200", sortOrder: 1 },
    { id: "opt_e2n3", label: "Helicopter tour over the Strip", description: "30 min flight, champagne toast", sortOrder: 2 },
  ];
  for (const o of e2opts) await db.collection("events").doc(e2Id).collection("polls").doc("poll_e2_nightout").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e2n1", "ve2_1", PRIMARY_USER], ["opt_e2n3", "ve2_2", marcus], ["opt_e2n1", "ve2_3", james], ["opt_e2n1", "ve2_4", daniel], ["opt_e2n2", "ve2_5", nathan]]) {
    await db.collection("events").doc(e2Id).collection("polls").doc("poll_e2_nightout").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // E2 Expenses (4)
  const e2expenses = [
    { id: "exp_e2_01", d: { paidBy: PRIMARY_USER, description: "Venetian suite (2 nights)", amount: 2800, currency: "USD", category: "ubytování", receiptUrl: "", createdAt: now - 18 * day }, perPerson: 400 },
    { id: "exp_e2_02", d: { paidBy: marcus, description: "Steakhouse dinner (group)", amount: 980, currency: "USD", category: "jídlo", receiptUrl: "", createdAt: now - 10 * day }, perPerson: 140 },
    { id: "exp_e2_03", d: { paidBy: james, description: "Pool cabana reservation", amount: 450, currency: "USD", category: "aktivity", receiptUrl: "", createdAt: now - 8 * day }, perPerson: 64.29 },
    { id: "exp_e2_04", d: { paidBy: daniel, description: "Airport shuttle service", amount: 210, currency: "USD", category: "doprava", receiptUrl: "", createdAt: now - 5 * day }, perPerson: 30 },
  ];
  for (const e of e2expenses) {
    await db.collection("events").doc(e2Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e2users.length; i++) {
      await db.collection("events").doc(e2Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e2users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E2 Carpool (1)
  await db.collection("events").doc(e2Id).collection("carpoolRides").doc("ride_e2_01").set({
    driverId: ryan, driverName: "Ryan Campbell", departureLocation: "LAX Airport", departureLat: 33.9425, departureLng: -118.408,
    departureTime: e2Start - 4 * hr, availableSeats: 4, notes: "Renting a convertible for the drive to Vegas!", type: "OFFER", isClosed: false, createdAt: now - 12 * day,
  });
  for (const p of [
    { id: "p_e2_01", userId: alex, displayName: "Alex Rivera", status: "APPROVED", pickupLocation: "LAX Terminal 4" },
    { id: "p_e2_02", userId: nathan, displayName: "Nathan Brooks", status: "APPROVED", pickupLocation: "LAX Terminal 4" },
  ]) await db.collection("events").doc(e2Id).collection("carpoolRides").doc("ride_e2_01").collection("passengers").doc(p.id).set(p);

  // E2 Rooms (2)
  const e2rooms = [
    { id: "room_e2_01", name: "Bella Suite — Floor 28", capacity: 4, notes: "Strip view, living room, 2 king beds", assignments: [PRIMARY_USER, marcus, james, daniel] },
    { id: "room_e2_02", name: "Rialto Suite — Floor 26", capacity: 3, notes: "Pool view, king + pullout sofa", assignments: [nathan, ryan, alex] },
  ];
  for (const r of e2rooms) await db.collection("events").doc(e2Id).collection("rooms").doc(r.id).set(r);

  // E2 Schedule (5)
  const e2sched = [
    { id: "sch_e2_01", title: "Arrival & Check-in", description: "Meet in lobby, get room keys", startTime: e2Start, endTime: e2Start + hr, location: "Venetian Lobby" },
    { id: "sch_e2_02", title: "Pool Party", description: "Cabana reserved, open bar", startTime: e2Start + 2 * hr, endTime: e2Start + 5 * hr, location: "Venetian Pool Deck" },
    { id: "sch_e2_03", title: "Steakhouse Dinner", description: "Private dining room booked", startTime: e2Start + 8 * hr, endTime: e2Start + 10 * hr, location: "CUT by Wolfgang Puck" },
    { id: "sch_e2_04", title: "Night Out", description: "See the poll results — TBD", startTime: e2Start + 11 * hr, endTime: e2Start + 16 * hr, location: "The Strip" },
    { id: "sch_e2_05", title: "Brunch & Checkout", description: "Recovery brunch before flights", startTime: e2Start + day + 4 * hr, endTime: e2Start + day + 6 * hr, location: "Grand Lux Cafe" },
  ];
  for (const s of e2sched) await db.collection("events").doc(e2Id).collection("schedule").doc(s.id).set(s);

  // E2 Packing (6)
  const e2packing = [
    { id: "pack_e2_01", name: "Swim trunks", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e2Id, createdAt: now - 12 * day },
    { id: "pack_e2_02", name: "Sunglasses", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e2Id, createdAt: now - 12 * day },
    { id: "pack_e2_03", name: "Dress shoes (for dinner)", isChecked: false, userId: null, addedBy: marcus, eventId: e2Id, createdAt: now - 10 * day },
    { id: "pack_e2_04", name: "Cash for poker", isChecked: false, userId: null, addedBy: nathan, eventId: e2Id, createdAt: now - 8 * day },
    { id: "pack_e2_05", name: "Phone charger", isChecked: true, userId: null, addedBy: james, eventId: e2Id, createdAt: now - 7 * day },
    { id: "pack_e2_06", name: "Matching party shirts", isChecked: false, userId: PRIMARY_USER, addedBy: PRIMARY_USER, eventId: e2Id, createdAt: now - 5 * day },
  ];
  for (const p of e2packing) await db.collection("events").doc(e2Id).collection("packingItems").doc(p.id).set(p);

  // E2 Chat (12 messages)
  const e2msgs = [
    { id: "msg_e2_01", userId: PRIMARY_USER, userName: "Organizer", content: "Alright boys, it's official — Vegas is ON. Jake has no idea.", replyTo: null, createdAt: now - 22 * day },
    { id: "msg_e2_02", userId: marcus, userName: "Marcus Wright", content: "Let's gooooo! This is gonna be legendary.", replyTo: null, createdAt: now - 22 * day + hr },
    { id: "msg_e2_03", userId: james, userName: "James Anderson", content: "I'm in charge of the pool cabana. Trust me.", replyTo: null, createdAt: now - 20 * day },
    { id: "msg_e2_04", userId: daniel, userName: "Daniel Kim", content: "How are we getting from the airport? I can book a shuttle.", replyTo: null, createdAt: now - 18 * day },
    { id: "msg_e2_05", userId: PRIMARY_USER, userName: "Organizer", content: "Daniel, that would be great. Ryan is driving from LA with Alex and Nathan.", replyTo: "msg_e2_04", createdAt: now - 18 * day + 2 * hr },
    { id: "msg_e2_06", userId: ryan, userName: "Ryan Campbell", content: "Convertible is booked. Road trip crew, be ready at LAX by noon.", replyTo: null, createdAt: now - 15 * day },
    { id: "msg_e2_07", userId: nathan, userName: "Nathan Brooks", content: "Should I bring poker chips or are we hitting the real tables?", replyTo: null, createdAt: now - 12 * day },
    { id: "msg_e2_08", userId: PRIMARY_USER, userName: "Organizer", content: "Real tables, baby. Bellagio has a private room option.", replyTo: "msg_e2_07", createdAt: now - 12 * day + hr },
    { id: "msg_e2_09", userId: alex, userName: "Alex Rivera", content: "Vote in the Saturday night poll! VIP table is leading.", replyTo: null, createdAt: now - 8 * day },
    { id: "msg_e2_10", userId: marcus, userName: "Marcus Wright", content: "I made a dinner reservation at CUT. 8 PM, dress code smart casual.", replyTo: null, createdAt: now - 6 * day },
    { id: "msg_e2_11", userId: james, userName: "James Anderson", content: "Reminder: what happens in Vegas stays in Vegas. And in this chat.", replyTo: null, createdAt: now - 3 * day },
    { id: "msg_e2_12", userId: PRIMARY_USER, userName: "Organizer", content: "Two weeks out. Everyone confirmed? Sound off below.", replyTo: null, createdAt: now - 2 * day },
  ];
  for (const m of e2msgs) await db.collection("events").doc(e2Id).collection("messages").doc(m.id).set(m);
  console.log("✅ E2: Jake's Last Ride — 7 modules + security");

  // ═══════════════════════════════════════════
  // EVENT 3: Lauren & David's Wedding
  // ═══════════════════════════════════════════
  const e3Id = "showcase_lauren_david_wedding";
  const e3Start = now + 45 * day;
  const e3 = {
    createdBy: PRIMARY_USER, name: "Lauren & David's Wedding",
    description: "A garden ceremony and reception in the heart of Napa Valley. Black tie optional.",
    theme: "svatba", templateSlug: "svatba",
    locationName: "Rosewood Estate Gardens, Napa Valley CA", locationLat: 38.2975, locationLng: -122.2869,
    locationAddress: "1000 Main St, St Helena, CA 94574",
    startDate: e3Start, endDate: e3Start + day, datesFinalized: false,
    coverImageUrl: "", inviteCode: "LDWED", maxParticipants: 100, status: "PLANNING",
    enabledModules: ["VOTING", "EXPENSES", "CHAT", "SCHEDULE", "TASKS", "WISHLIST", "CATERING", "BUDGET"],
    securityEnabled: false, eventPin: "", hideFinancials: false, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: false,
    moduleColors: {
      SCHEDULE: "#E879B8", TASKS: "#5B5FEF", WISHLIST: "#F5A623", BUDGET: "#34D399",
    },
    createdAt: now - 60 * day, updatedAt: now - 3 * day,
  };
  await db.collection("events").doc(e3Id).set(e3);

  const e3users = [PRIMARY_USER, emily, sophia, james, olivia, rachel, hannah, jessica, nathan, alex];
  for (const uid of e3users) {
    let role = "participant";
    if (uid === PRIMARY_USER) role = "organizer";
    if (uid === emily || uid === sophia) role = "co_organizer";
    await db.collection("events").doc(e3Id).collection("participants").doc(uid).set({
      userId: uid, role,
      rsvp: uid === nathan ? "maybe" : "accepted", joinedAt: now - 55 * day,
    });
  }

  // E3 Polls (2)
  await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_ceremony").set({
    createdBy: PRIMARY_USER, title: "Ceremony Time Preference", pollType: "DATE",
    allowMultiple: false, isAnonymous: false, deadline: e3Start - 14 * day, isClosed: false, createdAt: now - 50 * day,
  });
  const e3cerOpts = [
    { id: "opt_e3c1", label: "4:00 PM — Golden hour start", description: "Sunset photos right after ceremony", sortOrder: 0 },
    { id: "opt_e3c2", label: "2:00 PM — Classic afternoon", description: "More time for reception activities", sortOrder: 1 },
  ];
  for (const o of e3cerOpts) await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_ceremony").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e3c1", "vc1", PRIMARY_USER], ["opt_e3c1", "vc2", emily], ["opt_e3c2", "vc3", sophia], ["opt_e3c1", "vc4", james], ["opt_e3c1", "vc5", olivia], ["opt_e3c2", "vc6", rachel]]) {
    await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_ceremony").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_dessert").set({
    createdBy: sophia, title: "Dessert Table Selection", pollType: "CUSTOM",
    allowMultiple: true, isAnonymous: false, deadline: e3Start - 10 * day, isClosed: false, createdAt: now - 40 * day,
  });
  const e3dessOpts = [
    { id: "opt_e3d1", label: "Classic wedding cake (vanilla & raspberry)", description: "Three-tier, buttercream frosting", sortOrder: 0 },
    { id: "opt_e3d2", label: "Cupcake tower", description: "Assorted flavors, mini cupcakes", sortOrder: 1 },
    { id: "opt_e3d3", label: "Crème brûlée bar", description: "Individual ramekins, torched on site", sortOrder: 2 },
  ];
  for (const o of e3dessOpts) await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_dessert").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e3d1", "vd1", PRIMARY_USER], ["opt_e3d1", "vd2", emily], ["opt_e3d3", "vd3", jessica], ["opt_e3d2", "vd4", hannah]]) {
    await db.collection("events").doc(e3Id).collection("polls").doc("poll_e3_dessert").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // E3 Expenses (3)
  const e3expenses = [
    { id: "exp_e3_01", d: { paidBy: PRIMARY_USER, description: "Rosewood venue deposit", amount: 8500, currency: "USD", category: "pronájem", receiptUrl: "", createdAt: now - 55 * day }, users: [PRIMARY_USER], perPerson: 8500 },
    { id: "exp_e3_02", d: { paidBy: emily, description: "Floral arrangements (deposit)", amount: 2200, currency: "USD", category: "dekorace", receiptUrl: "", createdAt: now - 35 * day }, users: [PRIMARY_USER, emily], perPerson: 1100 },
    { id: "exp_e3_03", d: { paidBy: sophia, description: "Invitation printing & calligraphy", amount: 680, currency: "USD", category: "dekorace", receiptUrl: "", createdAt: now - 25 * day }, users: [PRIMARY_USER, sophia], perPerson: 340 },
  ];
  for (const e of e3expenses) {
    await db.collection("events").doc(e3Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e.users.length; i++) {
      await db.collection("events").doc(e3Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e.users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E3 Schedule (8 blocks)
  const e3sched = [
    { id: "sch_e3_01", title: "Guest Arrival & Cocktail Hour", description: "Welcome drinks, string quartet", startTime: e3Start, endTime: e3Start + hr, location: "Rose Garden Entrance" },
    { id: "sch_e3_02", title: "Ceremony", description: "Outdoor garden ceremony under the arch", startTime: e3Start + hr, endTime: e3Start + 2 * hr, location: "Main Garden Pavilion" },
    { id: "sch_e3_03", title: "Couple's Photo Session", description: "Golden hour portraits in the vineyard", startTime: e3Start + 2 * hr, endTime: e3Start + 3 * hr, location: "Vineyard Path" },
    { id: "sch_e3_04", title: "Reception Dinner", description: "Seated dinner, 3-course meal", startTime: e3Start + 3 * hr, endTime: e3Start + 5 * hr, location: "Estate Ballroom" },
    { id: "sch_e3_05", title: "Toasts & Speeches", description: "Best man, maid of honor, parents", startTime: e3Start + 5 * hr, endTime: e3Start + 6 * hr, location: "Estate Ballroom" },
    { id: "sch_e3_06", title: "First Dance & Open Floor", description: "Live band, open dance floor", startTime: e3Start + 6 * hr, endTime: e3Start + 8 * hr, location: "Ballroom Dance Floor" },
    { id: "sch_e3_07", title: "Dessert Bar & Late Night Bites", description: "Cake cutting, crème brûlée, sliders", startTime: e3Start + 8 * hr, endTime: e3Start + 9 * hr, location: "Garden Terrace" },
    { id: "sch_e3_08", title: "Sparkler Send-Off", description: "Line up for the couple's grand exit", startTime: e3Start + 10 * hr, endTime: e3Start + 10 * hr + 30 * 60000, location: "Main Drive" },
  ];
  for (const s of e3sched) await db.collection("events").doc(e3Id).collection("schedule").doc(s.id).set(s);

  // E3 Tasks (8)
  const e3tasks = [
    { id: "task_e3_01", name: "Confirm final headcount with venue", color: "Růžová", assignedTo: [PRIMARY_USER], deadline: e3Start - 14 * day, isCompleted: false, createdAt: now - 50 * day },
    { id: "task_e3_02", name: "Book hair & makeup artist", color: "Růžová", assignedTo: [emily], deadline: e3Start - 30 * day, isCompleted: true, createdAt: now - 55 * day },
    { id: "task_e3_03", name: "Arrange transportation for guests", color: "Modrá", assignedTo: [sophia], deadline: e3Start - 10 * day, isCompleted: false, createdAt: now - 40 * day },
    { id: "task_e3_04", name: "Select wine pairings for dinner", color: "Oranžová", assignedTo: [PRIMARY_USER, james], deadline: e3Start - 21 * day, isCompleted: true, createdAt: now - 45 * day },
    { id: "task_e3_05", name: "Design seating chart", color: "Zlatá", assignedTo: [emily, sophia], deadline: e3Start - 7 * day, isCompleted: false, createdAt: now - 35 * day },
    { id: "task_e3_06", name: "Order wedding favors", color: "Zelená", assignedTo: [rachel], deadline: e3Start - 21 * day, isCompleted: true, createdAt: now - 40 * day },
    { id: "task_e3_07", name: "Coordinate with DJ for playlist", color: "Modrá", assignedTo: [alex], deadline: e3Start - 14 * day, isCompleted: false, createdAt: now - 30 * day },
    { id: "task_e3_08", name: "Prepare emergency kit", color: "Oranžová", assignedTo: [hannah], deadline: e3Start - 3 * day, isCompleted: false, createdAt: now - 20 * day },
  ];
  for (const t of e3tasks) await db.collection("events").doc(e3Id).collection("tasks").doc(t.id).set(t);

  // E3 Budget (4 categories)
  const e3budgetCats = [
    { id: "bcat_e3_01", name: "Venue & Rentals", planned: 12000, actualAmount: 8500, fromTemplate: false, createdAt: now - 58 * day },
    { id: "bcat_e3_02", name: "Flowers & Decor", planned: 4000, actualAmount: 2880, fromTemplate: false, createdAt: now - 58 * day },
    { id: "bcat_e3_03", name: "Catering & Bar", planned: 8000, actualAmount: 0, fromTemplate: false, createdAt: now - 58 * day },
    { id: "bcat_e3_04", name: "Photography & Music", planned: 5000, actualAmount: 1500, fromTemplate: false, createdAt: now - 58 * day },
  ];
  for (const c of e3budgetCats) await db.collection("events").doc(e3Id).collection("budgetCategories").doc(c.id).set(c);
  const e3budgetExps = [
    { catId: "bcat_e3_01", id: "bexp_e3_01", amount: 8500, note: "Rosewood venue deposit", addedByName: "Organizer", createdAt: now - 55 * day },
    { catId: "bcat_e3_02", id: "bexp_e3_02", amount: 2200, note: "Floral deposit", addedByName: "Emily Chen", createdAt: now - 35 * day },
    { catId: "bcat_e3_02", id: "bexp_e3_03", amount: 680, note: "Invitations", addedByName: "Sophia Martinez", createdAt: now - 25 * day },
    { catId: "bcat_e3_04", id: "bexp_e3_04", amount: 1500, note: "Photographer retainer", addedByName: "Organizer", createdAt: now - 30 * day },
  ];
  for (const e of e3budgetExps) await db.collection("events").doc(e3Id).collection("budgetCategories").doc(e.catId).collection("expenses").doc(e.id).set({ id: e.id, amount: e.amount, note: e.note, addedByName: e.addedByName, createdAt: e.createdAt });

  // E3 Wishlist (6)
  const e3wishlist = [
    { id: "wish_e3_01", name: "KitchenAid Stand Mixer", price: "$350", productUrl: "", description: "For the new kitchen", status: "RESERVED", claimedBy: james, claimedByName: "James Anderson", addedBy: PRIMARY_USER, eventId: e3Id, createdAt: now - 50 * day },
    { id: "wish_e3_02", name: "Le Creuset Dutch Oven", price: "$280", productUrl: "", description: "Flame color, 5.5 qt", status: "BOUGHT", claimedBy: olivia, claimedByName: "Olivia Thompson", addedBy: PRIMARY_USER, eventId: e3Id, createdAt: now - 50 * day },
    { id: "wish_e3_03", name: "Honeymoon fund contribution", price: "", productUrl: "", description: "Any amount appreciated!", status: "FREE", claimedBy: null, claimedByName: null, addedBy: PRIMARY_USER, eventId: e3Id, createdAt: now - 50 * day },
    { id: "wish_e3_04", name: "Dyson V15 Vacuum", price: "$750", productUrl: "", description: "For the new apartment", status: "FREE", claimedBy: null, claimedByName: null, addedBy: PRIMARY_USER, eventId: e3Id, createdAt: now - 45 * day },
    { id: "wish_e3_05", name: "Wine of the Month subscription", price: "$200", productUrl: "", description: "6-month subscription", status: "RESERVED", claimedBy: hannah, claimedByName: "Hannah Ross", addedBy: emily, eventId: e3Id, createdAt: now - 40 * day },
    { id: "wish_e3_06", name: "Linen bedding set", price: "$320", productUrl: "", description: "King size, ivory", status: "FREE", claimedBy: null, claimedByName: null, addedBy: sophia, eventId: e3Id, createdAt: now - 35 * day },
  ];
  for (const w of e3wishlist) await db.collection("events").doc(e3Id).collection("wishlistItems").doc(w.id).set(w);

  // E3 Chat (10)
  const e3msgs = [
    { id: "msg_e3_01", userId: PRIMARY_USER, userName: "Organizer", content: "Welcome to the wedding planning group! Lauren and David are thrilled you're all here.", replyTo: null, createdAt: now - 55 * day },
    { id: "msg_e3_02", userId: emily, userName: "Emily Chen", content: "So honored to be a co-organizer. Let's make this the most beautiful day ever!", replyTo: null, createdAt: now - 54 * day },
    { id: "msg_e3_03", userId: sophia, userName: "Sophia Martinez", content: "I've started a mood board for the floral arrangements. Peonies and garden roses?", replyTo: null, createdAt: now - 50 * day },
    { id: "msg_e3_04", userId: PRIMARY_USER, userName: "Organizer", content: "Love that, Sophia! Soft pinks and whites would be gorgeous with the vineyard backdrop.", replyTo: "msg_e3_03", createdAt: now - 50 * day + hr },
    { id: "msg_e3_05", userId: jessica, userName: "Jessica Lee", content: "Just voted for the golden hour ceremony — the photos will be incredible.", replyTo: null, createdAt: now - 40 * day },
    { id: "msg_e3_06", userId: olivia, userName: "Olivia Thompson", content: "Checked off the Le Creuset from the registry! Can't wait to see their faces.", replyTo: null, createdAt: now - 30 * day },
    { id: "msg_e3_07", userId: rachel, userName: "Rachel Parker", content: "Wedding favors ordered! Personalized honey jars with a custom label.", replyTo: null, createdAt: now - 25 * day },
    { id: "msg_e3_08", userId: james, userName: "James Anderson", content: "Wine pairing task is done. Went with a local Napa Cab and a Chardonnay.", replyTo: null, createdAt: now - 20 * day },
    { id: "msg_e3_09", userId: hannah, userName: "Hannah Ross", content: "Should I bring anything for the emergency kit? I have a sewing kit and Advil.", replyTo: null, createdAt: now - 10 * day },
    { id: "msg_e3_10", userId: PRIMARY_USER, userName: "Organizer", content: "Yes please, Hannah! Also hairspray and safety pins if you have them.", replyTo: "msg_e3_09", createdAt: now - 10 * day + hr },
  ];
  for (const m of e3msgs) await db.collection("events").doc(e3Id).collection("messages").doc(m.id).set(m);
  console.log("✅ E3: Lauren & David's Wedding — 8 modules");

  // ═══════════════════════════════════════════
  // EVENT 4: Rocky Mountain Weekend (ONGOING)
  // ═══════════════════════════════════════════
  const e4Id = "showcase_rocky_mountain";
  const e4Start = now - day;
  const e4 = {
    createdBy: PRIMARY_USER, name: "Rocky Mountain Weekend",
    description: "Camping, hiking, and stargazing in the Rockies. Bring warm layers and a sense of adventure.",
    theme: "výlet", templateSlug: "vylet",
    locationName: "Rocky Mountain NP, Estes Park CO", locationLat: 40.3428, locationLng: -105.6836,
    locationAddress: "1000 US-36, Estes Park, CO 80517",
    startDate: e4Start, endDate: now + day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "RKMTN", maxParticipants: 10, status: "ONGOING",
    enabledModules: ["EXPENSES", "CARPOOL", "ROOMS", "CHAT", "SCHEDULE", "PACKING_LIST"],
    securityEnabled: false, eventPin: "", hideFinancials: false, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: false,
    createdAt: now - 14 * day, updatedAt: now - hr,
  };
  await db.collection("events").doc(e4Id).set(e4);

  const e4users = [PRIMARY_USER, marcus, daniel, rachel, nathan, hannah];
  for (const uid of e4users) {
    await db.collection("events").doc(e4Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 12 * day,
    });
  }

  // E4 Expenses (3)
  const e4expenses = [
    { id: "exp_e4_01", d: { paidBy: PRIMARY_USER, description: "Campsite reservation (3 nights)", amount: 180, currency: "USD", category: "ubytování", receiptUrl: "", createdAt: now - 10 * day }, perPerson: 30 },
    { id: "exp_e4_02", d: { paidBy: nathan, description: "Groceries and firewood", amount: 95, currency: "USD", category: "jídlo", receiptUrl: "", createdAt: now - 2 * day }, perPerson: 15.83 },
    { id: "exp_e4_03", d: { paidBy: marcus, description: "Gas money (round trip)", amount: 120, currency: "USD", category: "doprava", receiptUrl: "", createdAt: now - day }, perPerson: 20 },
  ];
  for (const e of e4expenses) {
    await db.collection("events").doc(e4Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e4users.length; i++) {
      await db.collection("events").doc(e4Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e4users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E4 Carpool (2)
  await db.collection("events").doc(e4Id).collection("carpoolRides").doc("ride_e4_01").set({
    driverId: PRIMARY_USER, driverName: "Organizer", departureLocation: "Denver Union Station", departureLat: 39.7531, departureLng: -105.0002,
    departureTime: e4Start - 3 * hr, availableSeats: 3, notes: "Leaving at 6 AM sharp. Truck with roof rack.", type: "OFFER", isClosed: false, createdAt: now - 10 * day,
  });
  for (const p of [
    { id: "p_e4_01", userId: marcus, displayName: "Marcus Wright", status: "APPROVED", pickupLocation: "Union Station main entrance" },
    { id: "p_e4_02", userId: rachel, displayName: "Rachel Parker", status: "APPROVED", pickupLocation: "Union Station main entrance" },
  ]) await db.collection("events").doc(e4Id).collection("carpoolRides").doc("ride_e4_01").collection("passengers").doc(p.id).set(p);

  await db.collection("events").doc(e4Id).collection("carpoolRides").doc("ride_e4_02").set({
    driverId: nathan, driverName: "Nathan Brooks", departureLocation: "Boulder, CO", departureLat: 40.015, departureLng: -105.2705,
    departureTime: e4Start - 2 * hr, availableSeats: 2, notes: "Coming from Boulder, can fit gear in the back", type: "OFFER", isClosed: false, createdAt: now - 8 * day,
  });
  await db.collection("events").doc(e4Id).collection("carpoolRides").doc("ride_e4_02").collection("passengers").doc("p_e4_03").set({ userId: daniel, displayName: "Daniel Kim", status: "APPROVED", pickupLocation: "28th & Pearl" });

  // E4 Rooms (2 — tent assignments)
  const e4rooms = [
    { id: "room_e4_01", name: "Tent A — Basecamp Ridge", capacity: 3, notes: "4-person tent, near the fire pit", assignments: [PRIMARY_USER, marcus, nathan] },
    { id: "room_e4_02", name: "Tent B — Creekside", capacity: 3, notes: "3-person tent, next to the stream", assignments: [daniel, rachel, hannah] },
  ];
  for (const r of e4rooms) await db.collection("events").doc(e4Id).collection("rooms").doc(r.id).set(r);

  // E4 Schedule (6)
  const e4sched = [
    { id: "sch_e4_01", title: "Arrive & Set Up Camp", description: "Pitch tents, gather firewood", startTime: e4Start, endTime: e4Start + 2 * hr, location: "Moraine Park Campground" },
    { id: "sch_e4_02", title: "Afternoon Hike: Bear Lake Trail", description: "Moderate 3.6mi loop, bring water", startTime: e4Start + 3 * hr, endTime: e4Start + 6 * hr, location: "Bear Lake Trailhead" },
    { id: "sch_e4_03", title: "Campfire Dinner & S'mores", description: "Grilled sausages, roasted corn, s'mores", startTime: e4Start + 8 * hr, endTime: e4Start + 11 * hr, location: "Campsite fire ring" },
    { id: "sch_e4_04", title: "Sunrise at Trail Ridge Road", description: "Drive to alpine overlook for sunrise", startTime: now + 5 * hr, endTime: now + 8 * hr, location: "Trail Ridge Road Summit" },
    { id: "sch_e4_05", title: "Emerald Lake Hike", description: "Scenic 3.2mi round trip, possible snow", startTime: now + 9 * hr, endTime: now + 12 * hr, location: "Emerald Lake Trail" },
    { id: "sch_e4_06", title: "Pack Up & Head Home", description: "Break camp, clean up, group photo", startTime: now + day, endTime: now + day + 2 * hr, location: "Campsite" },
  ];
  for (const s of e4sched) await db.collection("events").doc(e4Id).collection("schedule").doc(s.id).set(s);

  // E4 Packing (10)
  const e4packing = [
    { id: "pack_e4_01", name: "Tent", isChecked: true, userId: PRIMARY_USER, addedBy: PRIMARY_USER, eventId: e4Id, createdAt: now - 10 * day },
    { id: "pack_e4_02", name: "Sleeping bag (rated 20°F)", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e4Id, createdAt: now - 10 * day },
    { id: "pack_e4_03", name: "Hiking boots", isChecked: true, userId: null, addedBy: marcus, eventId: e4Id, createdAt: now - 8 * day },
    { id: "pack_e4_04", name: "Headlamp + extra batteries", isChecked: true, userId: null, addedBy: nathan, eventId: e4Id, createdAt: now - 8 * day },
    { id: "pack_e4_05", name: "First aid kit", isChecked: true, userId: rachel, addedBy: rachel, eventId: e4Id, createdAt: now - 7 * day },
    { id: "pack_e4_06", name: "Bear spray", isChecked: true, userId: marcus, addedBy: marcus, eventId: e4Id, createdAt: now - 7 * day },
    { id: "pack_e4_07", name: "Camp stove + propane", isChecked: true, userId: nathan, addedBy: nathan, eventId: e4Id, createdAt: now - 6 * day },
    { id: "pack_e4_08", name: "Water filter", isChecked: false, userId: daniel, addedBy: daniel, eventId: e4Id, createdAt: now - 6 * day },
    { id: "pack_e4_09", name: "Warm layers (fleece + puffy jacket)", isChecked: false, userId: null, addedBy: hannah, eventId: e4Id, createdAt: now - 5 * day },
    { id: "pack_e4_10", name: "Marshmallows & chocolate", isChecked: true, userId: hannah, addedBy: hannah, eventId: e4Id, createdAt: now - 3 * day },
  ];
  for (const p of e4packing) await db.collection("events").doc(e4Id).collection("packingItems").doc(p.id).set(p);

  // E4 Chat (12 — live updates)
  const e4msgs = [
    { id: "msg_e4_01", userId: PRIMARY_USER, userName: "Organizer", content: "Rocky Mountain Weekend is GO! Weather looks perfect — clear skies all weekend.", replyTo: null, createdAt: now - 10 * day },
    { id: "msg_e4_02", userId: marcus, userName: "Marcus Wright", content: "Gear check! Who's bringing the camp stove?", replyTo: null, createdAt: now - 8 * day },
    { id: "msg_e4_03", userId: nathan, userName: "Nathan Brooks", content: "I've got the stove and propane covered.", replyTo: "msg_e4_02", createdAt: now - 8 * day + hr },
    { id: "msg_e4_04", userId: rachel, userName: "Rachel Parker", content: "I'll bring the first aid kit. Anyone have allergies I should know about?", replyTo: null, createdAt: now - 7 * day },
    { id: "msg_e4_05", userId: hannah, userName: "Hannah Ross", content: "I'm nut-free, already in my profile. Thanks Rachel!", replyTo: "msg_e4_04", createdAt: now - 7 * day + 2 * hr },
    { id: "msg_e4_06", userId: daniel, userName: "Daniel Kim", content: "Getting excited! Nathan, I'll ride with you from Boulder if that's cool.", replyTo: null, createdAt: now - 3 * day },
    { id: "msg_e4_07", userId: nathan, userName: "Nathan Brooks", content: "For sure! I'll pick you up at 28th & Pearl at 7 AM.", replyTo: "msg_e4_06", createdAt: now - 3 * day + hr },
    { id: "msg_e4_08", userId: PRIMARY_USER, userName: "Organizer", content: "We made it! Camp is set up, fire is going. This view is unreal.", replyTo: null, createdAt: now - 4 * hr },
    { id: "msg_e4_09", userId: marcus, userName: "Marcus Wright", content: "Bear Lake was stunning. My legs are toast though.", replyTo: null, createdAt: now - 2 * hr },
    { id: "msg_e4_10", userId: rachel, userName: "Rachel Parker", content: "S'mores situation is under control. Hannah brought the good chocolate.", replyTo: null, createdAt: now - hr },
    { id: "msg_e4_11", userId: hannah, userName: "Hannah Ross", content: "The stars out here are INCREDIBLE. No light pollution at all.", replyTo: null, createdAt: now - 30 * 60000 },
    { id: "msg_e4_12", userId: nathan, userName: "Nathan Brooks", content: "Sunrise hike tomorrow at 5:30 AM. Who's in?", replyTo: null, createdAt: now - 15 * 60000 },
  ];
  for (const m of e4msgs) await db.collection("events").doc(e4Id).collection("messages").doc(m.id).set(m);
  console.log("✅ E4: Rocky Mountain Weekend — 6 modules (ONGOING)");

  // ═══════════════════════════════════════════
  // EVENT 5: Rachel's 30th — Surprise!
  // ═══════════════════════════════════════════
  const e5Id = "showcase_rachels_30th";
  const e5Start = now + 10 * day;
  const e5 = {
    createdBy: PRIMARY_USER, name: "Rachel's 30th — Surprise!",
    description: "Surprise rooftop party for Rachel's 30th birthday. DO NOT tell her!",
    theme: "oslava", templateSlug: "oslava",
    locationName: "The Rooftop Lounge, Brooklyn NY", locationLat: 40.6782, locationLng: -73.9442,
    locationAddress: "250 Wythe Ave, Brooklyn, NY 11249",
    startDate: e5Start, endDate: e5Start + 6 * hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "RCH30", maxParticipants: 30, status: "CONFIRMED",
    enabledModules: ["VOTING", "EXPENSES", "CHAT", "SCHEDULE", "TASKS", "WISHLIST"],
    securityEnabled: true, eventPin: "2580", hideFinancials: false, screenshotProtection: true,
    autoDeleteDays: 0, requireApproval: false,
    createdAt: now - 21 * day, updatedAt: now - 2 * day,
  };
  await db.collection("events").doc(e5Id).set(e5);

  const e5users = [PRIMARY_USER, emily, sophia, olivia, daniel, hannah, jessica, ryan];
  for (const uid of e5users) {
    await db.collection("events").doc(e5Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: uid === ryan ? "maybe" : "accepted", joinedAt: now - 18 * day,
    });
  }

  // E5 Polls (2)
  await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_theme").set({
    createdBy: PRIMARY_USER, title: "Party Theme", pollType: "CUSTOM",
    allowMultiple: false, isAnonymous: false, deadline: e5Start - 5 * day, isClosed: false, createdAt: now - 18 * day,
  });
  const e5themeOpts = [
    { id: "opt_e5t1", label: "Gatsby / Roaring 20s", description: "Gold, black, feathers, jazz", sortOrder: 0 },
    { id: "opt_e5t2", label: "Tropical Paradise", description: "Leis, fruity drinks, bright colors", sortOrder: 1 },
    { id: "opt_e5t3", label: "Neon Glow Party", description: "UV lights, glow sticks, white outfits", sortOrder: 2 },
  ];
  for (const o of e5themeOpts) await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_theme").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e5t1", "vt1", PRIMARY_USER], ["opt_e5t3", "vt2", emily], ["opt_e5t1", "vt3", sophia], ["opt_e5t2", "vt4", olivia], ["opt_e5t1", "vt5", daniel], ["opt_e5t1", "vt6", hannah]]) {
    await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_theme").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_cake").set({
    createdBy: emily, title: "Cake Flavor", pollType: "CUSTOM",
    allowMultiple: false, isAnonymous: false, deadline: e5Start - 3 * day, isClosed: false, createdAt: now - 14 * day,
  });
  const e5cakeOpts = [
    { id: "opt_e5c1", label: "Red velvet with cream cheese", description: "Rachel's rumored favorite", sortOrder: 0 },
    { id: "opt_e5c2", label: "Chocolate ganache torte", description: "Rich and decadent", sortOrder: 1 },
    { id: "opt_e5c3", label: "Lemon raspberry layer cake", description: "Light and summery", sortOrder: 2 },
  ];
  for (const o of e5cakeOpts) await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_cake").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e5c1", "vck1", PRIMARY_USER], ["opt_e5c1", "vck2", sophia], ["opt_e5c2", "vck3", jessica], ["opt_e5c1", "vck4", hannah]]) {
    await db.collection("events").doc(e5Id).collection("polls").doc("poll_e5_cake").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // E5 Expenses (3)
  const e5expenses = [
    { id: "exp_e5_01", d: { paidBy: PRIMARY_USER, description: "Rooftop venue rental (4 hours)", amount: 1200, currency: "USD", category: "pronájem", receiptUrl: "", createdAt: now - 15 * day }, users: e5users, perPerson: 150 },
    { id: "exp_e5_02", d: { paidBy: emily, description: "Decorations & balloons", amount: 280, currency: "USD", category: "dekorace", receiptUrl: "", createdAt: now - 8 * day }, users: e5users, perPerson: 35 },
    { id: "exp_e5_03", d: { paidBy: sophia, description: "Custom birthday cake", amount: 120, currency: "USD", category: "jídlo", receiptUrl: "", createdAt: now - 5 * day }, users: e5users, perPerson: 15 },
  ];
  for (const e of e5expenses) {
    await db.collection("events").doc(e5Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e.users.length; i++) {
      await db.collection("events").doc(e5Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e.users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E5 Schedule (4)
  const e5sched = [
    { id: "sch_e5_01", title: "Setup & Decoration", description: "Arrive early, set up Gatsby theme decor", startTime: e5Start - 2 * hr, endTime: e5Start, location: "The Rooftop Lounge" },
    { id: "sch_e5_02", title: "SURPRISE! Rachel Arrives", description: "Everyone hide. Lights off until she walks in.", startTime: e5Start, endTime: e5Start + 30 * 60000, location: "Main entrance" },
    { id: "sch_e5_03", title: "Cocktails & Mingling", description: "Open bar, passed appetizers, photo booth", startTime: e5Start + 30 * 60000, endTime: e5Start + 3 * hr, location: "Rooftop terrace" },
    { id: "sch_e5_04", title: "Cake & Toasts", description: "Birthday cake, speeches from friends", startTime: e5Start + 3 * hr, endTime: e5Start + 4 * hr, location: "Main area" },
  ];
  for (const s of e5sched) await db.collection("events").doc(e5Id).collection("schedule").doc(s.id).set(s);

  // E5 Tasks (4)
  const e5tasks = [
    { id: "task_e5_01", name: "Distract Rachel on party day", color: "Růžová", assignedTo: [sophia], deadline: e5Start, isCompleted: false, createdAt: now - 18 * day },
    { id: "task_e5_02", name: "Order birthday banner", color: "Zlatá", assignedTo: [emily], deadline: e5Start - 5 * day, isCompleted: true, createdAt: now - 15 * day },
    { id: "task_e5_03", name: "Create birthday playlist", color: "Modrá", assignedTo: [daniel, jessica], deadline: e5Start - 3 * day, isCompleted: false, createdAt: now - 12 * day },
    { id: "task_e5_04", name: "Collect photos for slideshow", color: "Zelená", assignedTo: [hannah, olivia], deadline: e5Start - 2 * day, isCompleted: false, createdAt: now - 10 * day },
  ];
  for (const t of e5tasks) await db.collection("events").doc(e5Id).collection("tasks").doc(t.id).set(t);

  // E5 Wishlist (5)
  const e5wishlist = [
    { id: "wish_e5_01", name: "Spa gift card ($100)", price: "$100", productUrl: "", description: "She's been wanting a spa day", status: "BOUGHT", claimedBy: emily, claimedByName: "Emily Chen", addedBy: PRIMARY_USER, eventId: e5Id, createdAt: now - 16 * day },
    { id: "wish_e5_02", name: "Polaroid camera + film pack", price: "$80", productUrl: "", description: "Instant photos for memories", status: "RESERVED", claimedBy: jessica, claimedByName: "Jessica Lee", addedBy: sophia, eventId: e5Id, createdAt: now - 14 * day },
    { id: "wish_e5_03", name: "Personalized jewelry box", price: "$55", productUrl: "", description: "Engraved with her initials", status: "FREE", claimedBy: null, claimedByName: null, addedBy: hannah, eventId: e5Id, createdAt: now - 12 * day },
    { id: "wish_e5_04", name: "Cooking class voucher", price: "$90", productUrl: "", description: "Italian cooking, 2-person class", status: "FREE", claimedBy: null, claimedByName: null, addedBy: olivia, eventId: e5Id, createdAt: now - 10 * day },
    { id: "wish_e5_05", name: "Birthday scrapbook", price: "$25", productUrl: "", description: "Fill with photos and notes from friends", status: "RESERVED", claimedBy: hannah, claimedByName: "Hannah Ross", addedBy: PRIMARY_USER, eventId: e5Id, createdAt: now - 8 * day },
  ];
  for (const w of e5wishlist) await db.collection("events").doc(e5Id).collection("wishlistItems").doc(w.id).set(w);

  // E5 Chat (15)
  const e5msgs = [
    { id: "msg_e5_01", userId: PRIMARY_USER, userName: "Organizer", content: "Alright team, Rachel's 30th is happening! This is a SURPRISE — do NOT mention it around her.", replyTo: null, createdAt: now - 20 * day },
    { id: "msg_e5_02", userId: emily, userName: "Emily Chen", content: "My lips are sealed! I already have decoration ideas.", replyTo: null, createdAt: now - 20 * day + hr },
    { id: "msg_e5_03", userId: sophia, userName: "Sophia Martinez", content: "I can keep her busy on the day. We'll do a 'girls lunch' as cover.", replyTo: null, createdAt: now - 19 * day },
    { id: "msg_e5_04", userId: PRIMARY_USER, userName: "Organizer", content: "Perfect, Sophia! Bring her to the venue at exactly 7 PM.", replyTo: "msg_e5_03", createdAt: now - 19 * day + 2 * hr },
    { id: "msg_e5_05", userId: daniel, userName: "Daniel Kim", content: "Gatsby theme is winning the poll. I love it. Going all in on the 20s outfit.", replyTo: null, createdAt: now - 15 * day },
    { id: "msg_e5_06", userId: jessica, userName: "Jessica Lee", content: "Working on the playlist — any song requests? She loves 80s throwbacks too.", replyTo: null, createdAt: now - 12 * day },
    { id: "msg_e5_07", userId: hannah, userName: "Hannah Ross", content: "I started the birthday scrapbook. Send me your favorite photos with Rachel!", replyTo: null, createdAt: now - 10 * day },
    { id: "msg_e5_08", userId: olivia, userName: "Olivia Thompson", content: "Sent you 12 photos from our trip last summer, Hannah.", replyTo: "msg_e5_07", createdAt: now - 9 * day },
    { id: "msg_e5_09", userId: PRIMARY_USER, userName: "Organizer", content: "Cake poll results: red velvet wins! Sophia is placing the order.", replyTo: null, createdAt: now - 7 * day },
    { id: "msg_e5_10", userId: sophia, userName: "Sophia Martinez", content: "Cake ordered! Three layers, cream cheese frosting, gold sparkler topper.", replyTo: "msg_e5_09", createdAt: now - 7 * day + hr },
    { id: "msg_e5_11", userId: ryan, userName: "Ryan Campbell", content: "90% sure I can make it. Checking my flight schedule.", replyTo: null, createdAt: now - 5 * day },
    { id: "msg_e5_12", userId: emily, userName: "Emily Chen", content: "Decorations arrived! Gold balloons, sequin tablecloths, feather centerpieces.", replyTo: null, createdAt: now - 4 * day },
    { id: "msg_e5_13", userId: PRIMARY_USER, userName: "Organizer", content: "Venue confirmed for setup at 5 PM. Who can come early to help?", replyTo: null, createdAt: now - 3 * day },
    { id: "msg_e5_14", userId: daniel, userName: "Daniel Kim", content: "I'll be there at 5. I can handle the sound system.", replyTo: "msg_e5_13", createdAt: now - 3 * day + hr },
    { id: "msg_e5_15", userId: hannah, userName: "Hannah Ross", content: "Scrapbook is done! She's going to cry. In the best way.", replyTo: null, createdAt: now - day },
  ];
  for (const m of e5msgs) await db.collection("events").doc(e5Id).collection("messages").doc(m.id).set(m);
  console.log("✅ E5: Rachel's 30th — 6 modules + security");

  // ═══════════════════════════════════════════
  // EVENT 6: Soundwave Festival Crew
  // ═══════════════════════════════════════════
  const e6Id = "showcase_soundwave_festival";
  const e6Start = now + 28 * day;
  const e6 = {
    createdBy: PRIMARY_USER, name: "Soundwave Festival Crew",
    description: "Three-day music festival at Zilker Park. Camping, live bands, and good vibes only.",
    theme: "festival", templateSlug: "festival",
    locationName: "Zilker Park, Austin TX", locationLat: 30.2669, locationLng: -97.7729,
    locationAddress: "2100 Barton Springs Rd, Austin, TX 78704",
    startDate: e6Start, endDate: e6Start + 3 * day, datesFinalized: true,
    coverImageUrl: "", inviteCode: "SNDWV", maxParticipants: 15, status: "CONFIRMED",
    enabledModules: ["VOTING", "EXPENSES", "CARPOOL", "CHAT", "SCHEDULE", "PACKING_LIST"],
    securityEnabled: false, eventPin: "", hideFinancials: false, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: false,
    moduleColors: {
      SCHEDULE: "#8B5CF6", CHAT: "#E879B8", PACKING_LIST: "#34D399",
    },
    createdAt: now - 20 * day, updatedAt: now - 2 * day,
  };
  await db.collection("events").doc(e6Id).set(e6);

  const e6users = [PRIMARY_USER, emily, marcus, sophia, alex, jessica, ryan, nathan];
  for (const uid of e6users) {
    await db.collection("events").doc(e6Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 18 * day,
    });
  }

  // E6 Poll (1)
  await db.collection("events").doc(e6Id).collection("polls").doc("poll_e6_headliner").set({
    createdBy: PRIMARY_USER, title: "Which headliner are we seeing Saturday?", pollType: "CUSTOM",
    allowMultiple: false, isAnonymous: false, deadline: e6Start - 3 * day, isClosed: false, createdAt: now - 15 * day,
  });
  const e6opts = [
    { id: "opt_e6h1", label: "The Midnight", description: "Synthwave, 9 PM main stage", sortOrder: 0 },
    { id: "opt_e6h2", label: "Khruangbin", description: "Psychedelic funk, 10 PM east stage", sortOrder: 1 },
    { id: "opt_e6h3", label: "Split up — see different sets", description: "Meet back at camp after", sortOrder: 2 },
  ];
  for (const o of e6opts) await db.collection("events").doc(e6Id).collection("polls").doc("poll_e6_headliner").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e6h2", "vh1", PRIMARY_USER], ["opt_e6h1", "vh2", emily], ["opt_e6h2", "vh3", marcus], ["opt_e6h2", "vh4", sophia], ["opt_e6h1", "vh5", alex], ["opt_e6h3", "vh6", jessica]]) {
    await db.collection("events").doc(e6Id).collection("polls").doc("poll_e6_headliner").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // E6 Expenses (4)
  const e6expenses = [
    { id: "exp_e6_01", d: { paidBy: PRIMARY_USER, description: "Festival tickets (8 x 3-day pass)", amount: 2400, currency: "USD", category: "aktivity", receiptUrl: "", createdAt: now - 18 * day }, perPerson: 300 },
    { id: "exp_e6_02", d: { paidBy: marcus, description: "Camping gear rental (tents + chairs)", amount: 320, currency: "USD", category: "ubytování", receiptUrl: "", createdAt: now - 12 * day }, perPerson: 40 },
    { id: "exp_e6_03", d: { paidBy: alex, description: "Cooler + ice + drinks for 3 days", amount: 180, currency: "USD", category: "nápoje", receiptUrl: "", createdAt: now - 8 * day }, perPerson: 22.5 },
    { id: "exp_e6_04", d: { paidBy: jessica, description: "Matching bandanas (crew merch)", amount: 64, currency: "USD", category: "dekorace", receiptUrl: "", createdAt: now - 5 * day }, perPerson: 8 },
  ];
  for (const e of e6expenses) {
    await db.collection("events").doc(e6Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e6users.length; i++) {
      await db.collection("events").doc(e6Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e6users[i], amount: e.perPerson, isSettled: false });
    }
  }

  // E6 Carpool (2)
  await db.collection("events").doc(e6Id).collection("carpoolRides").doc("ride_e6_01").set({
    driverId: PRIMARY_USER, driverName: "Organizer", departureLocation: "Downtown Austin", departureLat: 30.267, departureLng: -97.743,
    departureTime: e6Start - 2 * hr, availableSeats: 4, notes: "Truck with all the camping gear loaded", type: "OFFER", isClosed: false, createdAt: now - 10 * day,
  });
  for (const p of [
    { id: "p_e6_01", userId: emily, displayName: "Emily Chen", status: "APPROVED", pickupLocation: "Congress Ave Hotel" },
    { id: "p_e6_02", userId: sophia, displayName: "Sophia Martinez", status: "APPROVED", pickupLocation: "Congress Ave Hotel" },
    { id: "p_e6_03", userId: alex, displayName: "Alex Rivera", status: "APPROVED", pickupLocation: "South Lamar" },
  ]) await db.collection("events").doc(e6Id).collection("carpoolRides").doc("ride_e6_01").collection("passengers").doc(p.id).set(p);

  await db.collection("events").doc(e6Id).collection("carpoolRides").doc("ride_e6_02").set({
    driverId: ryan, driverName: "Ryan Campbell", departureLocation: "San Antonio, TX", departureLat: 29.4241, departureLng: -98.4936,
    departureTime: e6Start - 3 * hr, availableSeats: 3, notes: "Driving up from San Antonio, happy to carpool", type: "OFFER", isClosed: false, createdAt: now - 8 * day,
  });
  await db.collection("events").doc(e6Id).collection("carpoolRides").doc("ride_e6_02").collection("passengers").doc("p_e6_04").set({ userId: nathan, displayName: "Nathan Brooks", status: "APPROVED", pickupLocation: "San Marcos outlet" });

  // E6 Schedule (8)
  const e6sched = [
    { id: "sch_e6_01", title: "Arrive & Set Up Camp", description: "Find our spot, pitch tents, crack open cold ones", startTime: e6Start, endTime: e6Start + 2 * hr, location: "Festival Campgrounds Lot C" },
    { id: "sch_e6_02", title: "Day 1 — Opening Acts", description: "Explore stages, grab food truck lunch", startTime: e6Start + 3 * hr, endTime: e6Start + 7 * hr, location: "Various stages" },
    { id: "sch_e6_03", title: "Day 1 — Evening Sets", description: "Main stage headliners + late night DJ tent", startTime: e6Start + 9 * hr, endTime: e6Start + 14 * hr, location: "Main Stage & DJ Tent" },
    { id: "sch_e6_04", title: "Day 2 — Morning Yoga", description: "Free festival yoga session to start the day", startTime: e6Start + day + 2 * hr, endTime: e6Start + day + 3 * hr, location: "Wellness Area" },
    { id: "sch_e6_05", title: "Day 2 — Afternoon Discovery", description: "Check out smaller stages and local artists", startTime: e6Start + day + 5 * hr, endTime: e6Start + day + 8 * hr, location: "Discovery Stage" },
    { id: "sch_e6_06", title: "Day 2 — Saturday Headliner", description: "See poll results — group decision!", startTime: e6Start + day + 10 * hr, endTime: e6Start + day + 13 * hr, location: "TBD (see poll)" },
    { id: "sch_e6_07", title: "Day 3 — Farewell Brunch", description: "Cook breakfast at camp, share highlights", startTime: e6Start + 2 * day + 2 * hr, endTime: e6Start + 2 * day + 4 * hr, location: "Campsite" },
    { id: "sch_e6_08", title: "Day 3 — Final Sets & Pack Up", description: "Catch closing acts, break camp, head home", startTime: e6Start + 2 * day + 5 * hr, endTime: e6Start + 2 * day + 10 * hr, location: "Main Stage & Campgrounds" },
  ];
  for (const s of e6sched) await db.collection("events").doc(e6Id).collection("schedule").doc(s.id).set(s);

  // E6 Packing (8)
  const e6packing = [
    { id: "pack_e6_01", name: "Festival tickets (on phone)", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e6Id, createdAt: now - 15 * day },
    { id: "pack_e6_02", name: "Tent + sleeping pad", isChecked: false, userId: null, addedBy: marcus, eventId: e6Id, createdAt: now - 12 * day },
    { id: "pack_e6_03", name: "Portable phone charger", isChecked: true, userId: null, addedBy: emily, eventId: e6Id, createdAt: now - 12 * day },
    { id: "pack_e6_04", name: "Earplugs (for sleeping)", isChecked: false, userId: null, addedBy: sophia, eventId: e6Id, createdAt: now - 10 * day },
    { id: "pack_e6_05", name: "Sunscreen + hat", isChecked: true, userId: null, addedBy: PRIMARY_USER, eventId: e6Id, createdAt: now - 10 * day },
    { id: "pack_e6_06", name: "Refillable water bottle", isChecked: true, userId: null, addedBy: alex, eventId: e6Id, createdAt: now - 8 * day },
    { id: "pack_e6_07", name: "Cash for merch/food", isChecked: false, userId: null, addedBy: jessica, eventId: e6Id, createdAt: now - 7 * day },
    { id: "pack_e6_08", name: "Rain poncho (just in case)", isChecked: false, userId: nathan, addedBy: nathan, eventId: e6Id, createdAt: now - 5 * day },
  ];
  for (const p of e6packing) await db.collection("events").doc(e6Id).collection("packingItems").doc(p.id).set(p);

  // E6 Chat (10)
  const e6msgs = [
    { id: "msg_e6_01", userId: PRIMARY_USER, userName: "Organizer", content: "Soundwave Festival crew is assembled! Tickets are bought, camping is booked.", replyTo: null, createdAt: now - 18 * day },
    { id: "msg_e6_02", userId: emily, userName: "Emily Chen", content: "I've been waiting for this all year! The lineup is insane.", replyTo: null, createdAt: now - 17 * day },
    { id: "msg_e6_03", userId: marcus, userName: "Marcus Wright", content: "I'll handle the camping gear rental. Who has a tent already?", replyTo: null, createdAt: now - 15 * day },
    { id: "msg_e6_04", userId: sophia, userName: "Sophia Martinez", content: "Vote on the Saturday headliner! Khruangbin vs The Midnight.", replyTo: null, createdAt: now - 12 * day },
    { id: "msg_e6_05", userId: alex, userName: "Alex Rivera", content: "I'm bringing a massive cooler. Everyone chip in for drinks and ice.", replyTo: null, createdAt: now - 10 * day },
    { id: "msg_e6_06", userId: jessica, userName: "Jessica Lee", content: "Got matching bandanas for the crew! Teal with the Soundwave logo.", replyTo: null, createdAt: now - 8 * day },
    { id: "msg_e6_07", userId: ryan, userName: "Ryan Campbell", content: "Driving up from San Antonio. Nathan, want to carpool?", replyTo: null, createdAt: now - 6 * day },
    { id: "msg_e6_08", userId: nathan, userName: "Nathan Brooks", content: "Yes! Pick me up at San Marcos on the way.", replyTo: "msg_e6_07", createdAt: now - 6 * day + hr },
    { id: "msg_e6_09", userId: PRIMARY_USER, userName: "Organizer", content: "Packing list is up. Don't forget sunscreen — it's Texas in spring.", replyTo: null, createdAt: now - 4 * day },
    { id: "msg_e6_10", userId: emily, userName: "Emily Chen", content: "Four weeks out. This is going to be the best festival yet!", replyTo: null, createdAt: now - 2 * day },
  ];
  for (const m of e6msgs) await db.collection("events").doc(e6Id).collection("messages").doc(m.id).set(m);
  console.log("✅ E6: Soundwave Festival Crew — 6 modules");

  // ═══════════════════════════════════════════
  // EVENT 7: Hearts & Hands Charity Gala (COMPLETED)
  // ═══════════════════════════════════════════
  const e7Id = "showcase_charity_gala";
  const e7Start = now - 14 * day;
  const e7 = {
    createdBy: PRIMARY_USER, name: "Hearts & Hands Charity Gala",
    description: "Annual black-tie fundraiser benefiting local children's education programs. Silent auction, dinner, and live entertainment.",
    theme: "firemní", templateSlug: "firemni",
    locationName: "The Metropolitan Club, Washington DC", locationLat: 38.9072, locationLng: -77.0369,
    locationAddress: "1700 H St NW, Washington, DC 20006",
    startDate: e7Start, endDate: e7Start + 6 * hr, datesFinalized: true,
    coverImageUrl: "", inviteCode: "HHGLA", maxParticipants: 100, status: "COMPLETED",
    enabledModules: ["VOTING", "EXPENSES", "CHAT", "SCHEDULE", "TASKS", "BUDGET"],
    securityEnabled: true, eventPin: "9301", hideFinancials: true, screenshotProtection: false,
    autoDeleteDays: 0, requireApproval: true,
    moduleColors: {
      BUDGET: "#5B5FEF", SCHEDULE: "#E879B8", TASKS: "#34D399",
    },
    createdAt: now - 60 * day, updatedAt: now - 13 * day,
  };
  await db.collection("events").doc(e7Id).set(e7);

  const e7users = [PRIMARY_USER, emily, sophia, james, olivia, daniel, rachel, jessica, ryan, nathan];
  for (const uid of e7users) {
    await db.collection("events").doc(e7Id).collection("participants").doc(uid).set({
      userId: uid, role: uid === PRIMARY_USER ? "organizer" : "participant",
      rsvp: "accepted", joinedAt: now - 55 * day,
    });
  }

  // E7 Poll (1 — closed)
  await db.collection("events").doc(e7Id).collection("polls").doc("poll_e7_auction").set({
    createdBy: PRIMARY_USER, title: "Silent Auction Highlight Item", pollType: "CUSTOM",
    allowMultiple: false, isAnonymous: false, deadline: e7Start - 7 * day, isClosed: true, createdAt: now - 45 * day,
  });
  const e7opts = [
    { id: "opt_e7a1", label: "Weekend getaway package", description: "2 nights at a luxury resort", sortOrder: 0 },
    { id: "opt_e7a2", label: "Private chef dinner for 8", description: "In-home 5-course meal", sortOrder: 1 },
    { id: "opt_e7a3", label: "Original artwork by local artist", description: "Commissioned piece, oil on canvas", sortOrder: 2 },
  ];
  for (const o of e7opts) await db.collection("events").doc(e7Id).collection("polls").doc("poll_e7_auction").collection("options").doc(o.id).set(o);
  for (const [optId, vId, uid] of [["opt_e7a1", "va1", PRIMARY_USER], ["opt_e7a2", "va2", emily], ["opt_e7a1", "va3", james], ["opt_e7a2", "va4", olivia], ["opt_e7a1", "va5", daniel], ["opt_e7a3", "va6", jessica], ["opt_e7a1", "va7", ryan]]) {
    await db.collection("events").doc(e7Id).collection("polls").doc("poll_e7_auction").collection("options").doc(optId).collection("votes").doc(vId).set({ userId: uid, value: 1 });
  }

  // E7 Expenses (4)
  const e7expenses = [
    { id: "exp_e7_01", d: { paidBy: PRIMARY_USER, description: "Metropolitan Club venue rental", amount: 5500, currency: "USD", category: "pronájem", receiptUrl: "", createdAt: now - 50 * day }, users: e7users, perPerson: 550 },
    { id: "exp_e7_02", d: { paidBy: james, description: "Catering — plated dinner (50 guests)", amount: 4200, currency: "USD", category: "catering", receiptUrl: "", createdAt: now - 30 * day }, users: e7users, perPerson: 420 },
    { id: "exp_e7_03", d: { paidBy: emily, description: "Floral centerpieces & decor", amount: 1800, currency: "USD", category: "dekorace", receiptUrl: "", createdAt: now - 25 * day }, users: e7users, perPerson: 180 },
    { id: "exp_e7_04", d: { paidBy: olivia, description: "Live jazz quartet (3 hours)", amount: 2400, currency: "USD", category: "aktivity", receiptUrl: "", createdAt: now - 20 * day }, users: e7users, perPerson: 240 },
  ];
  for (const e of e7expenses) {
    await db.collection("events").doc(e7Id).collection("expenses").doc(e.id).set(e.d);
    for (let i = 0; i < e.users.length; i++) {
      await db.collection("events").doc(e7Id).collection("expenses").doc(e.id).collection("splits").doc(`s_${e.id}_${i}`).set({ userId: e.users[i], amount: e.perPerson, isSettled: true });
    }
  }

  // E7 Schedule (5)
  const e7sched = [
    { id: "sch_e7_01", title: "Guest Arrival & Welcome Reception", description: "Champagne, hors d'oeuvres, networking", startTime: e7Start, endTime: e7Start + hr, location: "Grand Foyer" },
    { id: "sch_e7_02", title: "Silent Auction Opens", description: "Browse items, place bids via tablet", startTime: e7Start + hr, endTime: e7Start + 3 * hr, location: "Gallery Hall" },
    { id: "sch_e7_03", title: "Seated Dinner & Keynote", description: "Three-course meal, keynote from foundation director", startTime: e7Start + 2 * hr, endTime: e7Start + 4 * hr, location: "Main Dining Room" },
    { id: "sch_e7_04", title: "Live Auction & Paddle Raise", description: "Top 5 items, live auctioneer, fund-a-need", startTime: e7Start + 4 * hr, endTime: e7Start + 5 * hr, location: "Main Stage" },
    { id: "sch_e7_05", title: "Dancing & After-Party", description: "Live jazz, dessert bar, open dance floor", startTime: e7Start + 5 * hr, endTime: e7Start + 7 * hr, location: "Terrace Ballroom" },
  ];
  for (const s of e7sched) await db.collection("events").doc(e7Id).collection("schedule").doc(s.id).set(s);

  // E7 Tasks (5 — all completed)
  const e7tasks = [
    { id: "task_e7_01", name: "Secure venue contract", color: "Modrá", assignedTo: [PRIMARY_USER], deadline: e7Start - 30 * day, isCompleted: true, createdAt: now - 55 * day },
    { id: "task_e7_02", name: "Solicit auction donations", color: "Zlatá", assignedTo: [james, olivia], deadline: e7Start - 21 * day, isCompleted: true, createdAt: now - 50 * day },
    { id: "task_e7_03", name: "Design & print event programs", color: "Růžová", assignedTo: [sophia], deadline: e7Start - 7 * day, isCompleted: true, createdAt: now - 40 * day },
    { id: "task_e7_04", name: "Coordinate volunteer assignments", color: "Zelená", assignedTo: [rachel, daniel], deadline: e7Start - 3 * day, isCompleted: true, createdAt: now - 35 * day },
    { id: "task_e7_05", name: "Send thank-you notes to donors", color: "Oranžová", assignedTo: [emily], deadline: e7Start + 7 * day, isCompleted: true, createdAt: now - 15 * day },
  ];
  for (const t of e7tasks) await db.collection("events").doc(e7Id).collection("tasks").doc(t.id).set(t);

  // E7 Budget (4 categories)
  const e7budgetCats = [
    { id: "bcat_e7_01", name: "Venue & Rentals", planned: 6000, actualAmount: 5500, fromTemplate: false, createdAt: now - 55 * day },
    { id: "bcat_e7_02", name: "Catering & Bar", planned: 5000, actualAmount: 4200, fromTemplate: false, createdAt: now - 55 * day },
    { id: "bcat_e7_03", name: "Decor & Florals", planned: 2000, actualAmount: 1800, fromTemplate: false, createdAt: now - 55 * day },
    { id: "bcat_e7_04", name: "Entertainment", planned: 3000, actualAmount: 2400, fromTemplate: false, createdAt: now - 55 * day },
  ];
  for (const c of e7budgetCats) await db.collection("events").doc(e7Id).collection("budgetCategories").doc(c.id).set(c);
  const e7budgetExps = [
    { catId: "bcat_e7_01", id: "bexp_e7_01", amount: 5500, note: "Metropolitan Club rental", addedByName: "Organizer", createdAt: now - 50 * day },
    { catId: "bcat_e7_02", id: "bexp_e7_02", amount: 4200, note: "Plated dinner catering", addedByName: "James Anderson", createdAt: now - 30 * day },
    { catId: "bcat_e7_03", id: "bexp_e7_03", amount: 1800, note: "Floral centerpieces", addedByName: "Emily Chen", createdAt: now - 25 * day },
    { catId: "bcat_e7_04", id: "bexp_e7_04", amount: 2400, note: "Jazz quartet", addedByName: "Olivia Thompson", createdAt: now - 20 * day },
  ];
  for (const e of e7budgetExps) await db.collection("events").doc(e7Id).collection("budgetCategories").doc(e.catId).collection("expenses").doc(e.id).set({ id: e.id, amount: e.amount, note: e.note, addedByName: e.addedByName, createdAt: e.createdAt });

  // E7 Chat (8)
  const e7msgs = [
    { id: "msg_e7_01", userId: PRIMARY_USER, userName: "Organizer", content: "Welcome to the Hearts & Hands Gala planning group! Let's make this the best year yet.", replyTo: null, createdAt: now - 55 * day },
    { id: "msg_e7_02", userId: james, userName: "James Anderson", content: "Catering contract is signed. Menu looks incredible.", replyTo: null, createdAt: now - 30 * day },
    { id: "msg_e7_03", userId: olivia, userName: "Olivia Thompson", content: "Jazz quartet is confirmed! They'll play during dinner and the after-party.", replyTo: null, createdAt: now - 20 * day },
    { id: "msg_e7_04", userId: sophia, userName: "Sophia Martinez", content: "Programs are at the printer. Gold foil on ivory stock — they look stunning.", replyTo: null, createdAt: now - 18 * day },
    { id: "msg_e7_05", userId: PRIMARY_USER, userName: "Organizer", content: "Auction items are incredible this year. 23 donations and counting!", replyTo: null, createdAt: now - 16 * day },
    { id: "msg_e7_06", userId: rachel, userName: "Rachel Parker", content: "Volunteer schedule is set. 12 helpers confirmed for the night.", replyTo: null, createdAt: now - 15 * day },
    { id: "msg_e7_07", userId: PRIMARY_USER, userName: "Organizer", content: "What an incredible night! We raised $47,000 for the foundation. Thank you all!", replyTo: null, createdAt: now - 13 * day },
    { id: "msg_e7_08", userId: emily, userName: "Emily Chen", content: "Thank-you notes are all sent. 87 donors received personalized cards.", replyTo: null, createdAt: now - 8 * day },
  ];
  for (const m of e7msgs) await db.collection("events").doc(e7Id).collection("messages").doc(m.id).set(m);

  // E7 Ratings (8)
  const e7ratings = [
    { id: rachel, userId: rachel, overallRating: 5, organizationRating: 5, atmosphereRating: 5, venueRating: 5, comment: "Absolutely flawless evening. The venue was breathtaking.", createdAt: now - 13 * day },
    { id: james, userId: james, overallRating: 5, organizationRating: 5, atmosphereRating: 4, venueRating: 5, comment: "Superb organization. The auction was a highlight.", createdAt: now - 13 * day },
    { id: emily, userId: emily, overallRating: 5, organizationRating: 5, atmosphereRating: 5, venueRating: 4, comment: "Best gala yet! The jazz quartet was perfection.", createdAt: now - 12 * day },
    { id: olivia, userId: olivia, overallRating: 4, organizationRating: 4, atmosphereRating: 5, venueRating: 5, comment: "Wonderful atmosphere. Proud of what we raised!", createdAt: now - 12 * day },
    { id: daniel, userId: daniel, overallRating: 5, organizationRating: 5, atmosphereRating: 5, venueRating: 5, comment: "Incredible event from start to finish.", createdAt: now - 12 * day },
    { id: sophia, userId: sophia, overallRating: 4, organizationRating: 5, atmosphereRating: 4, venueRating: 4, comment: "Beautifully executed. The programs came out great!", createdAt: now - 11 * day },
    { id: jessica, userId: jessica, overallRating: 5, organizationRating: 4, atmosphereRating: 5, venueRating: 5, comment: "Magical evening. Already looking forward to next year.", createdAt: now - 11 * day },
    { id: ryan, userId: ryan, overallRating: 4, organizationRating: 4, atmosphereRating: 4, venueRating: 5, comment: "Great cause, great people, great night.", createdAt: now - 10 * day },
  ];
  for (const r of e7ratings) await db.collection("events").doc(e7Id).collection("ratings").doc(r.id).set(r);
  console.log("✅ E7: Hearts & Hands Charity Gala — 7 modules + security + ratings (COMPLETED)");

  // ═══ SUMMARY ═══
  console.log("\n🎉 All done!");
  console.log(`   User ${PRIMARY_USER} has Business plan until ${new Date(now + 365 * day).toLocaleDateString("en-US")}`);
  console.log("   13 users, 7 events");
  console.log("   10 polls, 24 expenses, 7 carpool rides, 10 rooms");
  console.log("   42 schedule blocks, 22 tasks, 40 packing items");
  console.log("   11 budget categories, 20 wishlist items");
  console.log("   77 chat messages, 8 ratings, 4 activity log entries");
  process.exit(0);
}

seed().catch(err => {
  console.error("❌ Error:", err.message);
  process.exit(1);
});
