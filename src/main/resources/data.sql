-- Category seed data - ensure categories exist before inserting questions
INSERT INTO category (name)
SELECT * FROM (SELECT '술') t
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='술');

INSERT INTO category (name)
SELECT * FROM (SELECT '역사') t
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='역사');

INSERT INTO category (name)
SELECT * FROM (SELECT '스포츠') t
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='스포츠');

INSERT INTO category (name)
SELECT * FROM (SELECT '음식') t
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='음식');

INSERT INTO category (name)
SELECT * FROM (SELECT '상식') t
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name='상식');

-- 샘플 퀴즈 문제들
INSERT INTO quiz_question (question_text, category) VALUES ('맥주의 주 원료는 무엇일까요?', '술');
SET @Q1 = LAST_INSERT_ID();
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(@Q1, '보리', TRUE),
(@Q1, '쌀', FALSE),
(@Q1, '옥수수', FALSE),
(@Q1, '감자', FALSE);

INSERT INTO quiz_question (question_text, category) VALUES ('조선왕조 실록을 편찬한 왕조는?', '역사');
SET @Q2 = LAST_INSERT_ID();
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(@Q2, '조선', TRUE),
(@Q2, '고려', FALSE),
(@Q2, '신라', FALSE),
(@Q2, '백제', FALSE);

INSERT INTO quiz_question (question_text, category) VALUES ('축구에서 한 팀의 선수는 몇 명인가요?', '스포츠');
SET @Q3 = LAST_INSERT_ID();
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(@Q3, '11명', TRUE),
(@Q3, '10명', FALSE),
(@Q3, '12명', FALSE),
(@Q3, '9명', FALSE);

INSERT INTO quiz_question (question_text, category) VALUES ('김치의 주재료는 무엇인가요?', '음식');
SET @Q4 = LAST_INSERT_ID();
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(@Q4, '배추', TRUE),
(@Q4, '무', FALSE),
(@Q4, '오이', FALSE),
(@Q4, '당근', FALSE);

INSERT INTO quiz_question (question_text, category) VALUES ('한국의 수도는 어디인가요?', '상식');
SET @Q5 = LAST_INSERT_ID();
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
(@Q5, '서울', TRUE),
(@Q5, '부산', FALSE),
(@Q5, '인천', FALSE),
(@Q5, '대구', FALSE);

-- (선택) 기존 "노래" 카테고리를 "상식"으로 바꾸려면 다음 한 줄 실행
-- UPDATE quiz_question SET category='상식' WHERE category='노래';

