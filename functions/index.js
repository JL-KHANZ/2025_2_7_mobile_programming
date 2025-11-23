const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

// 감정 점수 Map에서 topEmotion + topTwoEmotions 계산
function getTopEmotions(emotionScores) {
  const entries = Object.entries(emotionScores || {});
  if (entries.length === 0) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "emotionScores가 비어 있습니다.",
    );
  }

  // 점수 내림차순, 같으면 이름 사전순
  entries.sort((a, b) => {
    const diff = b[1] - a[1];
    if (diff !== 0) return diff;
    return a[0].localeCompare(b[0], "ko");
  });

  const topEmotion = entries[0][0];
  const topScore = entries[0][1];

  const topTwoEmotions = entries.slice(0, 2).map(([name]) => name);

  return {topEmotion, topScore, topTwoEmotions};
}

/**
  requestMatch
  입력(data):
   {
     uid: string,
    displayName: string,
     emotionScores: { [emotion: string]: number }
   }

  반환:
   {
     ticketId: string,              // 현재는 uid와 동일
     candidates: [
       {
         uid: string,
         displayName: string|null,
         topTwoEmotions: string[]
       }, ...
     ]
   }
 */
exports.requestMatch = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "로그인이 필요합니다.",
    );
  }

  const callerUid = context.auth.uid;
  const uid = data.uid || callerUid;
  if (uid !== callerUid) {
    // 보안상, data.uid를 임의로 바꾸는 것을 막기
    throw new functions.https.HttpsError(
        "permission-denied",
        "uid가 현재 사용자와 일치하지 않습니다.",
    );
  }

  const displayName =
    typeof data.displayName === "string" && data.displayName.trim() !== "" ?
      data.displayName :
      null;

  const emotionScores = data.emotionScores;
  if (typeof emotionScores !== "object" || emotionScores === null) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "emotionScores가 필요합니다.",
    );
  }

  const {topEmotion, topScore, topTwoEmotions} =
    getTopEmotions(emotionScores);

  const now = admin.firestore.FieldValue.serverTimestamp();
  const ticketRef = db.collection("matchTickets").doc(uid);

  // 내 티켓 업서트
  await ticketRef.set(
      {
        uid,
        displayName,
        emotionScores,
        topTwoEmotions, // ["기쁨", "신뢰"] 등
        status: "OPEN",
        createdAt: now,
        updatedAt: now,
      },
      {merge: true},
  );

  // OPEN 상태의 다른 티켓들 조회
  const snap = await db
      .collection("matchTickets")
      .where("status", "==", "OPEN")
      .get();

  const candidates = [];

  snap.forEach((doc) => {
    const t = doc.data();
    const otherUid = t.uid || doc.id;
    if (otherUid === uid) return; // 자기 자신 제외

    const scores = t.emotionScores || {};
    const theirScoreRaw = scores[topEmotion];
    const theirScore =
      typeof theirScoreRaw === "number" ? theirScoreRaw : 0;

    // 매칭 조건: 상대의 같은 감정 점수가 나의 topScore 이상
    if (theirScore >= topScore) {
      candidates.push({
        uid: otherUid,
        displayName: t.displayName || null,
        topTwoEmotions: Array.isArray(t.topTwoEmotions) ?
          t.topTwoEmotions :
          [],
      });
    }
  });

  // 최대 5명까지만
  const limited = candidates.slice(0, 5);

  return {
    ticketId: uid,
    candidates: limited,
  };
});

/**
  createChatRoom
  입력(data):
   {
     otherUid: string
   }

  반환:
   {
     roomId: string,
     otherDisplayName: string
   }
 */
exports.createChatRoom = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "로그인이 필요합니다.",
    );
  }

  const myUid = context.auth.uid;
  const otherUid = data.otherUid;

  if (!otherUid || typeof otherUid !== "string") {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "otherUid가 필요합니다.",
    );
  }

  if (otherUid === myUid) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "자기 자신과는 방을 만들 수 없습니다.",
    );
  }

  // pairKey 생성 (uid1_uid2 형태, 정렬)
  const [uidA, uidB] = [myUid, otherUid].sort();
  const pairKey = `${uidA}_${uidB}`;

  const roomsRef = db.collection("chatRooms");
  const now = admin.firestore.FieldValue.serverTimestamp();

  //  기존 방 있는지 조회 (OPEN 상태)
  const existingSnap = await roomsRef
      .where("pairKey", "==", pairKey)
      .where("status", "==", "OPEN")
      .limit(1)
      .get();

  let roomRef;
  let roomId;

  if (!existingSnap.empty) {
    // 이미 OPEN 상태의 방이 있으면 재사용
    roomRef = existingSnap.docs[0].ref;
    roomId = roomRef.id;
    await roomRef.update({
      updatedAt: now,
    });
  } else {
    // 새 방 생성
    roomRef = roomsRef.doc(); // auto ID
    roomId = roomRef.id;

    // 두 사람의 표시 이름 가져오기 (profile/info 기준)
    const myProfileRef = db.doc(`users/${myUid}/profile/info`);
    const otherProfileRef = db.doc(`users/${otherUid}/profile/info`);

    const [mySnap, otherSnap] = await Promise.all([
      myProfileRef.get(),
      otherProfileRef.get(),
    ]);

    const memberDisplayNames = {};

    if (mySnap.exists) {
      const d = mySnap.data() || {};
      memberDisplayNames[myUid] =
        d.nickname || d.name || (d.email && d.email.split("@")[0]) || "사용자";
    } else {
      memberDisplayNames[myUid] = "사용자";
    }

    let otherDisplayNameDB = "사용자";
    if (otherSnap.exists) {
      const d = otherSnap.data() || {};
      otherDisplayNameDB =
        d.nickname || d.name || (d.email && d.email.split("@")[0]) || "사용자";
    }
    memberDisplayNames[otherUid] = otherDisplayNameDB;

    await roomRef.set({
      pairKey,
      members: {
        [myUid]: true,
        [otherUid]: true,
      },
      memberDisplayNames,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    });
  }

  //  users/{uid}/profile/info 의 activeRoomId 갱신
  const myProfileRef = db.doc(`users/${myUid}/profile/info`);
  const otherProfileRef = db.doc(`users/${otherUid}/profile/info`);

  //  matchTickets 상태 CLOSED로 변경
  const ticketsRef = db.collection("matchTickets");
  const myTicketRef = ticketsRef.doc(myUid);
  const otherTicketRef = ticketsRef.doc(otherUid);

  const batch = db.batch();
  batch.set(
      myProfileRef,
      {activeRoomId: roomId, updatedAt: now},
      {merge: true},
  );
  batch.set(
      otherProfileRef,
      {activeRoomId: roomId, updatedAt: now},
      {merge: true},
  );
  batch.set(
      myTicketRef,
      {status: "CLOSED", updatedAt: now},
      {merge: true},
  );
  batch.set(
      otherTicketRef,
      {status: "CLOSED", updatedAt: now},
      {merge: true},
  );

  await batch.commit();

  // 응답에서 otherDisplayName 계산 (항상 "상대 기준")
  const roomSnap = await roomRef.get();
  const roomData = roomSnap.data() || {};
  const memberDisplayNames = roomData.memberDisplayNames || {};
  const otherDisplayName =
    memberDisplayNames[otherUid] || "익명의 사용자";

  return {
    roomId,
    otherDisplayName,
  };
});

