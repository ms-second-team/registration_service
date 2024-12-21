package ru.ms.second.team.registration.client.notification;

import ru.ms.second.team.registration.dto.registration.RegistrationNotification;

public interface NotificationSender {

    void sendNotification(RegistrationNotification registrationNotification);
}
