package com.clinica.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "clinic.calendar.google")
public class GoogleCalendarProperties {
    private boolean enabled = false;
    private String calendarId = "primary";
    private String credentialsPath;
    private String credentialsJson;
    private String timezone = "America/Lima";
    private boolean sendNotifications = false;
}
