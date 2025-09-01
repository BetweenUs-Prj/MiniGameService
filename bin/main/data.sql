-- 기본 벌칙 데이터 (테스트용) - AUTO_INCREMENT 충돌 방지를 위해 ID 제거
INSERT INTO penalty (user_uid, penalty_text, created_at) VALUES 
('system', '커피 한 잔 사기', CURRENT_TIMESTAMP),
('system', '아이스크림 사기', CURRENT_TIMESTAMP),
('system', '치킨 한 마리 사기', CURRENT_TIMESTAMP),
('system', '노래 한 곡 부르기', CURRENT_TIMESTAMP),
('system', '댄스 한 곡 추기', CURRENT_TIMESTAMP);

-- 샘플 퀴즈 문제들
INSERT INTO quiz_question (id, question_text, category) VALUES (1, '맥주의 주 원료는 무엇일까요?', '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(1, '보리', TRUE),
(1, '쌀', FALSE),
(1, '옥수수', FALSE),
(1, '감자', FALSE);

INSERT INTO quiz_question (id, question_text, category) VALUES (2, '조선왕조 실록을 편찬한 왕조는?', '역사');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(2, '조선', TRUE),
(2, '고려', FALSE),
(2, '신라', FALSE),
(2, '백제', FALSE);

INSERT INTO quiz_question (id, question_text, category) VALUES (3, '축구에서 한 팀의 선수는 몇 명인가요?', '스포츠');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(3, '11명', TRUE),
(3, '10명', FALSE),
(3, '12명', FALSE),
(3, '9명', FALSE);

INSERT INTO quiz_question (id, question_text, category) VALUES (4, '김치의 주재료는 무엇인가요?', '음식');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(4, '배추', TRUE),
(4, '무', FALSE),
(4, '오이', FALSE),
(4, '당근', FALSE);

INSERT INTO quiz_question (id, question_text, category) VALUES (5, '한국의 수도는?', '상식');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(5, '서울', TRUE),
(5, '부산', FALSE),
(5, '인천', FALSE),
(5, '대구', FALSE);

-- 테스트용 게임 세션 (AUTO_INCREMENT 충돌 방지를 위해 제거)