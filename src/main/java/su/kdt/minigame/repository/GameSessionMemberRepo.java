package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.GameSessionMember;
import su.kdt.minigame.domain.GameSessionMemberId;

import java.util.List;
import java.util.Optional;

public interface GameSessionMemberRepo extends JpaRepository<GameSessionMember, GameSessionMemberId> {
    
    @Query("SELECT COUNT(gsm) FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT COUNT(gsm) FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId AND gsm.isReady = true")
    long countBySessionIdAndIsReadyTrue(@Param("sessionId") Long sessionId);
    
    @Query("SELECT gsm FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId ORDER BY gsm.joinedAt")
    List<GameSessionMember> findBySessionIdOrderByJoinedAt(@Param("sessionId") Long sessionId);
    
    Optional<GameSessionMember> findBySessionIdAndUserUid(Long sessionId, String userUid);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId AND gsm.userUid = :userUid")
    void deleteBySessionIdAndUserUid(@Param("sessionId") Long sessionId, @Param("userUid") String userUid);
    
    @Query("SELECT gsm FROM GameSessionMember gsm WHERE gsm.sessionId = :sessionId")
    List<GameSessionMember> findBySessionId(@Param("sessionId") Long sessionId);
}