-- ============ 술 카테고리 100문항 ============

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-001] 다음 중 실제 맥주 스타일은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-001] 다음 중 실제 맥주 스타일은?'), 'IPA', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-001] 다음 중 실제 맥주 스타일은?'), '버블티 라거', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-001] 다음 중 실제 맥주 스타일은?'), '콜드브루 스타우트', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-001] 다음 중 실제 맥주 스타일은?'), '레모네이드 에일', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-002] 필스너(Pilsner)의 기원 국가는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-002] 필스너(Pilsner)의 기원 국가는?'), '체코', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-002] 필스너(Pilsner)의 기원 국가는?'), '독일', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-002] 필스너(Pilsner)의 기원 국가는?'), '벨기에', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-002] 필스너(Pilsner)의 기원 국가는?'), '오스트리아', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-003] 스타우트의 짙은 색의 주된 이유는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-003] 스타우트의 짙은 색의 주된 이유는?'), '강하게 볶은 맥아', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-003] 스타우트의 짙은 색의 주된 이유는?'), '포도 껍질', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-003] 스타우트의 짙은 색의 주된 이유는?'), '차 잎', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-003] 스타우트의 짙은 색의 주된 이유는?'), '초콜릿 파우더', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-004] 바이스비어(Weissbier)의 주 곡물 조합은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-004] 바이스비어(Weissbier)의 주 곡물 조합은?'), '보리+밀', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-004] 바이스비어(Weissbier)의 주 곡물 조합은?'), '보리+호밀', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-004] 바이스비어(Weissbier)의 주 곡물 조합은?'), '보리만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-004] 바이스비어(Weissbier)의 주 곡물 조합은?'), '쌀+옥수수', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-005] 라거(Lager)의 숙성 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-005] 라거(Lager)의 숙성 특징은?'), '저온 장기 숙성', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-005] 라거(Lager)의 숙성 특징은?'), '상온 단기 숙성', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-005] 라거(Lager)의 숙성 특징은?'), '고온 증류', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-005] 라거(Lager)의 숙성 특징은?'), '오크통 발효', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-006] 세종(Saison)의 기원 국가는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-006] 세종(Saison)의 기원 국가는?'), '벨기에', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-006] 세종(Saison)의 기원 국가는?'), '영국', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-006] 세종(Saison)의 기원 국가는?'), '미국', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-006] 세종(Saison)의 기원 국가는?'), '네덜란드', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-007] 고제(Gose) 스타일의 대표 풍미는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-007] 고제(Gose) 스타일의 대표 풍미는?'), '짭짤함과 산미', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-007] 고제(Gose) 스타일의 대표 풍미는?'), '강한 훈연향', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-007] 고제(Gose) 스타일의 대표 풍미는?'), '바닐라 단맛', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-007] 고제(Gose) 스타일의 대표 풍미는?'), '포도향', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-008] 임페리얼(Imperial)의 의미로 옳은 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-008] 임페리얼(Imperial)의 의미로 옳은 것은?'), '도수/바디감이 높음', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-008] 임페리얼(Imperial)의 의미로 옳은 것은?'), '무알코올', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-008] 임페리얼(Imperial)의 의미로 옳은 것은?'), '필스너 계열', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-008] 임페리얼(Imperial)의 의미로 옳은 것은?'), '과일 첨가', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-009] IPA의 쓴맛을 주로 좌우하는 재료는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-009] IPA의 쓴맛을 주로 좌우하는 재료는?'), '홉', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-009] IPA의 쓴맛을 주로 좌우하는 재료는?'), '효모', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-009] IPA의 쓴맛을 주로 좌우하는 재료는?'), '물', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-009] IPA의 쓴맛을 주로 좌우하는 재료는?'), '맥아', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-010] 드라이 호핑(Dry hopping)의 주 목적은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-010] 드라이 호핑(Dry hopping)의 주 목적은?'), '향 강화', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-010] 드라이 호핑(Dry hopping)의 주 목적은?'), '도수 상승', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-010] 드라이 호핑(Dry hopping)의 주 목적은?'), '필터링', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-010] 드라이 호핑(Dry hopping)의 주 목적은?'), '산화 억제', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-011] SRM/EBU와 더 관련 있는 항목은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-011] SRM/EBU와 더 관련 있는 항목은?'), '맥주 색/쓴맛 지표', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-011] SRM/EBU와 더 관련 있는 항목은?'), '알코올 도수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-011] SRM/EBU와 더 관련 있는 항목은?'), '잔의 크기', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-011] SRM/EBU와 더 관련 있는 항목은?'), '가격', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-012] 페일에일의 ''페일''은 무엇을 뜻하나?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-012] 페일에일의 ''페일''은 무엇을 뜻하나?'), '밝은 색 맥아 사용', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-012] 페일에일의 ''페일''은 무엇을 뜻하나?'), '가벼운 바디', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-012] 페일에일의 ''페일''은 무엇을 뜻하나?'), '낮은 도수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-012] 페일에일의 ''페일''은 무엇을 뜻하나?'), '무여과', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-013] 라들러(Radler)는 무엇을 섞은 맥주인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-013] 라들러(Radler)는 무엇을 섞은 맥주인가?'), '레몬 소다', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-013] 라들러(Radler)는 무엇을 섞은 맥주인가?'), '토닉워터', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-013] 라들러(Radler)는 무엇을 섞은 맥주인가?'), '콜라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-013] 라들러(Radler)는 무엇을 섞은 맥주인가?'), '우롱차', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-014] 니트로 맥주의 크리미 헤드를 만드는 가스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-014] 니트로 맥주의 크리미 헤드를 만드는 가스는?'), '질소', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-014] 니트로 맥주의 크리미 헤드를 만드는 가스는?'), '수소', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-014] 니트로 맥주의 크리미 헤드를 만드는 가스는?'), '산소', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-014] 니트로 맥주의 크리미 헤드를 만드는 가스는?'), '아르곤', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-015] 트라피스트 맥주는 누가 만드는가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-015] 트라피스트 맥주는 누가 만드는가?'), '수도원/수도자 관리', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-015] 트라피스트 맥주는 누가 만드는가?'), '왕실 양조장', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-015] 트라피스트 맥주는 누가 만드는가?'), '대학 연구소', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-015] 트라피스트 맥주는 누가 만드는가?'), '개인 홈브루만', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-016] 샴페인은 어떤 경우에만 사용 가능한 명칭인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-016] 샴페인은 어떤 경우에만 사용 가능한 명칭인가?'), '샹파뉴 지역 스파클링', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-016] 샴페인은 어떤 경우에만 사용 가능한 명칭인가?'), '모든 스파클링', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-016] 샴페인은 어떤 경우에만 사용 가능한 명칭인가?'), '스파클링 화이트만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-016] 샴페인은 어떤 경우에만 사용 가능한 명칭인가?'), '병입 전 설탕 무첨가', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-017] 레드 와인의 색은 무엇에서 오나?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-017] 레드 와인의 색은 무엇에서 오나?'), '포도 껍질 안토시아닌', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-017] 레드 와인의 색은 무엇에서 오나?'), '씨의 탄닌', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-017] 레드 와인의 색은 무엇에서 오나?'), '과육의 엽록소', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-017] 레드 와인의 색은 무엇에서 오나?'), '줄기의 리그닌', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-018] 드라이(와인)의 의미는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-018] 드라이(와인)의 의미는?'), '잔당이 적음', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-018] 드라이(와인)의 의미는?'), '도수 낮음', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-018] 드라이(와인)의 의미는?'), '숙성 짧음', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-018] 드라이(와인)의 의미는?'), '탄산 적음', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-019] 디캔팅의 주 목적은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-019] 디캔팅의 주 목적은?'), '침전 분리/에어레이션', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-019] 디캔팅의 주 목적은?'), '온도 낮춤', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-019] 디캔팅의 주 목적은?'), '도수 낮춤', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-019] 디캔팅의 주 목적은?'), '산화 방지', FALSE);

INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-020] 리슬링(Riesling)은 주로 어떤 와인에 쓰이나?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-020] 리슬링(Riesling)은 주로 어떤 와인에 쓰이나?'), '화이트', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-020] 리슬링(Riesling)은 주로 어떤 와인에 쓰이나?'), '레드', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-020] 리슬링(Riesling)은 주로 어떤 와인에 쓰이나?'), '로제', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-020] 리슬링(Riesling)은 주로 어떤 와인에 쓰이나?'), '주정강화', FALSE);

-- ... 중략: 21번부터 100번까지 동일한 패턴으로 이어집니다.
-- 아래 블록들은 100문항을 모두 포함합니다.

-- 21
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-021] 탄닌은 주로 어디에서 오는가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-021] 탄닌은 주로 어디에서 오는가?'), '껍질/씨/줄기', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-021] 탄닌은 주로 어디에서 오는가?'), '오크 뚜껑', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-021] 탄닌은 주로 어디에서 오는가?'), '물 첨가', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-021] 탄닌은 주로 어디에서 오는가?'), '설탕 잔당', FALSE);

-- 22
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-022] 스파클링 와인의 기포를 만드는 가공은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-022] 스파클링 와인의 기포를 만드는 가공은?'), '2차 발효 CO2', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-022] 스파클링 와인의 기포를 만드는 가공은?'), '질소 주입', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-022] 스파클링 와인의 기포를 만드는 가공은?'), '감압', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-022] 스파클링 와인의 기포를 만드는 가공은?'), '가열', FALSE);

-- 23
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-023] 로제 와인의 보편적 제조법은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-023] 로제 와인의 보편적 제조법은?'), '짧은 껍질 접촉 후 제거', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-023] 로제 와인의 보편적 제조법은?'), '레드+화이트 혼합만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-023] 로제 와인의 보편적 제조법은?'), '화이트에 캐러멜', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-023] 로제 와인의 보편적 제조법은?'), '씨를 볶음', FALSE);

-- 24
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-024] 카베르네 소비뇽의 일반적 풍미는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-024] 카베르네 소비뇽의 일반적 풍미는?'), '검붉은 과실+탄닌', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-024] 카베르네 소비뇽의 일반적 풍미는?'), '강한 허브향만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-024] 카베르네 소비뇽의 일반적 풍미는?'), '높은 산미만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-024] 카베르네 소비뇽의 일반적 풍미는?'), '짙은 바닐라만', FALSE);

-- 25
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-025] 화이트 와인을 너무 낮은 온도에서 마시면?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-025] 화이트 와인을 너무 낮은 온도에서 마시면?'), '향미가 닫힘', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-025] 화이트 와인을 너무 낮은 온도에서 마시면?'), '산미가 사라짐', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-025] 화이트 와인을 너무 낮은 온도에서 마시면?'), '도수 상승', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-025] 화이트 와인을 너무 낮은 온도에서 마시면?'), '탄산 생성', FALSE);

-- 26
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-026] 말벡의 대표 산지는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-026] 말벡의 대표 산지는?'), '아르헨티나', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-026] 말벡의 대표 산지는?'), '스페인', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-026] 말벡의 대표 산지는?'), '이탈리아', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-026] 말벡의 대표 산지는?'), '포르투갈', FALSE);

-- 27
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-027] 포트 와인은 어떤 와인인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-027] 포트 와인은 어떤 와인인가?'), '주정강화', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-027] 포트 와인은 어떤 와인인가?'), '아이스와인', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-027] 포트 와인은 어떤 와인인가?'), '내추럴', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-027] 포트 와인은 어떤 와인인가?'), '오렌지 와인', FALSE);

-- 28
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-028] 오키한 풍미는 주로 어디서?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-028] 오키한 풍미는 주로 어디서?'), '오크통 숙성', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-028] 오키한 풍미는 주로 어디서?'), '스테인리스 발효', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-028] 오키한 풍미는 주로 어디서?'), '병입 직후', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-028] 오키한 풍미는 주로 어디서?'), '고온 발효', FALSE);

-- 29
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-029] 테루아(terroir)가 의미하는 바는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-029] 테루아(terroir)가 의미하는 바는?'), '재배 환경의 총합', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-029] 테루아(terroir)가 의미하는 바는?'), '와인 가격', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-029] 테루아(terroir)가 의미하는 바는?'), '병 모양', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-029] 테루아(terroir)가 의미하는 바는?'), '코르크 재질', FALSE);

-- 30
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-030] 코르크 테인트의 원인 물질로 유명한 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-030] 코르크 테인트의 원인 물질로 유명한 것은?'), 'TCA', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-030] 코르크 테인트의 원인 물질로 유명한 것은?'), 'THC', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-030] 코르크 테인트의 원인 물질로 유명한 것은?'), 'BPA', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-030] 코르크 테인트의 원인 물질로 유명한 것은?'), 'PFOA', FALSE);

-- 31
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-031] 증류주의 도수를 나타내는 단위로 옳은 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-031] 증류주의 도수를 나타내는 단위로 옳은 것은?'), 'ABV(%)', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-031] 증류주의 도수를 나타내는 단위로 옳은 것은?'), 'RPM', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-031] 증류주의 도수를 나타내는 단위로 옳은 것은?'), 'PSI', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-031] 증류주의 도수를 나타내는 단위로 옳은 것은?'), 'dB', FALSE);

