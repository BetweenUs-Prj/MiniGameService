package su.kdt.minigame.event;

/**
 * 로비 관련 도메인 이벤트 정의
 * 트랜잭션 커밋 후 STOMP 브로드캐스트를 보장하기 위한 이벤트
 */
public class LobbyEvents {

    public static class MemberJoinedEvent {
        private final Long sessionId;
        private final String userUid;
        private final String gameType;

        public MemberJoinedEvent(Long sessionId, String userUid, String gameType) {
            this.sessionId = sessionId;
            this.userUid = userUid;
            this.gameType = gameType;
        }

        public Long getSessionId() { return sessionId; }
        public String getUserUid() { return userUid; }
        public String getGameType() { return gameType; }
    }

    public static class MemberLeftEvent {
        private final Long sessionId;
        private final String userUid;
        private final String gameType;

        public MemberLeftEvent(Long sessionId, String userUid, String gameType) {
            this.sessionId = sessionId;
            this.userUid = userUid;
            this.gameType = gameType;
        }

        public Long getSessionId() { return sessionId; }
        public String getUserUid() { return userUid; }
        public String getGameType() { return gameType; }
    }

    /**
     * 통일된 로비 이벤트 페이로드
     */
    public static class LobbyEventPayload {
        private final String type;
        private final Long sessionId;
        private final java.util.List<MemberInfo> members;
        private final int count;
        private final long timestamp;

        public LobbyEventPayload(String type, Long sessionId, java.util.List<MemberInfo> members) {
            this.type = type;
            this.sessionId = sessionId;
            this.members = members;
            this.count = members.size();
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() { return type; }
        public Long getSessionId() { return sessionId; }
        public java.util.List<MemberInfo> getMembers() { return members; }
        public int getCount() { return count; }
        public long getTimestamp() { return timestamp; }

        public static class MemberInfo {
            private final String uid;
            private final String name;
            private final String role;
            private final int score;

            public MemberInfo(String uid, String name, String role, int score) {
                this.uid = uid;
                this.name = name;
                this.role = role;
                this.score = score;
            }

            public String getUid() { return uid; }
            public String getName() { return name; }
            public String getRole() { return role; }
            public int getScore() { return score; }
        }
    }
}