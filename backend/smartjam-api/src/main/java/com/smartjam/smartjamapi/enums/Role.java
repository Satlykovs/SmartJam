package com.smartjam.smartjamapi.enums;

/**
 * Represents the role of a user within the SmartJam platform.
 *
 * <p>Roles are used to control access and permissions across the application. The role value is persisted as a {`@link`
 * String} in the database via {`@link` jakarta.persistence.EnumType#STRING}.
 */
public enum Role {
    STUDENT,
    TEACHER
}
