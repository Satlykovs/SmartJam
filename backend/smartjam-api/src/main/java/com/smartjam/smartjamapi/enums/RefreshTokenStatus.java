package com.smartjam.smartjamapi.enums;

/**
 * Represents the lifecycle status of a refresh token stored in the database.
 *
 * <p>Status values are persisted as {@link String} via {@link jakarta.persistence.EnumType#STRING} in the
 * {@code refresh_tokens} table.
 */
public enum RefreshTokenStatus {
    /** The refresh token is valid and can be used to obtain a new access token. */
    ACTIVE,
    /**
     * The refresh token is no longer valid and cannot be used for token refresh. This covers tokens that have been
     * revoked, used, or expired.
     */
    INACTIVE
}
