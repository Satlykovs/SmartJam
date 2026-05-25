package com.smartjam.smartjamapi.controller;

import com.smartjam.api.api.DevicesApi;
import com.smartjam.api.model.DeviceRegistrationRequest;
import com.smartjam.smartjamapi.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller implementing the {@link DevicesApi} interface generated from the OpenAPI specification. Provides endpoints
 * for mobile clients to manage their notification tokens.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DevicesController implements DevicesApi {

    private final DeviceService deviceService;

    @Override
    public ResponseEntity<Void> registerDevice(DeviceRegistrationRequest body) {
        log.info("Request to register device received");
        deviceService.register(body.token());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unregisterDevice(DeviceRegistrationRequest body) {
        log.info("Request to unregister device received");
        deviceService.unregister(body.token());
        return ResponseEntity.noContent().build();
    }
}
