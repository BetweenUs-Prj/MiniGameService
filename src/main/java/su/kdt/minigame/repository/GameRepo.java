package su.kdt.minigame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import su.kdt.minigame.domain.GameSession;
import java.util.List;

public interface GameRepo extends JpaRepository<GameSession, Long> {
    List<GameSession> findByAppointmentId(Long appointmentId);
}