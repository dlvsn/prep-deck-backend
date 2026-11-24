package denys.mazurenko.prepdeckbackend.domain;

/** Where uploaded card pictures are kept. */
public enum ImageStorageType {
    /** A folder on this machine ({@code prep-deck.images.directory}). */
    LOCAL,
    /** A bucket in any S3-compatible storage, e.g. Cloudflare R2 ({@code prep-deck.images.s3.*}). */
    S3
}
