const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_UID = "aaWqMj9EgnWbZbX3skVUMOgpgRq2";
const ADMIN_UID = "admin_user_001";
const STUDENT_ALICE = "student_alice_123";
const STUDENT_BOB = "student_bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("subjects").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("questions_public").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("questionAnswers_private").doc("q1").get());
  await assertFails(unauthDb.collection("users").doc(STUDENT_ALICE).get());
});

test("Student registration defaults to student role and blocks self-promotion", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE).firestore();
  const now = pastDate();

  // Valid student registration
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_ALICE).set({
      uid: STUDENT_ALICE,
      name: "রহিম উদ্দিন",
      email: "rahim@example.com",
      role: "student",
      preferredLanguage: "bn",
      totalExams: 0,
      totalAttempted: 0,
      totalCorrect: 0,
      totalIncorrect: 0,
      bestScore: 0,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Blocked self-escalation to admin or super_admin on create
  const bobDb = testEnv.authenticatedContext(STUDENT_BOB).firestore();
  await assertFails(
    bobDb.collection("users").doc(STUDENT_BOB).set({
      uid: STUDENT_BOB,
      name: "Bob",
      email: "bob@example.com",
      role: "admin",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student can update permitted profile fields
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_ALICE).update({
      name: "রহিম উদ্দিন আহমেদ",
      phone: "01711000000",
      preferredLanguage: "en",
      updatedAt: pastDate(500),
    })
  );

  // Student CANNOT escalate role or forge exam stats on update
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_ALICE).update({
      role: "admin",
      updatedAt: pastDate(400),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_ALICE).update({
      bestScore: 100,
      updatedAt: pastDate(400),
    })
  );
});

test("Only Super Admin UID can manage Admin records and App Settings", async () => {
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE).firestore();
  const now = pastDate();

  // Student cannot create admin record
  await assertFails(
    aliceDb.collection("admins").doc(STUDENT_ALICE).set({
      uid: STUDENT_ALICE,
      email: "alice@example.com",
      name: "Alice",
      permissions: ["question_publish"],
      assignedBy: STUDENT_ALICE,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Super Admin can create admin record with granular permissions
  await assertSucceeds(
    superDb.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@example.com",
      name: "Teacher Admin",
      permissions: ["pdf_upload", "question_create", "question_edit", "question_publish", "subject_manage"],
      assignedBy: SUPER_ADMIN_UID,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Super Admin can manage appSettings; regular admin/student cannot
  await assertFails(
    aliceDb.collection("appSettings").doc("global").set({
      maintenanceMode: true,
      allowRegistration: false,
      status: "active",
      updatedBy: STUDENT_ALICE,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    superDb.collection("appSettings").doc("global").set({
      maintenanceMode: false,
      allowRegistration: true,
      defaultNegativeMarking: 0.5,
      announcementBanner: "BCS প্রস্তুতি শুরু হয়েছে",
      status: "active",
      updatedBy: SUPER_ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Private answer keys are strictly hidden from students and accessible to admins", async () => {
  const now = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      permissions: ["question_create", "question_edit", "question_publish"],
      assignedBy: SUPER_ADMIN_UID,
      status: "active",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("questions_public").doc("q_1").set({
      questionText: "বাংলাদেশের রাজধানী কোনটি?",
      optionA: "চট্টগ্রাম",
      optionB: "ঢাকা",
      optionC: "রাজশাহী",
      optionD: "খুলনা",
      subjectId: "sub_gk",
      difficulty: "easy",
      language: "bn",
      verified: true,
      needsReview: false,
      status: "published",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("questions_public").doc("q_draft").set({
      questionText: "খসড়া প্রশ্ন",
      optionA: "ক",
      optionB: "খ",
      optionC: "গ",
      optionD: "ঘ",
      subjectId: "sub_gk",
      difficulty: "medium",
      language: "bn",
      verified: false,
      needsReview: true,
      status: "unpublished",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("questionAnswers_private").doc("q_1").set({
      questionId: "q_1",
      correctAnswer: "B",
      explanation: "ঢাকা বাংলাদেশের রাজধানী।",
      verified: true,
      confidence: 0.98,
      status: "active",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Student can read published public question (filtered query)
  await assertSucceeds(
    aliceDb.collection("questions_public").where("status", "==", "published").get()
  );
  // Student cannot read unpublished questions or unfiltered list
  await assertFails(aliceDb.collection("questions_public").doc("q_draft").get());
  await assertFails(aliceDb.collection("questions_public").get());

  // Student CANNOT read or write private answer key
  await assertFails(aliceDb.collection("questionAnswers_private").doc("q_1").get());
  await assertFails(aliceDb.collection("questionAnswers_private").get());

  // Student CANNOT publish or create questions
  await assertFails(
    aliceDb.collection("questions_public").doc("q_draft").update({
      status: "published",
      updatedAt: pastDate(500),
    })
  );

  // Admin CAN read/update private answers & publish questions
  await assertSucceeds(adminDb.collection("questionAnswers_private").doc("q_1").get());
  await assertSucceeds(
    adminDb.collection("questions_public").doc("q_draft").update({
      verified: true,
      needsReview: false,
      status: "published",
      updatedAt: pastDate(500),
    })
  );
});

test("Students can only access their own bookmarks, wrong answers, and server-written exam results", async () => {
  const now = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("examinationResults").doc("res_alice").set({
      examId: "exam_1",
      examTitle: "বিসিএস মডেল টেস্ট ১",
      subjectId: "sub_gk",
      subjectName: "সাধারণ জ্ঞান",
      sessionId: "sess_1",
      userId: STUDENT_ALICE,
      totalQuestions: 10,
      answeredQuestions: 10,
      unansweredQuestions: 0,
      correctAnswers: 8,
      incorrectAnswers: 2,
      totalMarks: 10,
      obtainedMarks: 7,
      negativeMarks: 1,
      percentage: 70,
      accuracy: 80,
      timeTakenSeconds: 120,
      status: "completed",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(STUDENT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_BOB).firestore();

  // Alice reads her own result; Bob cannot; neither can create/forge client-side results
  await assertSucceeds(
    aliceDb.collection("examinationResults").where("userId", "==", STUDENT_ALICE).get()
  );
  await assertFails(bobDb.collection("examinationResults").doc("res_alice").get());
  await assertFails(
    bobDb.collection("examinationResults").doc("res_forged").set({
      examId: "exam_1",
      userId: STUDENT_BOB,
      totalQuestions: 10,
      correctAnswers: 10,
      incorrectAnswers: 0,
      obtainedMarks: 10,
      percentage: 100,
      status: "completed",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bookmarks scoped to owner
  await assertSucceeds(
    aliceDb.collection("bookmarks").doc(`${STUDENT_ALICE}_q_1`).set({
      userId: STUDENT_ALICE,
      questionId: "q_1",
      questionText: "বাংলাদেশের রাজধানী কোনটি?",
      subjectId: "sub_gk",
      subjectName: "সাধারণ জ্ঞান",
      collectionName: "Review Later",
      reviewLater: true,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("bookmarks").doc(`${STUDENT_ALICE}_q_1`).get());
});
