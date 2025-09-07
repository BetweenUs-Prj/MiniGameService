package su.kdt.minigame.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PinUtil {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * PIN 검증 (4자리)
     */
    public boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }

    /**
     * PIN 해시 생성
     */
    public String hashPin(String pin) {
        if (!isValidPin(pin)) {
            throw new IllegalArgumentException("PIN must be 4 digits");
        }
        return passwordEncoder.encode(pin);
    }

    /**
     * PIN 검증
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        return passwordEncoder.matches(plainPin, hashedPin);
    }
}