-- 32
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-032] 보드카의 전통적 특성은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-032] 보드카의 전통적 특성은?'), '중립적 향·맛', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-032] 보드카의 전통적 특성은?'), '강한 연기향', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-032] 보드카의 전통적 특성은?'), '단맛', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-032] 보드카의 전통적 특성은?'), '허브 비터', FALSE);

-- 33
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-033] 럼의 주 원료는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-033] 럼의 주 원료는?'), '사탕수수/당밀', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-033] 럼의 주 원료는?'), '보리', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-033] 럼의 주 원료는?'), '포도', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-033] 럼의 주 원료는?'), '감자', FALSE);

-- 34
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-034] 진(Gin)의 핵심 식물 향신료는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-034] 진(Gin)의 핵심 식물 향신료는?'), '주니퍼 베리', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-034] 진(Gin)의 핵심 식물 향신료는?'), '바닐라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-034] 진(Gin)의 핵심 식물 향신료는?'), '코코아', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-034] 진(Gin)의 핵심 식물 향신료는?'), '홍차', FALSE);

-- 35
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-035] 테킬라의 원료 식물은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-035] 테킬라의 원료 식물은?'), '청색 아가베', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-035] 테킬라의 원료 식물은?'), '선인장', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-035] 테킬라의 원료 식물은?'), '용설란 일반', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-035] 테킬라의 원료 식물은?'), '유카', FALSE);

-- 36
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-036] 브랜디는 무엇을 증류한 술인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-036] 브랜디는 무엇을 증류한 술인가?'), '와인', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-036] 브랜디는 무엇을 증류한 술인가?'), '맥주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-036] 브랜디는 무엇을 증류한 술인가?'), '사이다', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-036] 브랜디는 무엇을 증류한 술인가?'), '청주', FALSE);

-- 37
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-037] 리큐르의 공통 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-037] 리큐르의 공통 특징은?'), '당분/향료 첨가', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-037] 리큐르의 공통 특징은?'), '무여과', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-037] 리큐르의 공통 특징은?'), '오크 숙성 의무', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-037] 리큐르의 공통 특징은?'), '발효 의무 없음', FALSE);

-- 38
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-038] 아마로(Amaro)는 어떤 분류인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-038] 아마로(Amaro)는 어떤 분류인가?'), '허브 비터 리큐르', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-038] 아마로(Amaro)는 어떤 분류인가?'), '과일 증류주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-038] 아마로(Amaro)는 어떤 분류인가?'), '곡주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-038] 아마로(Amaro)는 어떤 분류인가?'), '발포주', FALSE);

-- 39
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-039] 메스칼은 무엇의 범주인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-039] 메스칼은 무엇의 범주인가?'), '아가베 증류주', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-039] 메스칼은 무엇의 범주인가?'), '옥수수 맥주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-039] 메스칼은 무엇의 범주인가?'), '포도 증류주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-039] 메스칼은 무엇의 범주인가?'), '사과 과실주', FALSE);

-- 40
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-040] 페르넷(Fernet)의 맛 성향은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-040] 페르넷(Fernet)의 맛 성향은?'), '진한 허브 비터', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-040] 페르넷(Fernet)의 맛 성향은?'), '강한 단맛', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-040] 페르넷(Fernet)의 맛 성향은?'), '훈연향 중심', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-040] 페르넷(Fernet)의 맛 성향은?'), '우유 풍미', FALSE);

-- 41
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-041] 캐스크 스트렝스 병입의 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-041] 캐스크 스트렝스 병입의 특징은?'), '가수 없이 높은 도수', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-041] 캐스크 스트렝스 병입의 특징은?'), '차갑게 여과', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-041] 캐스크 스트렝스 병입의 특징은?'), '달게 가미', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-041] 캐스크 스트렝스 병입의 특징은?'), '탄산 주입', FALSE);

-- 42
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-042] 콜드 필터링(차갑게 여과)의 목적은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-042] 콜드 필터링(차갑게 여과)의 목적은?'), '혼탁 원인 제거', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-042] 콜드 필터링(차갑게 여과)의 목적은?'), '도수 상승', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-042] 콜드 필터링(차갑게 여과)의 목적은?'), '색 진하게', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-042] 콜드 필터링(차갑게 여과)의 목적은?'), '향 첨가', FALSE);

-- 43
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-043] 블렌디드 위스키란?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-043] 블렌디드 위스키란?'), '여러 증류소/원액 블렌딩', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-043] 블렌디드 위스키란?'), '단일 통', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-043] 블렌디드 위스키란?'), '단일 곡물', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-043] 블렌디드 위스키란?'), '단일 빈티지', FALSE);

-- 44
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-044] 피트 향은 어느 공정에서 발생?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-044] 피트 향은 어느 공정에서 발생?'), '맥아 건조 시 이탄 연소', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-044] 피트 향은 어느 공정에서 발생?'), '증류 시 오렌지 껍질', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-044] 피트 향은 어느 공정에서 발생?'), '당화 시 바닐라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-044] 피트 향은 어느 공정에서 발생?'), '병입 시 오크칩', FALSE);

-- 45
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-045] 싱글 몰트의 ''싱글'' 의미는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-045] 싱글 몰트의 ''싱글'' 의미는?'), '단일 증류소 생산', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-045] 싱글 몰트의 ''싱글'' 의미는?'), '단일 곡물만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-045] 싱글 몰트의 ''싱글'' 의미는?'), '단일 통만', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-045] 싱글 몰트의 ''싱글'' 의미는?'), '단일 연도만', FALSE);

