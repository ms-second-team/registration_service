package ru.ms.second.team.registration.client.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.ms.second.team.registration.dto.registration.RegistrationNotification;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationSender implements NotificationSender {

    private final KafkaTemplate<String, RegistrationNotification> kafkaTemplate;

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    @Override
    public void sendNotification(RegistrationNotification registrationNotification) {
        log.info("Sending notification: '{}'", registrationNotification);
        kafkaTemplate.send(topic, registrationNotification);
    }
}
