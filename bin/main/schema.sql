-- 테이블: penalty  
-- 목적: 모든 벌칙의 내용을 관리합니다. 사용자가 직접 생성하거나, 시스템이 기본으로 제공하는 벌칙을 모두 이 테이블에 저장합니다.
CREATE TABLE IF NOT EXISTS penalty (
    penalty_id BIGINT PRIMARY KEY AUTO_INCREMENT,   -- 벌칙의 고유 식별자입니다.
    user_uid   VARCHAR(100) NOT NULL,               -- 벌칙을 생성한 사용자의 고유 ID입니다.
    penalty_text VARCHAR(255) NOT NULL,             -- 벌칙의 실제 내용입니다. (예: "커피 사기")
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 벌칙이 생성된 시각입니다.
);

-- 테이블: game_session
-- 목적: 미니게임 한 판(세션)의 전체적인 상태와 정보를 관리합니다. 게임의 종류, 참여자, 진행 상태, 최종 결과 등이 기록됩니다.
CREATE TABLE game_session (
    session_id          BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 게임 세션의 고유 식별자입니다.
    appointment_id      BIGINT,                    -- 이 게임이 어떤 약속에 속해있는지를 나타내는 ID입니다. (MSA 환경에서 '약속 서비스'와 연결됩니다.)
    host_uid            VARCHAR(100) NOT NULL,              -- 게임을 생성한 방장의 사용자 ID입니다. 이 사용자가 설정한 벌칙 목록이 게임에 사용됩니다.
    game_type           VARCHAR(50) NOT NULL,               -- 생성된 게임의 종류입니다. (예: 'REACTION', 'QUIZ')
    status              VARCHAR(50) DEFAULT 'WAITING',      -- 게임의 현재 진행 상태입니다. (WAITING, IN_PROGRESS, FINISHED)
    start_time          TIMESTAMP NULL,                     -- 게임이 실제로 시작된 시각입니다.
    end_time            TIMESTAMP NULL,                     -- 게임이 최종 종료된 시각입니다.
    selected_penalty_id BIGINT,                             -- 게임 시작 전에 방장이 선택한 벌칙의 ID입니다.
    penalty_description VARCHAR(255),                       -- [스냅샷] 게임 종료 시점의 벌칙 내용을 여기에 복사하여 저장합니다. 원본 벌칙이 수정되어도 과거 기록이 변하지 않도록 보존하는 중요한 역할을 합니다.
    total_rounds        INT DEFAULT 5, -- ◀◀◀ 퀴즈 문항 수 필드 추가 (기본값 5)
    is_private         BOOLEAN DEFAULT FALSE,               -- 비공개방 여부
    pin_hash           VARCHAR(255),                        -- 비공개방 PIN 해시값
    FOREIGN KEY (selected_penalty_id) REFERENCES penalty(penalty_id)
);


-- 테이블: game_penalty
-- 목적: 게임이 끝난 후, 최종적으로 결정된 패자와 그에게 할당된 벌칙을 기록하는 '결과표'입니다.
CREATE TABLE game_penalty (
    game_id    BIGINT NOT NULL,                       -- 벌칙이 발생한 게임 세션의 ID입니다.
    user_uid   VARCHAR(100) NOT NULL,                 -- 벌칙을 받게 된 패자의 사용자 ID입니다.
    penalty_id BIGINT NOT NULL,                       -- 패자에게 할당된 벌칙의 ID입니다.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 벌칙이 할당된 시각입니다.
    PRIMARY KEY (game_id, user_uid),                  -- 한 게임당 한 명의 패자만 존재하도록 보장합니다.
    FOREIGN KEY (game_id) REFERENCES game_session(session_id),
    FOREIGN KEY (penalty_id) REFERENCES penalty(penalty_id)
);

-- 테이블: quiz_question
-- 목적: 퀴즈 게임에 사용될 모든 질문의 원본 데이터를 저장합니다.
CREATE TABLE quiz_question (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 질문의 고유 식별자입니다.
    place_id      BIGINT,                             -- 질문이 특정 장소와 관련이 있을 경우, 해당 장소의 ID입니다. (선택적)
    question_text TEXT NOT NULL,                      -- 질문의 내용입니다.
    category      VARCHAR(100)                        -- 질문의 카테고리입니다. (예: '음식', '역사')
);