-- 46
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-046] 스카치 위스키 최소 숙성 규정은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-046] 스카치 위스키 최소 숙성 규정은?'), '3년', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-046] 스카치 위스키 최소 숙성 규정은?'), '1년', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-046] 스카치 위스키 최소 숙성 규정은?'), '2년', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-046] 스카치 위스키 최소 숙성 규정은?'), '6개월', FALSE);

-- 47
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-047] 버번 위스키의 통 규정은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-047] 버번 위스키의 통 규정은?'), '신 화이트오크 새통', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-047] 버번 위스키의 통 규정은?'), '중고 셰리통 허용', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-047] 버번 위스키의 통 규정은?'), '오크 아닌 통 가능', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-047] 버번 위스키의 통 규정은?'), '통 숙성 의무 없음', FALSE);

-- 48
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-048] 버번의 곡물 비중 요건은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-048] 버번의 곡물 비중 요건은?'), '옥수수 51% 이상', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-048] 버번의 곡물 비중 요건은?'), '호밀 51% 이상', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-048] 버번의 곡물 비중 요건은?'), '보리 51% 이상', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-048] 버번의 곡물 비중 요건은?'), '밀 51% 이상', FALSE);

-- 49
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-049] 라이 위스키의 곡물 요건은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-049] 라이 위스키의 곡물 요건은?'), '호밀 51% 이상', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-049] 라이 위스키의 곡물 요건은?'), '옥수수 51% 이상', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-049] 라이 위스키의 곡물 요건은?'), '보리 51% 이상', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-049] 라이 위스키의 곡물 요건은?'), '밀 51% 이상', FALSE);

-- 50
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-050] 캐스크 피니시는 무엇을 뜻하나?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-050] 캐스크 피니시는 무엇을 뜻하나?'), '마지막에 다른 통 추가 숙성', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-050] 캐스크 피니시는 무엇을 뜻하나?'), '통 없이 스테인리스', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-050] 캐스크 피니시는 무엇을 뜻하나?'), '병입 후 숙성', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-050] 캐스크 피니시는 무엇을 뜻하나?'), '오크칩 침지', FALSE);

-- 51
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-051] 표준잔(standard drink)의 개념은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-051] 표준잔(standard drink)의 개념은?'), '순알코올 일정량 기준', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-051] 표준잔(standard drink)의 개념은?'), '잔 크기', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-051] 표준잔(standard drink)의 개념은?'), '가격', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-051] 표준잔(standard drink)의 개념은?'), '브랜드', FALSE);

-- 52
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-052] 숙취를 줄이는 데 비교적 도움이 되는 습관은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-052] 숙취를 줄이는 데 비교적 도움이 되는 습관은?'), '물 섭취/속도 조절', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-052] 숙취를 줄이는 데 비교적 도움이 되는 습관은?'), '빈속 고도주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-052] 숙취를 줄이는 데 비교적 도움이 되는 습관은?'), '당분 과다', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-052] 숙취를 줄이는 데 비교적 도움이 되는 습관은?'), '혼성주 빠른 섭취', FALSE);

-- 53
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-053] 술과 약물 동시 복용은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-053] 술과 약물 동시 복용은?'), '상호작용 위험 큼', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-053] 술과 약물 동시 복용은?'), '안전함', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-053] 술과 약물 동시 복용은?'), '숙취 완화', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-053] 술과 약물 동시 복용은?'), '효과 무관', FALSE);

-- 54
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-054] 칵테일 셰이킹의 목적이 아닌 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-054] 칵테일 셰이킹의 목적이 아닌 것은?'), '탄산 주입', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-054] 칵테일 셰이킹의 목적이 아닌 것은?'), '냉각/희석/공기혼입', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-054] 칵테일 셰이킹의 목적이 아닌 것은?'), '균질화', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-054] 칵테일 셰이킹의 목적이 아닌 것은?'), '향 분산', FALSE);

-- 55
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-055] 온더락이 와인에 일반적이지 않은 이유는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-055] 온더락이 와인에 일반적이지 않은 이유는?'), '희석/향 저하', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-055] 온더락이 와인에 일반적이지 않은 이유는?'), '색 변함', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-055] 온더락이 와인에 일반적이지 않은 이유는?'), '탄산 유실', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-055] 온더락이 와인에 일반적이지 않은 이유는?'), '산도 급증', FALSE);

-- 56
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-056] 마가리타의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-056] 마가리타의 베이스는?'), '데킬라', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-056] 마가리타의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-056] 마가리타의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-056] 마가리타의 베이스는?'), '진', FALSE);

-- 57
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-057] 모히또의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-057] 모히또의 베이스는?'), '럼', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-057] 모히또의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-057] 모히또의 베이스는?'), '데킬라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-057] 모히또의 베이스는?'), '위스키', FALSE);

-- 58
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-058] 네그로니의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-058] 네그로니의 베이스는?'), '진', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-058] 네그로니의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-058] 네그로니의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-058] 네그로니의 베이스는?'), '데킬라', FALSE);

