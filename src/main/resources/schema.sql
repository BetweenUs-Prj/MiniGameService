-- 테이블: penalty
-- 기본 벌칙과 사용자 정의 벌칙을 모두 저장합니다.
-- user_uid가 NULL이면 기본 벌칙, 값이 있으면 해당 사용자의 벌칙입니다.
CREATE TABLE penalty (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255) NOT NULL,
    user_uid    VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 테이블: game_session
-- 모든 미니게임의 한 판(세션) 정보를 저장합니다.
-- host_uid: 게임을 생성한 방장의 ID
-- penalty_id: 게임 시작 전에 미리 선택된 벌칙의 ID
CREATE TABLE game_session (
    session_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_id  BIGINT NOT NULL,
    host_uid        VARCHAR(100) NOT NULL,
    game_type       VARCHAR(50) NOT NULL,
    status          VARCHAR(50) DEFAULT 'WAITING',
    start_time      TIMESTAMP NULL,
    end_time        TIMESTAMP NULL,
    penalty_id      BIGINT,
    FOREIGN KEY (penalty_id) REFERENCES penalty(id)
);

-- 테이블: reaction_result
-- 반응 속도 게임의 사용자별 결과를 저장합니다.
CREATE TABLE reaction_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id      BIGINT NOT NULL,
    user_uid        VARCHAR(100) NOT NULL,
    reaction_time   DOUBLE NOT NULL,
    ranking         INT,
    FOREIGN KEY (session_id) REFERENCES game_session(session_id)
);

-- 테이블: game_penalty
-- 게임 종료 후, 패자에게 할당된 벌칙 결과를 최종 기록합니다.
CREATE TABLE game_penalty (
    game_id    BIGINT NOT NULL,
    user_uid   VARCHAR(100) NOT NULL,
    penalty_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (game_id, user_uid),
    FOREIGN KEY (game_id) REFERENCES game_session(session_id),
    FOREIGN KEY (penalty_id) REFERENCES penalty(id)
);

-- 테이블: quiz_question
-- 퀴즈에 사용될 질문들을 저장합니다.
CREATE TABLE quiz_question (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    place_id        BIGINT,
    question_text   TEXT NOT NULL,
    category        VARCHAR(100)
);

-- 테이블: quiz_question_option
-- 각 퀴즈 질문에 대한 선택지들을 저장합니다.
CREATE TABLE quiz_question_option (
    option_id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    is_correct  BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

-- 테이블: quiz_round
-- 퀴즈 게임의 각 라운드 정보를 저장합니다.
CREATE TABLE quiz_round (
    round_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    start_time  TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES game_session(session_id),
    FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

-- 테이블: quiz_answer
-- 각 라운드에 대한 사용자의 답변을 저장합니다.
CREATE TABLE quiz_answer (
    answer_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    round_id          BIGINT NOT NULL,
    user_uid          VARCHAR(100) NOT NULL,
    answer_text       TEXT NOT NULL,
    answer_time       TIMESTAMP,
    is_correct        BOOLEAN,
    response_time_ms  BIGINT,
    FOREIGN KEY (round_id) REFERENCES quiz_round(round_id)
);