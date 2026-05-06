package com.events.platform.service;

import com.events.platform.domain.CheckIn;
import com.events.platform.domain.Event;
import com.events.platform.domain.RsvpRegistration;
import com.events.platform.domain.Ticket;
import com.events.platform.domain.User;
import com.events.platform.repo.CheckInRepository;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.repo.TicketRepository;
import com.events.platform.web.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final EventRepository eventRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;
    private final TicketRepository ticketRepository;
    private final CheckInRepository checkInRepository;
    private final AccessControlService accessControlService;

    public byte[] exportRsvps(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound());
        if (!accessControlService.canManageHost(event.getHost().getId(), user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        List<RsvpRegistration> rows = rsvpRegistrationRepository.findExportRows(eventId);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("name,email,RSVP status,check-in time\n");
        for (RsvpRegistration r : rows) {
            User u = r.getUser();
            String checkInStr = "";
            Optional<Ticket> ticket = ticketRepository.findByRegistrationId(r.getId());
            if (ticket.isPresent()) {
                Optional<CheckIn> ci = checkInRepository.findByTicketIdAndUndoneAtIsNull(ticket.get().getId());
                if (ci.isPresent()) {
                    checkInStr = ISO.format(ci.get().getCheckedInAt());
                }
            }
            sb.append(escape(u.getName()))
                    .append(',')
                    .append(escape(u.getEmail()))
                    .append(',')
                    .append(r.getStatus().name())
                    .append(',')
                    .append(escape(checkInStr))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String v) {
        if (v == null) {
            return "";
        }
        boolean needQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String s = v.replace("\"", "\"\"");
        return needQuote ? "\"" + s + "\"" : s;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