-- 59
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-059] 올드 패션드의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-059] 올드 패션드의 베이스는?'), '위스키', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-059] 올드 패션드의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-059] 올드 패션드의 베이스는?'), '진', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-059] 올드 패션드의 베이스는?'), '럼', FALSE);

-- 60
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-060] 모스크바 뮬의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-060] 모스크바 뮬의 베이스는?'), '보드카', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-060] 모스크바 뮬의 베이스는?'), '진', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-060] 모스크바 뮬의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-060] 모스크바 뮬의 베이스는?'), '데킬라', FALSE);

-- 61
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-061] 다이키리의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-061] 다이키리의 베이스는?'), '럼', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-061] 다이키리의 베이스는?'), '진', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-061] 다이키리의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-061] 다이키리의 베이스는?'), '위스키', FALSE);

-- 62
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-062] 피나 콜라다의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-062] 피나 콜라다의 베이스는?'), '럼', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-062] 피나 콜라다의 베이스는?'), '데킬라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-062] 피나 콜라다의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-062] 피나 콜라다의 베이스는?'), '진', FALSE);

-- 63
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-063] 블러디 메리의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-063] 블러디 메리의 베이스는?'), '보드카', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-063] 블러디 메리의 베이스는?'), '진', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-063] 블러디 메리의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-063] 블러디 메리의 베이스는?'), '데킬라', FALSE);

-- 64
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-064] 클래식 드라이 마티니의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-064] 클래식 드라이 마티니의 베이스는?'), '진', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-064] 클래식 드라이 마티니의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-064] 클래식 드라이 마티니의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-064] 클래식 드라이 마티니의 베이스는?'), '데킬라', FALSE);

-- 65
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-065] 위스키 사워의 구성은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-065] 위스키 사워의 구성은?'), '위스키+레몬+설탕', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-065] 위스키 사워의 구성은?'), '보드카+콜라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-065] 위스키 사워의 구성은?'), '럼+민트+사이다', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-065] 위스키 사워의 구성은?'), '진+토닉', FALSE);

-- 66
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-066] 롱아일랜드 아이스티의 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-066] 롱아일랜드 아이스티의 특징은?'), '여러 스피리츠 혼합+콜라', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-066] 롱아일랜드 아이스티의 특징은?'), '홍차 사용', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-066] 롱아일랜드 아이스티의 특징은?'), '진만 사용', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-066] 롱아일랜드 아이스티의 특징은?'), '보드카만 사용', FALSE);

-- 67
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-067] 쿠바 리브레의 구성은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-067] 쿠바 리브레의 구성은?'), '럼+라임+콜라', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-067] 쿠바 리브레의 구성은?'), '진+토닉', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-067] 쿠바 리브레의 구성은?'), '보드카+오렌지', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-067] 쿠바 리브레의 구성은?'), '데킬라+자몽', FALSE);

-- 68
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-068] 팔로마의 소프트드링크는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-068] 팔로마의 소프트드링크는?'), '자몽 소다', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-068] 팔로마의 소프트드링크는?'), '콜라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-068] 팔로마의 소프트드링크는?'), '진저에일', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-068] 팔로마의 소프트드링크는?'), '토닉워터', FALSE);

-- 69
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-069] 애플 마티니의 핵심 리큐르/시럽은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-069] 애플 마티니의 핵심 리큐르/시럽은?'), '사과(애플) 리큐르', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-069] 애플 마티니의 핵심 리큐르/시럽은?'), '체리 브랜디', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-069] 애플 마티니의 핵심 리큐르/시럽은?'), '아마레토', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-069] 애플 마티니의 핵심 리큐르/시럽은?'), '페퍼민트', FALSE);

-- 70
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-070] 화이트 러시안의 구성은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-070] 화이트 러시안의 구성은?'), '보드카+커피 리큐르+크림', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-070] 화이트 러시안의 구성은?'), '럼+코코넛+파인', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-070] 화이트 러시안의 구성은?'), '진+베르무트', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-070] 화이트 러시안의 구성은?'), '위스키+콜라', FALSE);

-- 71
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-071] 블랙 러시안에서 빠지는 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-071] 블랙 러시안에서 빠지는 것은?'), '크림', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-071] 블랙 러시안에서 빠지는 것은?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-071] 블랙 러시안에서 빠지는 것은?'), '커피 리큐르', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-071] 블랙 러시안에서 빠지는 것은?'), '얼음', FALSE);

-- 72
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-072] 사이드카의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-072] 사이드카의 베이스는?'), '브랜디', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-072] 사이드카의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-072] 사이드카의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-072] 사이드카의 베이스는?'), '진', FALSE);

-- 73
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-073] 프렌치 75의 스파클링 주류는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-073] 프렌치 75의 스파클링 주류는?'), '샴페인/스파클링 와인', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-073] 프렌치 75의 스파클링 주류는?'), '맥주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-073] 프렌치 75의 스파클링 주류는?'), '사케', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-073] 프렌치 75의 스파클링 주류는?'), '소다수', FALSE);

-- 74
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-074] 아페롤 스프리츠의 베이스 리큐르는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-074] 아페롤 스프리츠의 베이스 리큐르는?'), '아페롤', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-074] 아페롤 스프리츠의 베이스 리큐르는?'), '캄파리', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-074] 아페롤 스프리츠의 베이스 리큐르는?'), '베르무트 세코', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-074] 아페롤 스프리츠의 베이스 리큐르는?'), '트리플 섹', FALSE);

