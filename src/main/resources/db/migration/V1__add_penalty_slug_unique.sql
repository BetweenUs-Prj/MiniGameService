-- V1__add_penalty_slug_unique.sql
-- 벌칙 테이블에 slug 컬럼 추가 및 UNIQUE 제약 설정

-- 1. slug 컬럼 추가
ALTER TABLE penalty ADD COLUMN slug VARCHAR(64);

-- 2. 기존 데이터에 slug 값 생성 (한글 → 영문 케밥케이스)
UPDATE penalty SET slug = 'buy-coffee' WHERE text = '커피 한 잔 사기' AND slug IS NULL;
UPDATE penalty SET slug = 'buy-ice-cream' WHERE text = '아이스크림 사기' AND slug IS NULL;
UPDATE penalty SET slug = 'buy-chicken' WHERE text = '치킨 한 마리 사기' AND slug IS NULL;
UPDATE penalty SET slug = 'sing-song' WHERE text = '노래 한 곡 부르기' AND slug IS NULL;
UPDATE penalty SET slug = 'dance-song' WHERE text = '댄스 한 곡 추기' AND slug IS NULL;

-- 3. 아직 slug가 없는 데이터를 위한 fallback (ID 기반)
UPDATE penalty SET slug = CONCAT('penalty-', penalty_id) WHERE slug IS NULL;

-- 4. slug 컬럼을 NOT NULL로 변경
ALTER TABLE penalty MODIFY slug VARCHAR(64) NOT NULL;

-- 5. slug에 UNIQUE 제약 추가
ALTER TABLE penalty ADD CONSTRAINT ux_penalty_slug UNIQUE (slug);