package denys.mazurenko.prepdeckbackend.api.share;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.dto.share.request.InviteRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.ShareTopicsRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.UpdateLinkRequest;
import denys.mazurenko.prepdeckbackend.dto.share.response.SharingResponse;
import denys.mazurenko.prepdeckbackend.service.share.SharingWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/** The sharing of the tree the topic {id} belongs to; every call answers it as it stands after. */
@RestController
@RequestMapping(ApiPaths.TOPICS + ApiPaths.TOPIC_SHARING)
@RequiredArgsConstructor
public class SharingController {

    private final SharingWebService sharingWebService;

    /**
     * Who can see the topic's tree: people, waiting invitations, the public link.
     */
    @GetMapping
    public ApiResponse<SharingResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(sharingWebService.get(userId(jwt), id));
    }

    /**
     * Invites someone by email or username to topics of the tree and emails them a link.
     */
    @PostMapping("/invites")
    public ApiResponse<SharingResponse> invite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @Valid @RequestBody InviteRequest request, Locale locale) {
        return ApiResponse.ok(sharingWebService.invite(userId(jwt), id, request, locale));
    }

    /**
     * Revokes an invitation not accepted yet; its link stops working.
     */
    @DeleteMapping("/invites/{inviteId}")
    public ApiResponse<SharingResponse> revokeInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @PathVariable UUID inviteId) {
        return ApiResponse.ok(sharingWebService.revokeInvite(userId(jwt), id, inviteId));
    }

    /**
     * Replaces the topics of the tree a person may see.
     */
    @PutMapping("/people/{memberId}/topics")
    public ApiResponse<SharingResponse> changeTopics(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @PathVariable UUID memberId,
                                                     @Valid @RequestBody ShareTopicsRequest request) {
        return ApiResponse.ok(sharingWebService.changeTopics(userId(jwt), id, memberId, request));
    }

    /**
     * Takes the whole tree from a person; the public link no longer lets them back in.
     */
    @DeleteMapping("/people/{memberId}")
    public ApiResponse<SharingResponse> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @PathVariable UUID memberId) {
        return ApiResponse.ok(sharingWebService.removeMember(userId(jwt), id, memberId));
    }

    /**
     * Turns the tree's public link on or off and sets the topics it opens.
     */
    @PutMapping("/link")
    public ApiResponse<SharingResponse> updateLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateLinkRequest request) {
        return ApiResponse.ok(sharingWebService.updateLink(userId(jwt), id, request));
    }

    /**
     * Gives the public link a new token; the old link stops working, people keep their access.
     */
    @PostMapping("/link/reset")
    public ApiResponse<SharingResponse> resetLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(sharingWebService.resetLink(userId(jwt), id));
    }

    /**
     * Takes the tree from everyone who came through the public link alone.
     */
    @DeleteMapping("/link/people")
    public ApiResponse<SharingResponse> removeLinkMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(sharingWebService.removeLinkMembers(userId(jwt), id));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
