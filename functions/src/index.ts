import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();

const db = admin.firestore();
const SUPER_ADMIN_UID = "aaWqMj9EgnWbZbX3skVUMOgpgRq2";

function requireAuth(auth: { uid: string } | undefined): string {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  return auth.uid;
}

async function isAuthorizedAdmin(uid: string): Promise<boolean> {
  if (uid === SUPER_ADMIN_UID) return true;
  const snap = await db.collection("admins").doc(uid).get();
  return snap.exists && snap.data()?.status === "active";
}

function roundTwo(val: number): number {
  return Math.round(val * 100) / 100;
}

/**
 * Verifies a single student practice answer on the trusted backend without exposing
 * questionAnswers_private to students.
 */
export const checkPracticeAnswer = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const questionId = String(request.data?.questionId ?? "").trim();
  const selectedOption = String(request.data?.selectedOption ?? "").trim().toUpperCase();
  const sessionId = String(request.data?.sessionId ?? "").trim();
  const isWrongRetry = Boolean(request.data?.isWrongRetry ?? false);

  if (!questionId || !["A", "B", "C", "D"].includes(selectedOption)) {
    throw new HttpsError("invalid-argument", "Valid questionId and selectedOption (A-D) are required.");
  }

  const [pubSnap, privSnap] = await Promise.all([
    db.collection("questions_public").doc(questionId).get(),
    db.collection("questionAnswers_private").doc(questionId).get(),
  ]);

  if (!pubSnap.exists || !privSnap.exists) {
    throw new HttpsError("not-found", "Question or answer key not found.");
  }

  const pubData = pubSnap.data()!;
  const privData = privSnap.data()!;

  if (pubData.status !== "published" && !(await isAuthorizedAdmin(uid))) {
    throw new HttpsError("permission-denied", "This question is not published.");
  }

  const correctAnswer = String(privData.correctAnswer ?? "").toUpperCase();
  const explanation = String(privData.explanation ?? "");
  const isCorrect = correctAnswer !== "" && selectedOption === correctAnswer;
  const now = admin.firestore.FieldValue.serverTimestamp();

  const batch = db.batch();

  // Update user practice aggregates
  const userRef = db.collection("users").doc(uid);
  batch.set(
    userRef,
    {
      totalAttempted: admin.firestore.FieldValue.increment(1),
      totalCorrect: admin.firestore.FieldValue.increment(isCorrect ? 1 : 0),
      totalIncorrect: admin.firestore.FieldValue.increment(isCorrect ? 0 : 1),
      updatedAt: now,
    },
    { merge: true }
  );

  // Track subject/daily progress
  const todayKey = new Date().toISOString().slice(0, 10);
  const progressId = `${uid}_${pubData.subjectId || "general"}_${todayKey}`;
  const progressRef = db.collection("studentProgress").doc(progressId);
  const existingProgress = await progressRef.get();
  if (!existingProgress.exists) {
    batch.set(progressRef, {
      userId: uid,
      subjectId: pubData.subjectId ?? "",
      subjectName: pubData.subjectName ?? "",
      chapterId: pubData.chapterId ?? "",
      chapterName: pubData.chapterName ?? "",
      attempted: 1,
      correct: isCorrect ? 1 : 0,
      incorrect: isCorrect ? 0 : 1,
      dateKey: todayKey,
      status: "active",
      createdAt: now,
      updatedAt: now,
    });
  } else {
    batch.update(progressRef, {
      attempted: admin.firestore.FieldValue.increment(1),
      correct: admin.firestore.FieldValue.increment(isCorrect ? 1 : 0),
      incorrect: admin.firestore.FieldValue.increment(isCorrect ? 0 : 1),
      updatedAt: now,
    });
  }

  // Wrong answer tracking
  const wrongId = `${uid}_${questionId}`;
  const wrongRef = db.collection("wrongAnswers").doc(wrongId);
  if (!isCorrect) {
    const wrongSnap = await wrongRef.get();
    if (!wrongSnap.exists) {
      batch.set(wrongRef, {
        userId: uid,
        questionId,
        questionText: pubData.questionText ?? "",
        subjectId: pubData.subjectId ?? "",
        subjectName: pubData.subjectName ?? "",
        chapterId: pubData.chapterId ?? "",
        chapterName: pubData.chapterName ?? "",
        selectedOption,
        correctAnswer,
        explanation,
        mistakeCount: 1,
        status: "active",
        createdAt: now,
        updatedAt: now,
      });
    } else {
      batch.update(wrongRef, {
        selectedOption,
        correctAnswer,
        explanation,
        mistakeCount: admin.firestore.FieldValue.increment(1),
        status: "active",
        updatedAt: now,
      });
    }
  } else if (isWrongRetry) {
    const wrongSnap = await wrongRef.get();
    if (wrongSnap.exists) {
      batch.update(wrongRef, { status: "resolved", updatedAt: now });
    }
  }

  // Update practice session if provided
  if (sessionId) {
    const sessionRef = db.collection("practiceSessions").doc(sessionId);
    const sessionSnap = await sessionRef.get();
    if (sessionSnap.exists && sessionSnap.data()?.userId === uid) {
      batch.update(sessionRef, {
        [`selectedAnswers.${questionId}`]: selectedOption,
        correctCount: admin.firestore.FieldValue.increment(isCorrect ? 1 : 0),
        incorrectCount: admin.firestore.FieldValue.increment(isCorrect ? 0 : 1),
        updatedAt: now,
      });
    }
  }

  await batch.commit();

  return {
    questionId,
    isCorrect,
    correctAnswer,
    explanation,
  };
});

