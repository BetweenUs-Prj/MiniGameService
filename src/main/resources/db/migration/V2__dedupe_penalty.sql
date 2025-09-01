-- V2__dedupe_penalty.sql
-- 중복된 벌칙 데이터 정리 (slug 기준으로 최소 ID만 유지)

-- 1. 임시 테이블로 대표 ID 매핑
CREATE TEMPORARY TABLE penalty_canon AS
SELECT MIN(penalty_id) AS keep_id, slug, text 
FROM penalty 
GROUP BY slug;

-- 2. FK 정리 - game_session의 penalty_id 참조 수정
UPDATE game_session gs
JOIN penalty p ON gs.selected_penalty_id = p.penalty_id
JOIN penalty_canon pc ON p.slug = pc.slug
SET gs.selected_penalty_id = pc.keep_id
WHERE gs.selected_penalty_id != pc.keep_id;

-- 3. 중복 벌칙 삭제 (대표 ID 외 모든 중복 제거)
DELETE p FROM penalty p
JOIN penalty_canon pc ON p.slug = pc.slug
WHERE p.penalty_id > pc.keep_id;

-- 4. 임시 테이블 정리
DROP TEMPORARY TABLE penalty_canon;