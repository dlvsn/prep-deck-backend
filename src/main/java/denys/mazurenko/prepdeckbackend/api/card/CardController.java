package denys.mazurenko.prepdeckbackend.api.card;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.card.request.DuplicateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.request.UpdateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.response.CardResponse;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.service.card.CardWebService;
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
@RequestMapping(ApiPaths.CARDS)
@RequiredArgsConstructor
public class CardController {

    private final CardWebService cardWebService;

    /**
     * Every tag on the signed-in user's cards, for suggestions in the editor.
     */
    @GetMapping("/tags")
    public ApiResponse<List<String>> tags(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(cardWebService.tags(userId(jwt)));
    }

    /**
     * One card.
     */
    @GetMapping("/{id}")
    public ApiResponse<CardResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(cardWebService.get(userId(jwt), id));
    }

    /**
     * Changes the fields that are set; a topicId moves the card to that topic.
     */
    @PatchMapping("/{id}")
    public ApiResponse<CardResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @Valid @RequestBody UpdateCardRequest request) {
        return ApiResponse.ok(cardWebService.update(userId(jwt), id, request));
    }

    /**
     * Deletes a card.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        cardWebService.delete(userId(jwt), id);
        return ApiResponse.ok();
    }

    /**
     * Copies a card into the same topic with its tags and images and no practice progress.
     */
    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CardResponse> duplicate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @Valid @RequestBody(required = false) DuplicateCardRequest request) {
        return ApiResponse.ok(cardWebService.duplicate(userId(jwt), id, request));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