-- 75
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-075] 캄파리 소다의 쓴맛은 어디서?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-075] 캄파리 소다의 쓴맛은 어디서?'), '캄파리의 허브 비터', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-075] 캄파리 소다의 쓴맛은 어디서?'), '토닉의 키니네', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-075] 캄파리 소다의 쓴맛은 어디서?'), '레몬 껍질', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-075] 캄파리 소다의 쓴맛은 어디서?'), '생강', FALSE);

-- 76
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-076] 진 토닉의 핵심 쓴맛 성분은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-076] 진 토닉의 핵심 쓴맛 성분은?'), '키니네', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-076] 진 토닉의 핵심 쓴맛 성분은?'), '카페인', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-076] 진 토닉의 핵심 쓴맛 성분은?'), '타닌', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-076] 진 토닉의 핵심 쓴맛 성분은?'), '리모넨', FALSE);

-- 77
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-077] 아이리시 커피의 술은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-077] 아이리시 커피의 술은?'), '아이리시 위스키', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-077] 아이리시 커피의 술은?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-077] 아이리시 커피의 술은?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-077] 아이리시 커피의 술은?'), '진', FALSE);

-- 78
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-078] 카이피리냐의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-078] 카이피리냐의 베이스는?'), '카샤사', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-078] 카이피리냐의 베이스는?'), '피스코', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-078] 카이피리냐의 베이스는?'), '메스칼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-078] 카이피리냐의 베이스는?'), '그라파', FALSE);

-- 79
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-079] 미모사의 비알코올 재료는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-079] 미모사의 비알코올 재료는?'), '오렌지 주스', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-079] 미모사의 비알코올 재료는?'), '토마토 주스', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-079] 미모사의 비알코올 재료는?'), '콜라', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-079] 미모사의 비알코올 재료는?'), '진저에일', FALSE);

-- 80
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-080] 샹그리아의 전통적 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-080] 샹그리아의 전통적 베이스는?'), '레드 와인', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-080] 샹그리아의 전통적 베이스는?'), '맥주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-080] 샹그리아의 전통적 베이스는?'), '사케', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-080] 샹그리아의 전통적 베이스는?'), '보드카', FALSE);

-- 81
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-081] 뉴욕 사워의 상층으로 자주 올리는 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-081] 뉴욕 사워의 상층으로 자주 올리는 것은?'), '레드 와인 플로트', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-081] 뉴욕 사워의 상층으로 자주 올리는 것은?'), '오렌지 주스', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-081] 뉴욕 사워의 상층으로 자주 올리는 것은?'), '맥주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-081] 뉴욕 사워의 상층으로 자주 올리는 것은?'), '사이다', FALSE);

-- 82
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-082] 골든 러시의 베이스는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-082] 골든 러시의 베이스는?'), '버번 위스키', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-082] 골든 러시의 베이스는?'), '보드카', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-082] 골든 러시의 베이스는?'), '럼', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-082] 골든 러시의 베이스는?'), '진', FALSE);

-- 83
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-083] 모히또의 허브는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-083] 모히또의 허브는?'), '민트', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-083] 모히또의 허브는?'), '바질', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-083] 모히또의 허브는?'), '로즈메리', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-083] 모히또의 허브는?'), '타임', FALSE);

-- 84
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-084] 막걸리의 기본 발효 원료는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-084] 막걸리의 기본 발효 원료는?'), '쌀과 누룩', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-084] 막걸리의 기본 발효 원료는?'), '보리 맥아', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-084] 막걸리의 기본 발효 원료는?'), '포도', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-084] 막걸리의 기본 발효 원료는?'), '감자', FALSE);

-- 85
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-085] 동동주의 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-085] 동동주의 특징은?'), '뜬 밥알', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-085] 동동주의 특징은?'), '증류주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-085] 동동주의 특징은?'), '오크 숙성', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-085] 동동주의 특징은?'), '무알코올', FALSE);

-- 86
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-086] 약주/청주의 공통 특징은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-086] 약주/청주의 공통 특징은?'), '맑은 술(여과)', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-086] 약주/청주의 공통 특징은?'), '탄산 많음', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-086] 약주/청주의 공통 특징은?'), '증류주', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-086] 약주/청주의 공통 특징은?'), '산도 낮음', FALSE);

-- 87
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-087] 증류식 소주의 전통 원료로 흔한 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-087] 증류식 소주의 전통 원료로 흔한 것은?'), '쌀/고구마/보리', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-087] 증류식 소주의 전통 원료로 흔한 것은?'), '사탕수수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-087] 증류식 소주의 전통 원료로 흔한 것은?'), '사과', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-087] 증류식 소주의 전통 원료로 흔한 것은?'), '포도', FALSE);

-- 88
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-088] 탁주와 청주의 가장 큰 차이는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-088] 탁주와 청주의 가장 큰 차이는?'), '여과 여부', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-088] 탁주와 청주의 가장 큰 차이는?'), '도수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-088] 탁주와 청주의 가장 큰 차이는?'), '원료', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-088] 탁주와 청주의 가장 큰 차이는?'), '지역', FALSE);

