package denys.mazurenko.prepdeckbackend.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/** The picture formats a card accepts, recognised by their first bytes rather than the file name. */
@Getter
@RequiredArgsConstructor
public enum ImageType {
    PNG("image/png"),
    JPEG("image/jpeg"),
    WEBP("image/webp"),
    GIF("image/gif");

    private final String mediaType;

    /** The format the file starts like, or empty when it is none of ours. */
    public static Optional<ImageType> sniff(byte[] head) {
        return Arrays.stream(values()).filter(type -> type.matches(head)).findFirst();
    }

    private boolean matches(byte[] h) {
        return switch (this) {
            case PNG -> startsWith(h, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case JPEG -> startsWith(h, 0, 0xFF, 0xD8, 0xFF);
            case GIF -> startsWith(h, 0, 'G', 'I', 'F', '8');
            case WEBP -> startsWith(h, 0, 'R', 'I', 'F', 'F') && startsWith(h, 8, 'W', 'E', 'B', 'P');
        };
    }

    private static boolean startsWith(byte[] h, int offset, int... expected) {
        if (h.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((h[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
