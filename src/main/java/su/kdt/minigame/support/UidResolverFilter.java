package su.kdt.minigame.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class UidResolverFilter extends OncePerRequestFilter {

    public static final String ATTR_UID = "X-USER-UID";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("X-USER-UID"); // **헤더 우선**
        String cookie = Optional.ofNullable(req.getCookies())
                .stream().flatMap(Arrays::stream)
                .filter(c -> "X-USER-UID".equalsIgnoreCase(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);

        String effective = header != null ? header : cookie;
        if (effective == null || effective.isBlank()) {
            // 개발 편의상만 기본값. 운영이라면 400으로 거절하는 것을 권장.
            effective = "1001"; // 숫자 UID로 변경
        }

        req.setAttribute(ATTR_UID, effective);
        // 디버그 로그 (필요 시)
        // log.debug("UID resolved: header={}, cookie={}, effective={}", header, cookie, effective);

        chain.doFilter(req, res);
    }
}