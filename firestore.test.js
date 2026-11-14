const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const pastTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, age = 25) {
  return {
    userId: uid,
    displayName: "Alice",
    age,
    heightCm: 170,
    weightKg: 68.5,
    activityLevel: "moderate",
    fitnessGoal: "fat_loss",
    nutritionPreference: "mediterranean",
    experienceLevel: "beginner",
    healthNotes: "",
    parqAnswers: [false, false, false, false, false, false, false],
    parqFlagged: false,
    selectedPlanDurationWeeks: 8,
    preferredLanguage: "ar",
    themeMode: "system",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or exercises", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("exercises").get());
  await assertFails(unauthDb.collection("coachRequests").get());
});

test("Under-18 user registration is strictly blocked by Firestore rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, 17))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, 18))
  );
});

test("Shadow update with ghost role/isAdmin field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = { ...validProfilePayload(ALICE_UID, 26), isAdmin: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(payload));
});

test("Cross-user isolation blocks Bob from reading Alice profile, logs, and plans", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, 28));
    await db.collection("users").doc(ALICE_UID).collection("workoutLogs").doc("log1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-14",
      workoutTitle: "Full Body",
      durationMinutes: 35,
      caloriesBurned: 260,
      completed: true,
      createdAt: pastTimestamp(),
    });
    await db.collection("coachRequests").doc("req1").set({
      userId: ALICE_UID,
      userDisplayName: "Alice",
      goalSummary: "Strength",
      preferredContactMethod: "in_app_note",
      status: "pending",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workoutLogs").get());
  await assertFails(bobDb.collection("coachRequests").doc("req1").get());
  await assertFails(bobDb.collection("coachRequests").get());
  await assertSucceeds(
    bobDb.collection("coachRequests").where("userId", "==", BOB_UID).get()
  );
});

test("User cannot self-approve AI fitness plan", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const planBase = {
    userId: ALICE_UID,
    durationWeeks: 8,
    title: "8-Week Lean Plan",
    summary: "Balanced routine",
    weeklyScheduleJson: "[]",
    dailyMealsJson: "[]",
    reviewStatus: "approved",
    version: 1,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("plans").doc("p1").set(planBase)
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("plans")
      .doc("p1")
      .set({ ...planBase, reviewStatus: "pending_review" })
  );
});

test("Email spoofing test: unverified admin email cannot write exercises or auditLogs", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "uni.adf2279@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("exercises").doc("ex1").set({
      nameEn: "Squat",
      nameAr: "سكوات",
      descriptionEn: "Lower body compound",
      descriptionAr: "تمرين للجزء السفلي",
      targetMusclesEn: "Quads, Glutes",
      targetMusclesAr: "الأفخاذ والأرداف",
      difficulty: "beginner",
      repsOrDuration: "3x12",
      equipmentEn: "Bodyweight",
      equipmentAr: "وزن الجسم",
      instructionsEn: "Lower hips",
      instructionsAr: "اخفض الوركين",
      mistakesEn: "Knees caving",
      mistakesAr: "دخول الركبتين",
      animationType: "squat",
      isActive: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Verified admin can manage exercises, approve plans, and append immutable audit logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("plans").doc("p1").set({
      userId: ALICE_UID,
      durationWeeks: 8,
      title: "8-Week Plan",
      summary: "Summary",
      weeklyScheduleJson: "[]",
      dailyMealsJson: "[]",
      reviewStatus: "pending_review",
      version: 1,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "uni.adf2279@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("exercises").doc("ex1").set({
      nameEn: "Squat",
      nameAr: "سكوات",
      descriptionEn: "Lower body compound",
      descriptionAr: "تمرين للجزء السفلي",
      targetMusclesEn: "Quads, Glutes",
      targetMusclesAr: "الأفخاذ والأرداف",
      difficulty: "beginner",
      repsOrDuration: "3x12",
      equipmentEn: "Bodyweight",
      equipmentAr: "وزن الجسم",
      instructionsEn: "Lower hips",
      instructionsAr: "اخفض الوركين",
      mistakesEn: "Knees caving",
      mistakesAr: "دخول الركبتين",
      animationType: "squat",
      isActive: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("plans").doc("p1").update({
      reviewStatus: "approved",
      adminFeedback: "Looks balanced and safe.",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("auditLogs").doc("log1").set({
      actorUid: ADMIN_UID,
      actorEmail: "uni.adf2279@gmail.com",
      action: "APPROVE_PLAN",
      targetCollection: "plans",
      targetId: "p1",
      summary: "Approved Alice's 8-week plan",
      version: 1,
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(
    adminDb.collection("auditLogs").doc("log1").update({ summary: "Tampered" })
  );
  await assertFails(adminDb.collection("auditLogs").doc("log1").delete());
});