/**
  getActiveChatRoom

  입력: 없음 (context.auth로 현재 사용자 uid 확인)
  반환:
    방 없을 때:
     { hasRoom: false }
    방 있을 때:
     {
       hasRoom: true,
       roomId: string,
       otherDisplayName: string
     }
 */
exports.getActiveChatRoom = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "로그인이 필요합니다.",
    );
  }

  const uid = context.auth.uid;
  const profileRef = db.doc(`users/${uid}/profile/info`);
  const profileSnap = await profileRef.get();

  if (!profileSnap.exists) {
    return {hasRoom: false};
  }

  const profile = profileSnap.data() || {};
  const activeRoomId = profile.activeRoomId;

  if (!activeRoomId || typeof activeRoomId !== "string") {
    return {hasRoom: false};
  }

  const roomRef = db.collection("chatRooms").doc(activeRoomId);
  const roomSnap = await roomRef.get();

  if (!roomSnap.exists) {
    // 방이 사라졌다면 activeRoomId를 정리해도 되지만, 여기선 단순히 없음 처리
    return {hasRoom: false};
  }

  const room = roomSnap.data() || {};

  if (room.status !== "OPEN") {
    return {hasRoom: false};
  }

  const members = room.members || {};
  const memberDisplayNames = room.memberDisplayNames || {};

  // otherUid 찾기
  const memberUids = Object.keys(members);
  const otherUid = memberUids.find((u) => u !== uid);

  if (!otherUid) {
    return {hasRoom: false};
  }

  const otherDisplayName =
    memberDisplayNames[otherUid] || "익명의 사용자";

  return {
    hasRoom: true,
    roomId: activeRoomId,
    otherDisplayName,
  };
});
// ===============================================================================================
/*
  closeChatRoom

  입력:
   {
     roomId: string
   }

 chatRooms/{roomId} 문서 읽어서 호출자가 멤버인지 확인
 두 멤버의 users/{uid}/profile/info.activeRoomId = null 로 설정
 chatRooms/{roomId} 문서 삭제
 Realtime DB /chatMessages/{roomId} 삭제

  반환:
   {
     success: true
   }
 */
exports.closeChatRoom = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "로그인이 필요합니다.",
    );
  }

  const callerUid = context.auth.uid;
  const roomId = data.roomId;

  if (!roomId || typeof roomId !== "string") {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "roomId가 필요합니다.",
    );
  }

  const roomRef = db.collection("chatRooms").doc(roomId);
  const roomSnap = await roomRef.get();

  // 방이 아예 없으면: 호출자 activeRoomId만 정리하고 성공 리턴(에러로 터뜨리지 않음)
  if (!roomSnap.exists) {
    const myProfileRef = db.doc(`users/${callerUid}/profile/info`);
    await myProfileRef.set(
        {
          activeRoomId: null,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        {merge: true},
    );
    return {success: true, message: "room_not_found"};
  }

  const room = roomSnap.data() || {};
  const members = room.members || {};

  // 호출자가 멤버가 아니면 권한 없음
  if (!members[callerUid]) {
    throw new functions.https.HttpsError(
        "permission-denied",
        "이 방의 참여자가 아닙니다.",
    );
  }

  const memberUids = Object.keys(members);
  const now = admin.firestore.FieldValue.serverTimestamp();

  const batch = db.batch();

  // 각 멤버 프로필의 activeRoomId = null로 변경
  memberUids.forEach((uid) => {
    const profileRef = db.doc(`users/${uid}/profile/info`);
    batch.set(
        profileRef,
        {
          activeRoomId: null,
          updatedAt: now,
        },
        {merge: true},
    );
  });

  // 방 문서 삭제
  batch.delete(roomRef);

  // Firestore 배치 커밋
  await batch.commit();

  // Realtime DB의 메시지 삭제
  const rtdb = admin.database();
  await rtdb.ref(`chatMessages/${roomId}`).remove();

  return {success: true};
});

