package denys.mazurenko.prepdeckbackend.dto.image.response;

/** The data of an "images.tooSmall" refusal: the smallest size accepted, in pixels. */
public record MinImageSizeResponse(int minWidth, int minHeight) {
}