-- 89
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-089] 막걸리 보관 권장 온도는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-089] 막걸리 보관 권장 온도는?'), '냉장 보관', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-089] 막걸리 보관 권장 온도는?'), '실온 고온', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-089] 막걸리 보관 권장 온도는?'), '영하 보관', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-089] 막걸리 보관 권장 온도는?'), '직사광선', FALSE);

-- 90
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-090] 소주 잔 돌려마시기 위생 측면은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-090] 소주 잔 돌려마시기 위생 측면은?'), '비권장', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-090] 소주 잔 돌려마시기 위생 측면은?'), '권장', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-090] 소주 잔 돌려마시기 위생 측면은?'), '상관없음', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-090] 소주 잔 돌려마시기 위생 측면은?'), '필수', FALSE);

-- 91
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-091] 국내 편의점 맥주 라벨의 도수 표기는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-091] 국내 편의점 맥주 라벨의 도수 표기는?'), 'ABV %', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-091] 국내 편의점 맥주 라벨의 도수 표기는?'), 'Proof', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-091] 국내 편의점 맥주 라벨의 도수 표기는?'), 'SRM', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-091] 국내 편의점 맥주 라벨의 도수 표기는?'), 'IBU', FALSE);

-- 92
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-092] 하이볼이 의미하는 것은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-092] 하이볼이 의미하는 것은?'), '위스키+탄산수 기반 롱드링크', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-092] 하이볼이 의미하는 것은?'), '보드카 샷', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-092] 하이볼이 의미하는 것은?'), '진+베르무트', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-092] 하이볼이 의미하는 것은?'), '와인 칵테일', FALSE);

-- 93
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-093] IBU는 무엇의 지표인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-093] IBU는 무엇의 지표인가?'), '쓴맛 강도', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-093] IBU는 무엇의 지표인가?'), '알코올 도수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-093] IBU는 무엇의 지표인가?'), '당도', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-093] IBU는 무엇의 지표인가?'), '산도', FALSE);

-- 94
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-094] SRM은 무엇의 지표인가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-094] SRM은 무엇의 지표인가?'), '맥주 색상', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-094] SRM은 무엇의 지표인가?'), '발효 속도', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-094] SRM은 무엇의 지표인가?'), '효모 수', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-094] SRM은 무엇의 지표인가?'), '가스 압력', FALSE);

-- 95
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-095] Proof 100은 ABV로 얼마인가? (미국식)', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-095] Proof 100은 ABV로 얼마인가? (미국식)'), '50%', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-095] Proof 100은 ABV로 얼마인가? (미국식)'), '40%', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-095] Proof 100은 ABV로 얼마인가? (미국식)'), '57.1%', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-095] Proof 100은 ABV로 얼마인가? (미국식)'), '30%', FALSE);

-- 96
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-096] 알코올은 일반적으로 어떤 순서로 체내 대사되는가?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-096] 알코올은 일반적으로 어떤 순서로 체내 대사되는가?'), '흡수→분포→대사→배설', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-096] 알코올은 일반적으로 어떤 순서로 체내 대사되는가?'), '대사→흡수→분포→배설', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-096] 알코올은 일반적으로 어떤 순서로 체내 대사되는가?'), '배설→흡수→대사→분포', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-096] 알코올은 일반적으로 어떤 순서로 체내 대사되는가?'), '분포→흡수→배설→대사', FALSE);

-- 97
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-097] 와인잔을 스템(다리) 잡는 주된 이유는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-097] 와인잔을 스템(다리) 잡는 주된 이유는?'), '온도 전달 줄이기', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-097] 와인잔을 스템(다리) 잡는 주된 이유는?'), '향을 늘리기', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-097] 와인잔을 스템(다리) 잡는 주된 이유는?'), '무게 증가', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-097] 와인잔을 스템(다리) 잡는 주된 이유는?'), '색 짙게', FALSE);

-- 98
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-098] 온더락 얼음의 큰 장점은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-098] 온더락 얼음의 큰 장점은?'), '천천히 녹아 희석 적음', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-098] 온더락 얼음의 큰 장점은?'), '향 강화', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-098] 온더락 얼음의 큰 장점은?'), '탄산 생성', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-098] 온더락 얼음의 큰 장점은?'), '도수 상승', FALSE);

-- 99
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-099] 드링크 메뉴에서 ''니트''의 의미는?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-099] 드링크 메뉴에서 ''니트''의 의미는?'), '얼음/물 없이 그대로', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-099] 드링크 메뉴에서 ''니트''의 의미는?'), '탄산수 함께', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-099] 드링크 메뉴에서 ''니트''의 의미는?'), '얼음 위에', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-099] 드링크 메뉴에서 ''니트''의 의미는?'), '물을 타서', FALSE);

-- 100
INSERT INTO quiz_question (question_text, place_id, category) VALUES ('[술-100] 토닉워터의 쓴맛 성분은?', NULL, '술');
INSERT INTO quiz_question_option (question_id, option_text, is_correct) VALUES
((SELECT id FROM quiz_question WHERE question_text='[술-100] 토닉워터의 쓴맛 성분은?'), '키니네', TRUE),
((SELECT id FROM quiz_question WHERE question_text='[술-100] 토닉워터의 쓴맛 성분은?'), '카페인', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-100] 토닉워터의 쓴맛 성분은?'), '타닌', FALSE),
((SELECT id FROM quiz_question WHERE question_text='[술-100] 토닉워터의 쓴맛 성분은?'), '벤조산', FALSE);