/**
 * Scores an online examination securely on the server, persists official results,
 * updates student progress and wrong answers, and returns review breakdown.
 */
export const submitExamination = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const examId = String(request.data?.examId ?? "").trim();
  const sessionId = String(request.data?.sessionId ?? "").trim();
  const rawAnswers = (request.data?.answers ?? {}) as Record<string, string>;
  const timeTakenSeconds = Math.max(0, Number(request.data?.timeTakenSeconds ?? 0));

  if (!examId || !sessionId) {
    throw new HttpsError("invalid-argument", "examId and sessionId are required.");
  }

  const examRef = db.collection("examinations").doc(examId);
  const sessionRef = db.collection("examSessions").doc(sessionId);
  const [examSnap, sessionSnap] = await Promise.all([examRef.get(), sessionRef.get()]);

  if (!examSnap.exists) {
    throw new HttpsError("not-found", "Examination not found.");
  }
  if (!sessionSnap.exists) {
    throw new HttpsError("not-found", "Examination session not found.");
  }

  const sessionData = sessionSnap.data()!;
  if (sessionData.userId !== uid) {
    throw new HttpsError("permission-denied", "Cannot submit another user's exam session.");
  }
  if (sessionData.status === "completed") {
    throw new HttpsError("already-exists", "This exam session has already been submitted.");
  }

  const examData = examSnap.data()!;
  const questionIds: string[] = Array.isArray(examData.questionIds) ? examData.questionIds : [];
  if (questionIds.length === 0) {
    throw new HttpsError("failed-precondition", "Examination has no assigned questions.");
  }

  // Fetch public questions and private answers in chunks of 30 (Firestore getAll supports large arrays)
  const pubRefs = questionIds.map((qid) => db.collection("questions_public").doc(qid));
  const privRefs = questionIds.map((qid) => db.collection("questionAnswers_private").doc(qid));
  const [pubSnaps, privSnaps] = await Promise.all([db.getAll(...pubRefs), db.getAll(...privRefs)]);

  const marksPerQuestion = Number(examData.marksPerQuestion ?? 1);
  const negativeMarking = Number(examData.negativeMarking ?? 0);
  const totalQuestions = questionIds.length;
  const totalMarks = roundTwo(Number(examData.totalMarks ?? totalQuestions * marksPerQuestion));

  let answeredQuestions = 0;
  let correctAnswers = 0;
  let incorrectAnswers = 0;

  const reviewItems: Array<{
    questionId: string;
    questionText: string;
    optionA: string;
    optionB: string;
    optionC: string;
    optionD: string;
    selectedOption: string;
    correctAnswer: string;
    explanation: string;
    isCorrect: boolean;
  }> = [];

  const now = admin.firestore.FieldValue.serverTimestamp();
  const batch = db.batch();

  for (let i = 0; i < questionIds.length; i++) {
    const qid = questionIds[i];
    const pub = pubSnaps[i]?.data() ?? {};
    const priv = privSnaps[i]?.data() ?? {};
    const selected = String(rawAnswers[qid] ?? "").trim().toUpperCase();
    const correct = String(priv.correctAnswer ?? "").trim().toUpperCase();
    const explanation = String(priv.explanation ?? "");

    const isAnswered = ["A", "B", "C", "D"].includes(selected);
    const isCorrect = isAnswered && correct !== "" && selected === correct;

    if (isAnswered) {
      answeredQuestions++;
      if (isCorrect) {
        correctAnswers++;
      } else {
        incorrectAnswers++;
        const wrongRef = db.collection("wrongAnswers").doc(`${uid}_${qid}`);
        batch.set(
          wrongRef,
          {
            userId: uid,
            questionId: qid,
            questionText: String(pub.questionText ?? ""),
            subjectId: String(pub.subjectId ?? examData.subjectId ?? ""),
            subjectName: String(pub.subjectName ?? examData.subjectName ?? ""),
            chapterId: String(pub.chapterId ?? examData.chapterId ?? ""),
            chapterName: String(pub.chapterName ?? ""),
            selectedOption: selected,
            correctAnswer: correct,
            explanation,
            mistakeCount: admin.firestore.FieldValue.increment(1),
            status: "active",
            createdAt: now,
            updatedAt: now,
          },
          { merge: true }
        );
      }
    }

    reviewItems.push({
      questionId: qid,
      questionText: String(pub.questionText ?? ""),
      optionA: String(pub.optionA ?? ""),
      optionB: String(pub.optionB ?? ""),
      optionC: String(pub.optionC ?? ""),
      optionD: String(pub.optionD ?? ""),
      selectedOption: isAnswered ? selected : "",
      correctAnswer: correct,
      explanation,
      isCorrect,
    });
  }

  const unansweredQuestions = totalQuestions - answeredQuestions;
  const negativeMarks = roundTwo(incorrectAnswers * negativeMarking);
  const obtainedMarks = roundTwo(correctAnswers * marksPerQuestion - negativeMarks);
  const percentage = totalMarks > 0 ? roundTwo(Math.max(0, (obtainedMarks / totalMarks) * 100)) : 0;
  const accuracy = answeredQuestions > 0 ? roundTwo((correctAnswers / answeredQuestions) * 100) : 0;

  const resultRef = db.collection("examinationResults").doc();
  const resultPayload = {
    examId,
    examTitle: String(examData.title ?? ""),
    subjectId: String(examData.subjectId ?? ""),
    subjectName: String(examData.subjectName ?? ""),
    sessionId,
    userId: uid,
    totalQuestions,
    answeredQuestions,
    unansweredQuestions,
    correctAnswers,
    incorrectAnswers,
    totalMarks,
    obtainedMarks,
    negativeMarks,
    percentage,
    accuracy,
    timeTakenSeconds,
    status: "completed",
    createdAt: now,
    updatedAt: now,
  };

  batch.set(resultRef, resultPayload);
  batch.update(sessionRef, {
    answers: rawAnswers,
    status: "completed",
    updatedAt: now,
  });

  const userRef = db.collection("users").doc(uid);
  const userSnap = await userRef.get();
  const prevBest = Number(userSnap.data()?.bestScore ?? 0);
  batch.set(
    userRef,
    {
      totalExams: admin.firestore.FieldValue.increment(1),
      totalAttempted: admin.firestore.FieldValue.increment(answeredQuestions),
      totalCorrect: admin.firestore.FieldValue.increment(correctAnswers),
      totalIncorrect: admin.firestore.FieldValue.increment(incorrectAnswers),
      bestScore: Math.max(prevBest, percentage),
      updatedAt: now,
    },
    { merge: true }
  );

  // Subject/Daily progress update
  const todayKey = new Date().toISOString().slice(0, 10);
  const progressRef = db.collection("studentProgress").doc(`${uid}_${examData.subjectId || "general"}_${todayKey}`);
  const progSnap = await progressRef.get();
  if (!progSnap.exists) {
    batch.set(progressRef, {
      userId: uid,
      subjectId: String(examData.subjectId ?? ""),
      subjectName: String(examData.subjectName ?? ""),
      chapterId: String(examData.chapterId ?? ""),
      chapterName: "",
      attempted: answeredQuestions,
      correct: correctAnswers,
      incorrect: incorrectAnswers,
      dateKey: todayKey,
      status: "active",
      createdAt: now,
      updatedAt: now,
    });
  } else {
    batch.update(progressRef, {
      attempted: admin.firestore.FieldValue.increment(answeredQuestions),
      correct: admin.firestore.FieldValue.increment(correctAnswers),
      incorrect: admin.firestore.FieldValue.increment(incorrectAnswers),
      updatedAt: now,
    });
  }

  await batch.commit();

  return {
    resultId: resultRef.id,
    ...resultPayload,
    reviewItems,
  };
});

