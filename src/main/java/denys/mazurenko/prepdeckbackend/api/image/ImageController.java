package denys.mazurenko.prepdeckbackend.api.image;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;
import denys.mazurenko.prepdeckbackend.service.image.ImageContent;
import denys.mazurenko.prepdeckbackend.service.image.ImageWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.IMAGES)
@RequiredArgsConstructor
public class ImageController {

    /** An image never changes under its id, so a client may keep it as long as it likes. */
    private static final CacheControl FOREVER = CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable();

    private final ImageWebService imageWebService;

    /**
     * Uploads a picture before the card is saved (multipart: file); the card then lists its id.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImageResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(imageWebService.upload(UUID.fromString(jwt.getSubject()), file));
    }

    /**
     * A picture's bytes; open without a token so an img tag can load it. The one endpoint that answers raw bytes, not ApiResponse.
     */
    @GetMapping(ApiPaths.IMAGE_CONTENT)
    public ResponseEntity<Resource> content(@PathVariable UUID id) {
        ImageContent content = imageWebService.content(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.type().getMediaType()))
                .contentLength(content.size())
                .cacheControl(FOREVER)
                .body(content.resource());
    }
}
