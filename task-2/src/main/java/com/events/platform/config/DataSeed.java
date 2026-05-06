package com.events.platform.config;

import com.events.platform.domain.CheckIn;
import com.events.platform.domain.ContentReport;
import com.events.platform.domain.Event;
import com.events.platform.domain.EventFeedback;
import com.events.platform.domain.EventLifecycle;
import com.events.platform.domain.EventVisibility;
import com.events.platform.domain.GalleryPhoto;
import com.events.platform.domain.GalleryPhotoStatus;
import com.events.platform.domain.Host;
import com.events.platform.domain.HostMembership;
import com.events.platform.domain.InviteLink;
import com.events.platform.domain.MembershipRole;
import com.events.platform.domain.ReportStatus;
import com.events.platform.domain.ReportTargetType;
import com.events.platform.domain.RsvpRegistration;
import com.events.platform.domain.RsvpStatus;
import com.events.platform.domain.Ticket;
import com.events.platform.domain.User;
import com.events.platform.repo.CheckInRepository;
import com.events.platform.repo.ContentReportRepository;
import com.events.platform.repo.EventFeedbackRepository;
import com.events.platform.repo.EventRepository;
import com.events.platform.repo.GalleryPhotoRepository;
import com.events.platform.repo.HostMembershipRepository;
import com.events.platform.repo.HostRepository;
import com.events.platform.repo.InviteLinkRepository;
import com.events.platform.repo.RsvpRegistrationRepository;
import com.events.platform.repo.TicketRepository;
import com.events.platform.repo.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeed {

    private final UserRepository userRepository;
    private final HostRepository hostRepository;
    private final HostMembershipRepository hostMembershipRepository;
    private final EventRepository eventRepository;
    private final RsvpRegistrationRepository rsvpRegistrationRepository;
    private final TicketRepository ticketRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final ContentReportRepository contentReportRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final CheckInRepository checkInRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            String pwd = passwordEncoder.encode(SeedDemoConstants.DEMO_PASSWORD_PLAINTEXT);

            User demo = newUser(SeedDemoConstants.USER_DEMO_EMAIL, pwd, "Demo User");
            User attendee = newUser(SeedDemoConstants.USER_ATTENDEE_EMAIL, pwd, "Seed Attendee");
            User checker = newUser(SeedDemoConstants.USER_CHECKER_EMAIL, pwd, "Seed Checker");
            userRepository.save(demo);
            userRepository.save(attendee);
            userRepository.save(checker);

            Host host = new Host();
            host.setSlug("demo-community");
            host.setDisplayName("Demo Community");
            host.setBio("Seeded host for SDD walkthrough.");
            host.setContactEmail("host@example.com");
            host.setLogoUrl(null);
            hostRepository.save(host);

            HostMembership hmHost = new HostMembership();
            hmHost.setUserId(demo.getId());
            hmHost.setHostId(host.getId());
            hmHost.setRole(MembershipRole.HOST);
            hostMembershipRepository.save(hmHost);

            HostMembership hmChecker = new HostMembership();
            hmChecker.setUserId(checker.getId());
            hmChecker.setHostId(host.getId());
            hmChecker.setRole(MembershipRole.CHECKER);
            hostMembershipRepository.save(hmChecker);

            Instant now = Instant.now();

            Event upcoming = new Event();
            upcoming.setHost(host);
            upcoming.setSlug("seed-meetup-upcoming");
            upcoming.setTitle("Community Meetup (Upcoming)");
            upcoming.setDescription("Seeded upcoming public event.");
            upcoming.setStartAt(now.plus(7, ChronoUnit.DAYS));
            upcoming.setEndAt(now.plus(7, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
            upcoming.setTimezone("UTC");
            upcoming.setVenueText("123 Demo Street, Demo City");
            upcoming.setOnlineUrl(null);
            upcoming.setCapacity(50);
            upcoming.setCoverImageUrl(null);
            upcoming.setVisibility(EventVisibility.PUBLIC);
            upcoming.setLifecycle(EventLifecycle.PUBLISHED);
            eventRepository.save(upcoming);

            Event past = new Event();
            past.setHost(host);
            past.setSlug("seed-meetup-past");
            past.setTitle("Past Workshop");
            past.setDescription("Seeded past event for Explore filters and post-event feedback.");
            past.setStartAt(now.minus(30, ChronoUnit.DAYS));
            past.setEndAt(now.minus(30, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS));
            past.setTimezone("UTC");
            past.setVenueText("Online");
            past.setOnlineUrl("https://example.com/join");
            past.setCapacity(100);
            past.setLifecycle(EventLifecycle.PUBLISHED);
            past.setVisibility(EventVisibility.PUBLIC);
            eventRepository.save(past);

            Event waitlistLab = new Event();
            waitlistLab.setHost(host);
            waitlistLab.setSlug("seed-waitlist-lab");
            waitlistLab.setTitle("Capacity One Lab (waitlist demo)");
            waitlistLab.setDescription("Ровно одно место: demo — confirmed, attendee — в очереди (см. README).");
            waitlistLab.setStartAt(now.plus(14, ChronoUnit.DAYS));
            waitlistLab.setEndAt(now.plus(14, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
            waitlistLab.setTimezone("Europe/Berlin");
            waitlistLab.setVenueText("Lab Berlin");
            waitlistLab.setCapacity(1);
            waitlistLab.setVisibility(EventVisibility.PUBLIC);
            waitlistLab.setLifecycle(EventLifecycle.PUBLISHED);
            eventRepository.save(waitlistLab);

            Event draft = new Event();
            draft.setHost(host);
            draft.setSlug("seed-draft-newsletter");
            draft.setTitle("Draft: Internal newsletter");
            draft.setDescription("Черновик только в Host dashboard.");
            draft.setStartAt(now.plus(21, ChronoUnit.DAYS));
            draft.setEndAt(now.plus(21, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
            draft.setTimezone("UTC");
            draft.setVenueText("TBD");
            draft.setCapacity(20);
            draft.setVisibility(EventVisibility.PUBLIC);
            draft.setLifecycle(EventLifecycle.DRAFT);
            eventRepository.save(draft);

            Event unlisted = new Event();
            unlisted.setHost(host);
            unlisted.setSlug("seed-unlisted-sync");
            unlisted.setTitle("Private team sync (Unlisted)");
            unlisted.setDescription("Опубликовано, но не в Explore — только прямая ссылка.");
            unlisted.setStartAt(now.plus(10, ChronoUnit.DAYS));
            unlisted.setEndAt(now.plus(10, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES));
            unlisted.setTimezone("UTC");
            unlisted.setVenueText("HQ / Zoom");
            unlisted.setCapacity(8);
            unlisted.setVisibility(EventVisibility.UNLISTED);
            unlisted.setLifecycle(EventLifecycle.PUBLISHED);
            eventRepository.save(unlisted);

            RsvpRegistration regUpcoming = persistRsvp(upcoming, demo, RsvpStatus.CONFIRMED, null);
            saveTicket(regUpcoming, SeedDemoConstants.TICKET_PUBLIC_CODE_UPCOMING);

            RsvpRegistration regPast = persistRsvp(past, attendee, RsvpStatus.CONFIRMED, null);
            Ticket pastTicket = saveTicket(regPast, SeedDemoConstants.TICKET_PUBLIC_CODE_PAST);

            RsvpRegistration regWaitDemo = persistRsvp(waitlistLab, demo, RsvpStatus.CONFIRMED, null);
            saveTicket(regWaitDemo, "D4E5F67890A1B2C3");
            persistRsvp(waitlistLab, attendee, RsvpStatus.WAITLISTED, 1);

            CheckIn pastCheckIn = new CheckIn();
            pastCheckIn.setTicket(pastTicket);
            pastCheckIn.setEvent(past);
            pastCheckIn.setCheckedInAt(past.getEndAt().minus(1, ChronoUnit.HOURS));
            pastCheckIn.setCheckedInBy(demo);
            pastCheckIn.setSessionId("seed-session");
            checkInRepository.save(pastCheckIn);

            GalleryPhoto pendingPhoto = new GalleryPhoto();
            pendingPhoto.setEvent(upcoming);
            pendingPhoto.setSubmittedBy(attendee);
            pendingPhoto.setImageUrl("https://placehold.co/800x450/png?text=Seed+Pending+Review");
            pendingPhoto.setStatus(GalleryPhotoStatus.PENDING);
            galleryPhotoRepository.save(pendingPhoto);

            GalleryPhoto approvedPast = new GalleryPhoto();
            approvedPast.setEvent(past);
            approvedPast.setSubmittedBy(demo);
            approvedPast.setImageUrl("https://placehold.co/800x450/png?text=Seed+Approved+Past");
            approvedPast.setStatus(GalleryPhotoStatus.APPROVED);
            galleryPhotoRepository.save(approvedPast);

            EventFeedback fb = new EventFeedback();
            fb.setEvent(past);
            fb.setUser(attendee);
            fb.setStars(5);
            fb.setComment("Отзыв из сидов: полезный воркшоп.");
            eventFeedbackRepository.save(fb);

            ContentReport report = new ContentReport();
            report.setTargetType(ReportTargetType.EVENT);
            report.setTargetEvent(upcoming);
            report.setTargetPhoto(null);
            report.setReporterUser(attendee);
            report.setReporterIp("203.0.113.42");
            report.setStatus(ReportStatus.OPEN);
            contentReportRepository.save(report);

            InviteLink invite = new InviteLink();
            invite.setHost(host);
            invite.setTokenHash(SeedDemoConstants.sha256Hex(SeedDemoConstants.CHECKER_INVITE_RAW_TOKEN));
            invite.setRole(MembershipRole.CHECKER);
            invite.setExpiresAt(null);
            inviteLinkRepository.save(invite);
        };
    }

    private static User newUser(String email, String passwordHash, String name) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setName(name);
        u.setCreatedAt(Instant.now());
        return u;
    }

    private RsvpRegistration persistRsvp(Event event, User user, RsvpStatus status, Integer waitlistPos) {
        RsvpRegistration reg = new RsvpRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        reg.setStatus(status);
        reg.setWaitlistPosition(waitlistPos);
        return rsvpRegistrationRepository.save(reg);
    }

    private Ticket saveTicket(RsvpRegistration reg, String publicCode) {
        Ticket t = new Ticket();
        t.setRegistration(reg);
        t.setPublicCode(publicCode);
        return ticketRepository.save(t);
    }
}