/**
 * Returns answer keys and explanations for a completed examination result.
 */
export const getExamReview = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const resultId = String(request.data?.resultId ?? "").trim();
  if (!resultId) {
    throw new HttpsError("invalid-argument", "resultId is required.");
  }

  const resultSnap = await db.collection("examinationResults").doc(resultId).get();
  if (!resultSnap.exists) {
    throw new HttpsError("not-found", "Result not found.");
  }
  const resultData = resultSnap.data()!;
  if (resultData.userId !== uid && !(await isAuthorizedAdmin(uid))) {
    throw new HttpsError("permission-denied", "Access denied to this exam result.");
  }

  const [examSnap, sessionSnap] = await Promise.all([
    db.collection("examinations").doc(resultData.examId).get(),
    db.collection("examSessions").doc(resultData.sessionId).get(),
  ]);

  const questionIds: string[] = Array.isArray(examSnap.data()?.questionIds) ? examSnap.data()!.questionIds : [];
  const savedAnswers = (sessionSnap.data()?.answers ?? {}) as Record<string, string>;
  if (questionIds.length === 0) {
    return { reviewItems: [] };
  }

  const [pubSnaps, privSnaps] = await Promise.all([
    db.getAll(...questionIds.map((q) => db.collection("questions_public").doc(q))),
    db.getAll(...questionIds.map((q) => db.collection("questionAnswers_private").doc(q))),
  ]);

  const reviewItems = questionIds.map((qid, i) => {
    const pub = pubSnaps[i]?.data() ?? {};
    const priv = privSnaps[i]?.data() ?? {};
    const selected = String(savedAnswers[qid] ?? "").toUpperCase();
    const correct = String(priv.correctAnswer ?? "").toUpperCase();
    return {
      questionId: qid,
      questionText: String(pub.questionText ?? ""),
      optionA: String(pub.optionA ?? ""),
      optionB: String(pub.optionB ?? ""),
      optionC: String(pub.optionC ?? ""),
      optionD: String(pub.optionD ?? ""),
      selectedOption: selected,
      correctAnswer: correct,
      explanation: String(priv.explanation ?? ""),
      isCorrect: selected !== "" && selected === correct,
    };
  });

  return { reviewItems };
});

