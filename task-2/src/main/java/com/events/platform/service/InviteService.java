package com.events.platform.service;

import com.events.platform.domain.Host;
import com.events.platform.domain.HostMembership;
import com.events.platform.domain.InviteLink;
import com.events.platform.domain.MembershipRole;
import com.events.platform.domain.User;
import com.events.platform.repo.HostMembershipRepository;
import com.events.platform.repo.HostRepository;
import com.events.platform.repo.InviteLinkRepository;
import com.events.platform.web.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteLinkRepository inviteLinkRepository;
    private final HostRepository hostRepository;
    private final HostMembershipRepository hostMembershipRepository;
    private final AccessControlService accessControlService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public String createInvite(Long hostId, User user, MembershipRole role) {
        if (!accessControlService.canManageHost(hostId, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Host host = hostRepository.findById(hostId).orElseThrow(() -> notFound());
        String raw = UUID.randomUUID().toString().replace("-", "");
        InviteLink link = new InviteLink();
        link.setHost(host);
        link.setTokenHash(sha256Hex(raw));
        link.setRole(role);
        inviteLinkRepository.save(link);
        return baseUrl.replaceAll("/$", "") + "/#/invite?token=" + raw + "&hostId=" + host.getId();
    }

    @Transactional
    public void accept(String rawToken, Long hostId, User user) {
        InviteLink link =
                inviteLinkRepository
                        .findByTokenHash(sha256Hex(rawToken))
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid invite"));
        if (!link.getHost().getId().equals(hostId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite does not match host");
        }
        if (link.getExpiresAt() != null && Instant.now().isAfter(link.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite expired");
        }
        if (hostMembershipRepository.existsByHostIdAndUserId(hostId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Already a member");
        }
        HostMembership m = new HostMembership();
        m.setUserId(user.getId());
        m.setHostId(hostId);
        m.setRole(link.getRole());
        hostMembershipRepository.save(m);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Not found");
    }
}
