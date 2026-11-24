package denys.mazurenko.prepdeckbackend.api.topic;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.card.request.CreateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.response.CardResponse;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.dto.topic.request.CreateTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.MoveTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.RenameTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.service.card.CardWebService;
import denys.mazurenko.prepdeckbackend.service.topic.TopicWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.TOPICS)
@RequiredArgsConstructor
public class TopicController {

    private final TopicWebService topicWebService;
    private final CardWebService cardWebService;

    /**
     * The signed-in user's topics, flat, with the number of cards directly in each.
     */
    @GetMapping
    public ApiResponse<List<TopicResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(topicWebService.listMine(userId(jwt)));
    }

    /**
     * Creates a topic at the end of its parent's children (parentId null: a root).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TopicResponse> create(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.ok(topicWebService.create(userId(jwt), request));
    }

    /**
     * Renames a topic.
     */
    @PatchMapping("/{id}")
    public ApiResponse<TopicResponse> rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                             @Valid @RequestBody RenameTopicRequest request) {
        return ApiResponse.ok(topicWebService.rename(userId(jwt), id, request));
    }

    /**
     * Moves a topic under a new parent at a given place; a topic cannot go inside itself.
     */
    @PostMapping("/{id}/move")
    public ApiResponse<TopicResponse> move(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody MoveTopicRequest request) {
        return ApiResponse.ok(topicWebService.move(userId(jwt), id, request));
    }

    /**
     * Deletes a topic with its subtopics and all their cards.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        topicWebService.delete(userId(jwt), id);
        return ApiResponse.ok();
    }

    /**
     * The cards directly in a topic, newest first.
     */
    @GetMapping("/{id}/cards")
    public ApiResponse<List<CardResponse>> cards(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(cardWebService.list(userId(jwt), id));
    }

    /**
     * Creates a card in the topic from its question, answer, tags and already uploaded images.
     */
    @PostMapping("/{id}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CardResponse> createCard(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                @Valid @RequestBody CreateCardRequest request) {
        return ApiResponse.ok(cardWebService.create(userId(jwt), id, request));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