-- 테이블: quiz_question_option
-- 목적: 각 퀴즈 질문에 대한 객관식 선택지를 저장합니다.
CREATE TABLE quiz_question_option (
    option_id   BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 선택지의 고유 식별자입니다.
    question_id BIGINT NOT NULL,                    -- 이 선택지가 어떤 질문에 속하는지를 나타냅니다.
    option_text VARCHAR(255) NOT NULL,              -- 선택지의 내용입니다.
    is_correct  BOOLEAN NOT NULL,                   -- 이 선택지가 정답인지 여부를 나타냅니다. (true: 정답, false: false: 오답)
    FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

-- 테이블: quiz_round
-- 목적: 퀴즈 게임의 한 라운드 정보를 관리합니다. 한 게임 세션은 여러 라운드로 구성됩니다.
CREATE TABLE quiz_round (
    round_id    BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 라운드의 고유 식별자입니다.
    session_id  BIGINT NOT NULL,                    -- 이 라운드가 어떤 게임 세션에 속하는지를 나타냅니다.
    question_id BIGINT NOT NULL,                    -- 이 라운드에서 출제된 질문의 ID입니다.
    start_time  TIMESTAMP,                          -- 라운드가 시작된 시각입니다. 응답 시간 계산의 기준점이 됩니다.
    FOREIGN KEY (session_id) REFERENCES game_session(session_id),
    FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

-- 테이블: quiz_answer
-- 목적: 각 라운드에서 사용자가 제출한 답변을 기록합니다.
CREATE TABLE quiz_answer (
    answer_id        BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 답변의 고유 식별자입니다.
    round_id         BIGINT NOT NULL,                    -- 이 답변이 어떤 라운드에 대한 것인지를 나타냅니다.
    user_uid         VARCHAR(100) NOT NULL,              -- 답변을 제출한 사용자의 ID입니다.
    answer_text      TEXT NOT NULL,                      -- 사용자가 제출한 답변의 내용입니다.
    answer_time      TIMESTAMP,                          -- 답변을 제출한 시각입니다.
    is_correct       BOOLEAN,                            -- 제출된 답변의 정답 여부입니다.
    response_time_ms BIGINT,                             -- [규칙 핵심] 정답인 경우, 라운드 시작부터 정답 제출까지 걸린 시간(ms)입니다. 이 값의 총합이 가장 큰 사용자가 패배합니다.
    FOREIGN KEY (round_id) REFERENCES quiz_round(round_id)
);

-- 테이블: game_session_member
-- 목적: 게임 세션에 참여하는 멤버들을 관리합니다. 최대 10명까지 참여 가능하며, 각 멤버의 준비 상태를 추적합니다.
CREATE TABLE game_session_member (
    session_id  BIGINT NOT NULL,                        -- 게임 세션의 ID입니다.
    user_uid    VARCHAR(100) NOT NULL,                  -- 참여하는 사용자의 고유 ID입니다.
    is_ready    BOOLEAN DEFAULT FALSE,                  -- 사용자의 준비 상태입니다.
    joined_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,    -- 세션에 참여한 시각입니다.
    PRIMARY KEY (session_id, user_uid),                 -- 복합 기본키로 한 세션에 한 사용자는 한 번만 참여 가능합니다.
    FOREIGN KEY (session_id) REFERENCES game_session(session_id)
);

-- 테이블: reaction_round
-- 목적: 반응속도 게임의 라운드를 관리합니다. 각 라운드는 WAITING → READY → RED → FINISHED 상태로 진행됩니다.
CREATE TABLE IF NOT EXISTS reaction_round (
    round_id     BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 라운드의 고유 식별자입니다.
    session_id   BIGINT NOT NULL,                       -- 이 라운드가 속한 게임 세션의 ID입니다.
    status       VARCHAR(20) NOT NULL DEFAULT 'WAITING',-- 라운드 상태 (WAITING, READY, RED, FINISHED)
    red_at       TIMESTAMP NULL,                        -- 빨강 신호로 전환된 시각입니다.
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 라운드가 생성된 시각입니다.
    FOREIGN KEY (session_id) REFERENCES game_session(session_id)
);

-- 테이블: reaction_result
-- 목적: 반응속도 게임에서 각 사용자의 클릭 결과를 기록합니다.
DROP TABLE IF EXISTS reaction_result;
CREATE TABLE reaction_result (
    result_id    BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 결과의 고유 식별자입니다.
    round_id     BIGINT NOT NULL,                       -- 이 결과가 속한 라운드의 ID입니다.
    user_uid     VARCHAR(100) NOT NULL,                 -- 결과를 제출한 사용자의 ID입니다.
    clicked_at   TIMESTAMP NULL,                        -- 사용자가 클릭한 시각입니다.
    delta_ms     INT NULL,                              -- 빨강 신호 이후 반응 시간(ms), false_start시 NULL
    false_start  BOOLEAN NOT NULL DEFAULT FALSE,        -- 빨강 신호 전에 클릭했는지 여부
    rank_order   INT NULL,                              -- 최종 순위 (1등, 2등, ...)
    FOREIGN KEY (round_id) REFERENCES reaction_round(round_id)
);