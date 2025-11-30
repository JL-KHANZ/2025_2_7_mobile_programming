// const functions = require("firebase-functions"); // 이건 v1버전
const {onCall, HttpsError} = require("firebase-functions/v2/https"); // v2버전으로 통일(헷갈리면 안됨)
const {onSchedule} = require("firebase-functions/v2/scheduler");// 스케줄러
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();
// 날짜 기준으로 문서 삭제하기 위해서
function getTodayKeyKST() {
  const now = new Date(); // UTC0
  const kst = new Date(now.getTime() + 9 * 60 * 60 * 1000); // +9시간 (한국 utc+9이여서)
  return kst.toISOString().slice(0, 10); // "YYYY-MM-DD"
}

// 감정 점수 Map에서 topEmotion + topTwoEmotions 계산
function getTopEmotions(emotionScores) {
  const entries = Object.entries(emotionScores || {});
  if (entries.length === 0) {
    throw new HttpsError(
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
exports.requestMatch = onCall(async (request) => {
  console.log("[requestMatch] START", {
    auth: request.auth,
    data: request.data,
  });

  // 인증 확인 (ID 토큰 붙여서 들어왔는지)
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }

  const callerUid = request.auth.uid;
  const data = request.data || {};

  // uid 위변조 방지
  const uid = data.uid || callerUid;
  if (uid !== callerUid) {
    throw new HttpsError(
        "permission-denied",
        "uid가 현재 사용자와 일치하지 않습니다.",
    );
  }
  //nickname사용(없으면 익명의 사용자)
  const profileRef = db.doc(`users/${uid}/profile/info`);
  const profileSnap = await profileRef.get();

  let displayName = "익명의 사용자";
  if (profileSnap.exists) {
    const p = profileSnap.data() || {};
    if (typeof p.nickname === "string" && p.nickname.trim() !== "") {
      displayName = p.nickname.trim();
    }
  }
  // 감정 점수 검증
  const emotionScores = data.emotionScores;
  if (typeof emotionScores !== "object" || emotionScores === null) {
    throw new HttpsError("invalid-argument", "emotionScores가 필요합니다.");
  }

  const {topEmotion, topScore, topTwoEmotions} =
    getTopEmotions(emotionScores);

  const now = admin.firestore.FieldValue.serverTimestamp();
  const ticketRef = db.collection("matchTickets").doc(uid);
  const dayKey = getTodayKeyKST();

  // 내 티켓 업서트 (uid를 ticketId처럼 쓰는 구조)
  await ticketRef.set(
      {
        uid,
        displayName,
        emotionScores, // 전체 점수 저장
        topEmotion,
        topTwoEmotions, // ["기쁨", "신뢰"] 등
        status: "OPEN",
        dayKey,
        createdAt: now,
        updatedAt: now,
      },
      {merge: true},
  );

  // OPEN 상태,오늘(daykey)의 다른 티켓 조회
  const snap = await db
      .collection("matchTickets")
      .where("status", "==", "OPEN")
      .where("dayKey", "==", dayKey)
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
      const dName =typeof t.displayName === "string" && t.displayName.trim() !== ""
          ? t.displayName.trim()
          : "익명의 사용자";
      candidates.push({
        uid: otherUid,
        displayName: dName,
        topTwoEmotions: Array.isArray(t.topTwoEmotions) ?
          t.topTwoEmotions :
          [],
      });
    }
  });

  // 최대 5명까지 제한
  const limited = candidates.slice(0, 5);

  // 클라이언트로 반환
  return {
    ticketId: uid, // 현재 설계에선 uid == ticketId
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
exports.createChatRoom = onCall(async (request) => {
  // 인증 확인
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }

  const myUid = request.auth.uid;
  const data = request.data || {};
  const otherUid = data.otherUid;

  if (!otherUid || typeof otherUid !== "string") {
    throw new HttpsError("invalid-argument", "otherUid가 필요합니다.");
  }

  if (otherUid === myUid) {
    throw new HttpsError("invalid-argument", "자기 자신과는 방을 만들 수 없습니다.");
  }

  // pairKey 생성 (uid1_uid2 형태, 정렬)
  const [uidA, uidB] = [myUid, otherUid].sort();
  const pairKey = `${uidA}_${uidB}`;

  const roomsRef = db.collection("chatRooms");
  const now = admin.firestore.FieldValue.serverTimestamp();
  const dayKey = getTodayKeyKST();

  // 기존 OPEN 방 있는지 조회
  const existingSnap = await roomsRef
      .where("pairKey", "==", pairKey)
      .where("status", "==", "OPEN")
      .limit(1)
      .get();

  let roomRef;
  let roomId;

  if (!existingSnap.empty) {
    // 이미 OPEN 상태 방이 있으면 재사용
    roomRef = existingSnap.docs[0].ref;
    roomId = roomRef.id;
    await roomRef.update({
      updatedAt: now,
    });
  } else {
    // 새 방 생성
    roomRef = roomsRef.doc(); // auto ID
    roomId = roomRef.id;

    // 두 사람의 표시 이름 가져오기 (users/{uid}/profile/info)
    const myProfileRef = db.doc(`users/${myUid}/profile/info`);
    const otherProfileRef = db.doc(`users/${otherUid}/profile/info`);

    const [mySnap, otherSnap] = await Promise.all([
      myProfileRef.get(),
      otherProfileRef.get(),
    ]);

    const memberDisplayNames = {};
    //my
    if (mySnap.exists) {
      const d = mySnap.data() || {};
      if (typeof d.nickname === "string" && d.nickname.trim() !== "") {
        memberDisplayNames[myUid] = d.nickname.trim();
      } else {
        memberDisplayNames[myUid] = "익명의 사용자";
      }
    } else {
      memberDisplayNames[myUid] = "익명의 사용자";
    }
    //other
    if (otherSnap.exists) {
      const d = otherSnap.data() || {};
      if (typeof d.nickname === "string" && d.nickname.trim() !== "") {
        memberDisplayNames[otherUid] = d.nickname.trim();
      } else {
        memberDisplayNames[otherUid] = "익명의 사용자";
      }
    } else {
      memberDisplayNames[otherUid] = "익명의 사용자";
    }

    await roomRef.set({
      pairKey,
      members: {
        [myUid]: true,
        [otherUid]: true,
      },
      memberDisplayNames,
      status: "OPEN",
      dayKey,
      createdAt: now,
      updatedAt: now,
    });
  }

  //  users/{uid}/profile/info 의 activeRoomId 갱신
  const myProfileRef = db.doc(`users/${myUid}/profile/info`);
  const otherProfileRef = db.doc(`users/${otherUid}/profile/info`);

  // matchTickets 상태 CLOSED 로 변경
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

  // 응답에서 otherDisplayName 계산(ui에서 상대방 이름 표시해야해서)
  const roomSnap = await roomRef.get();
  const roomData = roomSnap.data() || {};
  const memberDisplayNames = roomData.memberDisplayNames || {};
  const otherDisplayName = memberDisplayNames[otherUid] || "익명의 사용자";

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
exports.getActiveChatRoom = onCall(async (request) => {
  // 인증 체크
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }

  const uid = request.auth.uid;
  const todayKey = getTodayKeyKST();

  // 내 profile/info 에서 activeRoomId 읽기
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

  // chatRooms/{activeRoomId} 조회
  const roomRef = db.collection("chatRooms").doc(activeRoomId);
  const roomSnap = await roomRef.get();

  if (!roomSnap.exists) {
    // 방이 삭제된 경우: 여기서는 그냥 없음 처리
    return {hasRoom: false};
  }

  const room = roomSnap.data() || {};

  if (room.status !== "OPEN") {
    return {hasRoom: false};
  }
  if (room.dayKey !== todayKey) {
    return {hasRoom: false};
  }

  const members = room.members || {};
  const memberDisplayNames = room.memberDisplayNames || {};

  // 나 말고 상대 uid 찾기
  const memberUids = Object.keys(members);
  const otherUid = memberUids.find((u) => u !== uid);

  if (!otherUid) {
    return {hasRoom: false};
  }

  const otherDisplayName =
    memberDisplayNames[otherUid] || "익명의 사용자";

  // 현재 진행 중인 방 정보 반환
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
exports.closeChatRoom = onCall(async (request) => {
  // 인증 체크
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }

  const callerUid = request.auth.uid;
  const roomId = request.data.roomId;

  if (!roomId || typeof roomId !== "string") {
    throw new HttpsError("invalid-argument", "roomId가 필요합니다.");
  }

  const roomRef = db.collection("chatRooms").doc(roomId);
  const roomSnap = await roomRef.get();

  // 방이 아예 없으면: 내 activeRoomId만 정리하고 그냥 성공 리턴
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
    throw new HttpsError(
        "permission-denied",
        "이 방의 참여자가 아닙니다.",
    );
  }

  const memberUids = Object.keys(members);
  const now = admin.firestore.FieldValue.serverTimestamp();

  const batch = db.batch();

  // 모든 멤버 프로필에서 activeRoomId 초기화
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

  // Realtime DB 메시지도 삭제
  const rtdb = admin.database();
  await rtdb.ref(`chatMessages/${roomId}`).remove();

  return {success: true};
});
// ========================================================================스케줄러
/**
매일 3시(서울) 기준으로 티켓,채팅방 등에서 dayKey 기준 데이터 삭제
관련된 활성화 된 방 초기화 및 roomId 삭제
 */
exports.cleanupOldMatchData = onSchedule(
    {
      schedule: "0 3 * * *", // 매일 03:00
      timeZone: "Asia/Seoul", // 현재는 서울 기준
    },
    async (event) => {
      const todayKey = getTodayKeyKST();
      console.log("[cleanupOldMatchData] START, todayKey =", todayKey);

      // 오래된 matchTickets 삭제
      try {
        const ticketsCol = db.collection("matchTickets");

        // dayKey < todayKey (어제)인 예전 티켓들
        const oldTicketsSnap = await ticketsCol
            .where("dayKey", "<", todayKey)
            .get();

        console.log(
            "[cleanupOldMatchData] old matchTickets count =",
            oldTicketsSnap.size,
        );

        for (const doc of oldTicketsSnap.docs) {
          console.log(
              "[cleanupOldMatchData] delete matchTicket",
              doc.id,
              doc.data().dayKey,
          );
          await doc.ref.delete();
        }

        // 이전 버전에서 작성된 dayKey 없는 티켓들 삭제
        const legacyTicketsSnap = await ticketsCol
            .where("dayKey", "==", null)
            .get();

        console.log(
            "[cleanupOldMatchData] legacy matchTickets(without dayKey) count =",
            legacyTicketsSnap.size,
        );

        for (const doc of legacyTicketsSnap.docs) {
          console.log(
              "[cleanupOldMatchData] delete legacy matchTicket",
              doc.id,
          );
          await doc.ref.delete();
        }
      } catch (e) {
        console.error("[cleanupOldMatchData] error while cleaning matchTickets", e);
      }

      //  오래된 chatRooms + RTDB 삭제
      try {
        const roomsCol = db.collection("chatRooms");

        // dayKey < todayKey 인 방들(전날 이전의 모든 방)
        const oldRoomsSnap = await roomsCol
            .where("dayKey", "<", todayKey)
            .get();

        console.log(
            "[cleanupOldMatchData] old chatRooms count =",
            oldRoomsSnap.size,
        );

        const rtdb = admin.database();

        for (const roomDoc of oldRoomsSnap.docs) {
          const roomId = roomDoc.id;
          const roomData = roomDoc.data() || {};
          const members = roomData.members || {};
          const memberUids = Object.keys(members);
          console.log(
              "[cleanupOldMatchData] processing chatRoom",
              roomId,
              "dayKey=",
              roomData.dayKey,
              "members=",
              memberUids,
          );

          const nowTs = admin.firestore.FieldValue.serverTimestamp();
          const batch = db.batch();

          // 각 멤버 프로필의 activeRoomId 초기화
          memberUids.forEach((uid) => {
            const profileRef = db.doc(`users/${uid}/profile/info`);
            batch.set(
                profileRef,
                {
                  activeRoomId: null,
                  updatedAt: nowTs,
                },
                {merge: true},
            );
          });

          // 방 문서 삭제
          batch.delete(roomDoc.ref);

          // Firestore 쪽 커밋
          await batch.commit();
          console.log(
              "[cleanupOldMatchData] Firestore room deleted:",
              roomId,
          );

          // Realtime DB의 채팅 메시지 삭제
          try {
            await rtdb.ref(`chatMessages/${roomId}`).remove();
            console.log(
                "[cleanupOldMatchData] RTDB messages deleted for room",
                roomId,
            );
          } catch (e) {
            console.error(
                "[cleanupOldMatchData] RTDB delete failed for room",
                roomId,
                e,
            );
          }
        }
      } catch (e) {
        console.error("[cleanupOldMatchData] error while cleaning chatRooms", e);
      }

      console.log("[cleanupOldMatchData] DONE");
    },
);
