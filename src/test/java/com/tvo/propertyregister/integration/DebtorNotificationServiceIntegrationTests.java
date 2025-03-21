package com.tvo.propertyregister.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvo.propertyregister.exception.DontHaveTaxDebtsException;
import com.tvo.propertyregister.exception.NoDebtorsInDebtorListException;
import com.tvo.propertyregister.exception.NoSuchOwnerException;
import com.tvo.propertyregister.integration.config.TestConfig;
import com.tvo.propertyregister.model.dto.EmailEventDto;
import com.tvo.propertyregister.model.dto.EmailType;
import com.tvo.propertyregister.model.owner.FamilyStatus;
import com.tvo.propertyregister.model.owner.Owner;
import com.tvo.propertyregister.model.property.Property;
import com.tvo.propertyregister.model.property.PropertyCondition;
import com.tvo.propertyregister.model.property.PropertyType;
import com.tvo.propertyregister.repository.OwnerRepository;
import com.tvo.propertyregister.service.DebtorNotificationService;
import com.tvo.propertyregister.service.EmailSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tvo.propertyregister.service.utils.Constants.EMAIL_TOPIC;
import static org.junit.Assert.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class DebtorNotificationServiceIntegrationTests {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DebtorNotificationService debtorNotificationService;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private ObjectMapper mapper;

    private static final Property HOUSE_1 = new Property(2, PropertyType.HOUSE, "Prague", "Boris Niemcov Street 220",
            150, 5, new BigDecimal("750000"),
            LocalDate.of(2020, 4, 10),
            LocalDate.of(2012, 1, 9),
            PropertyCondition.GOOD);

    private static final RabbitMQContainer RABBIT_MQ_CONTAINER =
            new RabbitMQContainer("rabbitmq:3.9-management");

    @BeforeAll
    static void setUp() {
        RABBIT_MQ_CONTAINER.start();
        System.setProperty("spring.rabbitmq.host", RABBIT_MQ_CONTAINER.getHost());
        System.setProperty("spring.rabbitmq.port", RABBIT_MQ_CONTAINER.getAmqpPort().toString());
    }

    @BeforeEach
    void cleanUp() {
        ownerRepository.clear();
    }

    @Test
    void should_send_notification_to_all_debtors() {
        Owner debtor = new Owner(1, "Frank", "John",
                30, FamilyStatus.SINGLE,
                false, "frankjohn@gmail.com",
                "+456987123",
                LocalDate.of(1994, 5, 9),
                new BigDecimal("10000"), List.of(HOUSE_1));

        ownerRepository.save(debtor);

        debtorNotificationService.notifyAllDebtors();

        String body = (String) rabbitTemplate.receiveAndConvert(EMAIL_TOPIC);

        assertTrue(Objects.nonNull(body));
    }

    @Test
    void should_not_notify_debtors_if_the_list_is_empty() {
        assertThrows(NoDebtorsInDebtorListException.class, () -> debtorNotificationService.notifyAllDebtors());
    }

    @Test
    void should_send_email_event_to_rabbitmq_queue() throws JsonProcessingException {
        EmailEventDto expectedEvent = new EmailEventDto(
                "terebylov@ssemi.cz",
                EmailType.ALL_DEBTOR_NOTIFICATION,
                Map.of("firstName", "John", "lastName", "Doe", "debt", "10000")
        );

        emailSender.send(expectedEvent);

        String body = (String) rabbitTemplate.receiveAndConvert(EMAIL_TOPIC);

        assertTrue(Objects.nonNull(body));

        EmailEventDto receivedEvent = mapper.readValue(body, EmailEventDto.class);

        assertTrue(Objects.nonNull(receivedEvent));
        assertEquals(expectedEvent.email(), receivedEvent.email());
        assertEquals(expectedEvent.type(), receivedEvent.type());
        assertEquals(expectedEvent.params(), receivedEvent.params());
    }

    @Test
    void should_send_notification_to_certain_debtor_by_id() {
        Owner debtor = new Owner(1, "Frank", "John",
                30, FamilyStatus.SINGLE,
                false, "frankjohn@gmail.com",
                "+456987123",
                LocalDate.of(1994, 5, 9),
                new BigDecimal("10000"), List.of(HOUSE_1));

        ownerRepository.save(debtor);

        debtorNotificationService.notifyDebtorById(debtor.getId());

        String body = (String) rabbitTemplate.receiveAndConvert(EMAIL_TOPIC);

        assertTrue(Objects.nonNull(body));
    }

    @Test
    void should_not_send_notification_to_certain_debtor_by_id_if_id_is_wrong() {
        assertThrows(NoSuchOwnerException.class, () -> debtorNotificationService.notifyDebtorById(1));
    }

    @Test
    void should_not_send_notification_to_certain_debtor_by_id_if_the_owner_debt_is_zero() {
        Owner debtor = new Owner(1, "Frank", "John",
                30, FamilyStatus.SINGLE,
                false, "frankjohn@gmail.com",
                "+456987123",
                LocalDate.of(1994, 5, 9),
                new BigDecimal("0"), List.of(HOUSE_1));

        ownerRepository.save(debtor);

        assertThrows(DontHaveTaxDebtsException.class, () -> debtorNotificationService.notifyDebtorById(debtor.getId()));
    }
}
