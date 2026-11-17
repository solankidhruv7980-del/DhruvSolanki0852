const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 1000);
}

function validConfig(uid) {
  return {
    userId: uid,
    isMasterAutopilotOn: true,
    dailyVideoFrequency: 5,
    contentType: "HYBRID",
    formatMode: "DUAL",
    shortsRatioPercent: 80,
    primaryNiche: "Viral Shorts & Funk Edits",
    activeFunkTrackId: "funk_neon_drift",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validBatch(uid, status = "QUEUED") {
  return {
    userId: uid,
    title: "3 AI Hooks That Double Retention",
    hookScript: "Stop scrolling! Watch how this beat drop locks viewers in.",
    format: "9:16",
    contentMode: "AI_ANIMATION",
    scheduledSlotLabel: "14:30 UTC",
    funkTrackId: "funk_neon_drift",
    funkTrackTitle: "Neon Drift Phonk (132 BPM)",
    memeCues: ["0:01 Vine Boom", "0:05 Zoom Punch"],
    animationCode: "from manim import *\nclass HookScene(Scene): pass",
    seoDescription: "Viral Shorts production package with YPP compliance.",
    seoTags: ["shorts", "funk", "ai"],
    yppScore: 94,
    estimatedRpmUsd: 4.85,
    status,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validAudit(uid) {
  return {
    userId: uid,
    targetTitle: "Neon Drift Short #1",
    safetyScore: 96,
    estimatedRpmUsd: 5.2,
    reusedContentRisk: "LOW",
    copyrightRisk: "LOW",
    advertiserFriendly: true,
    summary: "Original kinetic animation and original synth funk groove pass YPP.",
    recommendations: ["Add spoken commentary hook in first 2s"],
    createdAt: serverTimestamp(),
  };
}

function validConnection(uid) {
  return {
    userId: uid,
    channelHandle: "@omnitube_demo",
    channelId: "UC1234567890",
    oauthClientId: "client-id-123.apps.googleusercontent.com",
    grantedScopes: ["https://www.googleapis.com/auth/youtube.upload"],
    isLinked: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
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

test("Unauthenticated caller is denied on all studio collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("studio_config").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("batches").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("audits").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("channel_connection")
      .get()
  );
});

test("Owner can create and update studio_config; cross-user and shadow updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceConfigRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("studio_config")
    .doc("main");

  await assertSucceeds(aliceConfigRef.set(validConfig(ALICE_UID)));
  await assertSucceeds(
    aliceConfigRef.update({
      dailyVideoFrequency: 10,
      isMasterAutopilotOn: false,
      updatedAt: serverTimestamp(),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceConfigRef.update({
      isAdminOverride: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read or overwrite Alice's config
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studio_config")
      .doc("main")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studio_config")
      .doc("main")
      .set(validConfig(BOB_UID))
  );
});

test("Batches support funk track swap and terminal PUBLISHED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batchRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("batches")
    .doc("batch_1");

  await assertSucceeds(batchRef.set(validBatch(ALICE_UID, "QUEUED")));
  await assertSucceeds(
    batchRef.update({
      funkTrackId: "funk_tokyo_bass",
      funkTrackTitle: "Tokyo Bassline (140 BPM)",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    batchRef.update({
      status: "PUBLISHED",
      updatedAt: serverTimestamp(),
    })
  );

  // Terminal PUBLISHED document cannot be mutated further
  await assertFails(
    batchRef.update({
      status: "QUEUED",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Audits are append-only and channel_connection is owner-isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const auditRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("audits")
    .doc("audit_1");

  await assertSucceeds(auditRef.set(validAudit(ALICE_UID)));
  await assertFails(auditRef.update({ safetyScore: 100 }));

  const connRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("channel_connection")
    .doc("youtube");
  await assertSucceeds(connRef.set(validConnection(ALICE_UID)));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("channel_connection")
      .get()
  );
});
