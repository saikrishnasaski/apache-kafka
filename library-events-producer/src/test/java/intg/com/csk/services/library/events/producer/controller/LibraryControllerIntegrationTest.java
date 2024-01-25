package com.csk.services.library.events.producer.controller;

import com.csk.services.library.events.producer.domain.EventPayload;
import com.csk.services.library.events.producer.util.TestUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedKafka(topics = "library-events")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class LibraryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {

        var config = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        consumer = new DefaultKafkaConsumerFactory<>(config, new IntegerDeserializer(), new StringDeserializer())
                .createConsumer();

        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {

        consumer.close();
    }

    @Test
    void createLibraryEvent() {
        // Given
        var libraryEvent = TestUtil.eventPayload();
        var httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", MediaType.APPLICATION_JSON_VALUE);

        var httpEntity = new HttpEntity<>(libraryEvent, httpHeaders);

        //when
        var response = testRestTemplate.exchange("/v1/libraryevent", HttpMethod.POST,
                httpEntity, EventPayload.class);

        var consumerRecords = consumer.poll(Duration.ofMillis(2000));

        //then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assert consumerRecords.count() == 1;

        consumerRecords.records("library-events").forEach(record -> {
            assertEquals(libraryEvent.eventId(), record.key());
            assertEquals(CONSUMER_RECORD_VALUE, record.value());
        });
    }

    private static final String CONSUMER_RECORD_VALUE = "{\"eventId\":121,\"eventType\":\"NEW\",\"book\":{\"bookId\":12,\"bookName\":\"Haraveer Reddy\",\"bookAuthor\":\"AstroPhysics\"}}";
}