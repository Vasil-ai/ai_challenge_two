package com.events.platform.web;

import com.events.platform.domain.User;
import com.events.platform.service.InviteService;
import com.events.platform.web.dto.InviteAcceptRequest;
import com.events.platform.web.dto.InviteCreateRequest;
import com.events.platform.web.dto.InviteLinkResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InviteApiController {

    private final InviteService inviteService;

    @PostMapping("/api/hosts/{hostId}/invites")
    public InviteLinkResponse create(@PathVariable Long hostId, @Valid @RequestBody InviteCreateRequest req) {
        User user = SecurityUtils.requireUser();
        String url = inviteService.createInvite(hostId, user, req.role());
        return new InviteLinkResponse(url);
    }

    @PostMapping("/api/invites/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody InviteAcceptRequest req) {
        User user = SecurityUtils.requireUser();
        inviteService.accept(req.token(), req.hostId(), user);
    }
}