/**
 * Super Admin exclusive function to promote, update permissions, or revoke an Admin.
 */
export const manageAdminRole = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  if (uid !== SUPER_ADMIN_UID) {
    throw new HttpsError("permission-denied", "Only the Super Admin can manage Admin roles.");
  }

  const targetUid = String(request.data?.targetUid ?? "").trim();
  const action = String(request.data?.action ?? "upsert").trim();
  const permissions = Array.isArray(request.data?.permissions) ? request.data.permissions.map(String) : [];

  if (!targetUid || targetUid === SUPER_ADMIN_UID) {
    throw new HttpsError("invalid-argument", "Invalid targetUid.");
  }

  const userRef = db.collection("users").doc(targetUid);
  const adminRef = db.collection("admins").doc(targetUid);
  const userSnap = await userRef.get();
  const now = admin.firestore.FieldValue.serverTimestamp();
  const batch = db.batch();

  if (action === "revoke") {
    batch.set(
      adminRef,
      {
        uid: targetUid,
        status: "revoked",
        permissions: [],
        assignedBy: uid,
        updatedAt: now,
      },
      { merge: true }
    );
    if (userSnap.exists) {
      batch.update(userRef, { role: "student", updatedAt: now });
    }
  } else {
    const email = String(userSnap.data()?.email ?? request.data?.email ?? "");
    const name = String(userSnap.data()?.name ?? request.data?.name ?? "Admin");
    batch.set(
      adminRef,
      {
        uid: targetUid,
        email,
        name,
        permissions,
        assignedBy: uid,
        status: "active",
        createdAt: now,
        updatedAt: now,
      },
      { merge: true }
    );
    if (userSnap.exists) {
      batch.update(userRef, { role: "admin", updatedAt: now });
    }
  }

  const logRef = db.collection("auditLogs").doc();
  batch.set(logRef, {
    actorUid: uid,
    action: `admin_${action}`,
    targetPath: `admins/${targetUid}`,
    details: JSON.stringify({ permissions }),
    status: "logged",
    createdAt: now,
    updatedAt: now,
  });

  await batch.commit();
  return { success: true };
});

