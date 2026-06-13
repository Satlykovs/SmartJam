package com.smartjam.common.dto.connection;

import java.util.UUID;

public record StudentJoinedEvent(UUID connectionId, String studentName) {}
