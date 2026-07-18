package com.shareway.application.usecase;

import com.shareway.application.dto.request.RecordVisitRequest;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.VisitorRowResponse;
import com.shareway.application.dto.response.VisitorStatsResponse;
import com.shareway.domain.model.User;
import com.shareway.domain.model.Visitor;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitorUseCase {

    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;

    private static final String ADMIN_EMAIL = VisitorRepository.ADMIN_EMAIL;

    public Visitor recordVisit(RecordVisitRequest req, String userId, String ip) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Visitor visitor = Visitor.builder()
                .userId(userId)
                .userName(user != null ? user.getFirstName() + " " + user.getLastName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .anonymousId(req.getAnonymousId())
                .ipAddress(ip)
                .pageUrl(req.getPageUrl())
                .referrer(req.getReferrer())
                .userAgent(req.getUserAgent())
                .acceptedCookies(req.isAcceptedCookies())
                .build();

        return visitorRepository.save(visitor);
    }

    public void updateCookiesAccepted(String anonymousId, boolean accepted) {
        List<Visitor> visitors = visitorRepository.findAll().stream()
                .filter(v -> anonymousId.equals(v.getAnonymousId()))
                .toList();
        visitors.forEach(v -> v.setAcceptedCookies(accepted));
        visitorRepository.saveAll(visitors);
    }

    public VisitorStatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        long totalVisits = visitorRepository.countExcluding(ADMIN_EMAIL);
        long cookiesAccepted = visitorRepository.countByAcceptedCookiesTrueAndUserEmailNot(ADMIN_EMAIL);
        long cookiesRejected = visitorRepository.countByAcceptedCookiesFalseAndUserEmailNot(ADMIN_EMAIL);

        Map<String, Long> byCountry = new LinkedHashMap<>();
        visitorRepository.countByCountryExcluding(ADMIN_EMAIL).forEach(row ->
                byCountry.put((String) row[0], (Long) row[1]));

        Map<String, Long> byCity = new LinkedHashMap<>();
        visitorRepository.countByCityExcluding(ADMIN_EMAIL).forEach(row ->
                byCity.put((String) row[0], (Long) row[1]));

        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = now.toLocalDate().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            long count = visitorRepository.countBetweenExcluding(dayStart, dayEnd, ADMIN_EMAIL);
            byDay.put(date.format(DateTimeFormatter.ofPattern("dd/MM")), count);
        }

        long loggedUsers = visitorRepository.countByUserIdIsNotNullAndUserEmailNot(ADMIN_EMAIL);

        return VisitorStatsResponse.builder()
                .totalVisits(totalVisits)
                .uniqueUsers(visitorRepository.countDistinctUsersSinceExcluding(startOfMonth, ADMIN_EMAIL))
                .uniqueAnonymous(visitorRepository.countDistinctAnonymousSinceExcluding(startOfMonth, ADMIN_EMAIL))
                .totalAnonymous(totalVisits - loggedUsers)
                .cookiesAccepted(cookiesAccepted)
                .cookiesRejected(cookiesRejected)
                .visitsByCountry(byCountry)
                .visitsByCity(byCity)
                .visitsByDay(byDay)
                .build();
    }

    public PageResponse<VisitorRowResponse> getAllVisitors(String search, String country, Boolean cookiesAccepted, int page, int size) {
        Page<Visitor> visitors = visitorRepository.searchExcluding(ADMIN_EMAIL, search, country, cookiesAccepted, PageRequest.of(page, size));
        return PageResponse.from(visitors.map(this::toRow));
    }

    private VisitorRowResponse toRow(Visitor v) {
        return VisitorRowResponse.builder()
                .id(v.getId())
                .userName(v.getUserName())
                .userEmail(v.getUserEmail())
                .anonymousId(v.getAnonymousId())
                .country(v.getCountry())
                .city(v.getCity())
                .pageUrl(v.getPageUrl())
                .acceptedCookies(v.isAcceptedCookies())
                .visitedAt(v.getVisitedAt())
                .build();
    }
}