/**
 * Broadcasts an educational notification and dispatches FCM push notification.
 */
export const broadcastNotification = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  if (!(await isAuthorizedAdmin(uid))) {
    throw new HttpsError("permission-denied", "Admin authorization required.");
  }

  const title = String(request.data?.title ?? "").trim();
  const body = String(request.data?.body ?? "").trim();
  const category = String(request.data?.category ?? "announcement").trim();
  const targetUid = String(request.data?.targetUid ?? "all").trim();
  const referenceId = String(request.data?.referenceId ?? "").trim();

  if (!title || !body) {
    throw new HttpsError("invalid-argument", "title and body are required.");
  }

  const now = admin.firestore.FieldValue.serverTimestamp();
  const notifRef = db.collection("notifications").doc();
  await notifRef.set({
    targetUid,
    title,
    body,
    category,
    referenceId,
    readByUids: [],
    status: "active",
    createdBy: uid,
    createdAt: now,
    updatedAt: now,
  });

  try {
    if (targetUid === "all") {
      await admin.messaging().send({
        topic: "all_students",
        notification: { title, body },
        data: { category, referenceId },
      });
    } else {
      const targetUser = await db.collection("users").doc(targetUid).get();
      const token = targetUser.data()?.fcmToken;
      if (typeof token === "string" && token.length > 0) {
        await admin.messaging().send({
          token,
          notification: { title, body },
          data: { category, referenceId },
        });
      }
    }
  } catch (err) {
    console.warn("FCM dispatch warning:", err);
  }

  return { notificationId: notifRef.id };
});
