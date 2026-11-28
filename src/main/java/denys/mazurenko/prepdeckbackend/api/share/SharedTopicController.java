package denys.mazurenko.prepdeckbackend.api.share;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.dto.share.request.AcceptShareRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.CopyTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.service.share.SharedTopicWebService;
import denys.mazurenko.prepdeckbackend.service.share.TopicCopyWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.SHARES)
@RequiredArgsConstructor
public class SharedTopicController {

    private final SharedTopicWebService sharedTopicWebService;
    private final TopicCopyWebService topicCopyWebService;

    /**
     * Topics other people shared with the signed-in user, flat, each with its owner.
     */
    @GetMapping
    public ApiResponse<List<TopicResponse>> listShared(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(sharedTopicWebService.listShared(userId(jwt)));
    }

    /**
     * Opens a public link or an invitation link and answers the first topic it opened.
     */
    @PostMapping("/accept")
    public ApiResponse<TopicResponse> accept(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody AcceptShareRequest request) {
        return ApiResponse.ok(sharedTopicWebService.accept(userId(jwt), request));
    }

    /**
     * Marks the share a topic came through as opened.
     */
    @PostMapping("/{topicId}/seen")
    public ApiResponse<Void> markSeen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID topicId) {
        sharedTopicWebService.markSeen(userId(jwt), topicId);
        return ApiResponse.ok();
    }

    /**
     * Leaves the share a topic came through, with every topic below it.
     */
    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID topicId) {
        sharedTopicWebService.leave(userId(jwt), topicId);
        return ApiResponse.ok();
    }

    /**
     * Copies a topic the user can see, with its subtopics, cards and pictures, into their own topics.
     */
    @PostMapping("/{topicId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TopicResponse> copyToMine(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID topicId,
                                                 @Valid @RequestBody(required = false) CopyTopicRequest request) {
        return ApiResponse.ok(topicCopyWebService.copyToMine(userId(jwt), topicId, request));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
