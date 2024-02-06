package com.csk.services.library.events.consumer.processor;

import com.csk.services.library.events.consumer.config.AppConfig;
import com.csk.services.library.events.consumer.repository.LibraryEventService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = { "library-events", "library-events.RETRY", "library-events.DLT" }, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "retry-listener.autostart=false"
})
class EventListenerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AppConfig appConfig;

    @SpyBean
    private EventListener  eventListener;

    @SpyBean
    private LibraryEventService libraryEventService;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {

        //wait for consumer to come up.
        var messageListenerContainer = endpointRegistry.getListenerContainers().stream()
                .filter(listenerContainer -> Objects.equals(listenerContainer.getGroupId(), "library-events-consumer-group"))
                .toList().get(0);

        ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());

        /*for (MessageListenerContainer messageListenerContainer: endpointRegistry.getListenerContainers()) {

            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }*/
    }

    @AfterEach
    void cleanUp() {

        var query = Query.query(Criteria.where("book.bookName").is("test_book"));
        mongoTemplate.remove(query, "library-event");
    }

    @Test
    void publishLibraryEvent() throws ExecutionException, InterruptedException {
        //Given
        String eventPayload = "{\"eventId\":null,\"eventType\":\"NEW\",\"book\":{\"bookId\":0,\"bookName\":\"test_book\",\"bookAuthor\":\"test_author\"}}";

        kafkaTemplate.send("library-events", eventPayload).get();

        //When
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(3, TimeUnit.SECONDS); // wait for consumer to receive the message.

        //Then
        verify(eventListener, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).persistEvent(isA(ConsumerRecord.class));
    }

    @Test
    void throwsException_whenEventIdIsNotPassedForUpdateEvent() throws InterruptedException, ExecutionException {
        //Given
        String eventPayload = "{\"eventId\":null,\"eventType\":\"UPDATE\",\"book\":{\"bookId\":0,\"bookName\":\"test_book\",\"bookAuthor\":\"test_author\"}}";

        kafkaTemplate.send("library-events", eventPayload).get();

        //When
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(3, TimeUnit.SECONDS); // wait for consumer to receive the message.

        //Then
        verify(eventListener, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).persistEvent(isA(ConsumerRecord.class));
    }

    @Test
    void throwsRecoverableException_whenEventIdIs999() throws InterruptedException, ExecutionException {
        //Given
        String eventPayload = "{\"eventId\":999,\"eventType\":\"UPDATE\",\"book\":{\"bookId\":0,\"bookName\":\"test_book\",\"bookAuthor\":\"test_author\"}}";

        kafkaTemplate.send("library-events", eventPayload).get();

        configureKafkaConsumer("retry-processor-group");

        //When
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(10, TimeUnit.SECONDS); // wait for consumer to receive the message.

        //Then
        verify(eventListener, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(3)).persistEvent(isA(ConsumerRecord.class));

        var consumerRecord = KafkaTestUtils.getSingleRecord(consumer, appConfig.getTopics().getRetry());

        assert consumerRecord != null;
        assertEquals(eventPayload, consumerRecord.value());
    }

    @Test
    void throwsNonRecoverableException_whenEventIdIsNull() throws ExecutionException, InterruptedException {

        //Given
        String eventPayload = "{\"eventId\":null,\"eventType\":\"UPDATE\",\"book\":{\"bookId\":0,\"bookName\":\"test_book\",\"bookAuthor\":\"test_author\"}}";

        kafkaTemplate.send("library-events", eventPayload).get();

        configureKafkaConsumer("dlt-processor-group");

        //When
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(3, TimeUnit.SECONDS); // wait for consumer to receive the message.

        //Then
        verify(eventListener, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(1)).persistEvent(isA(ConsumerRecord.class));

        var consumerRecord = KafkaTestUtils.getSingleRecord(consumer, appConfig.getTopics().getDlt());

        assert consumerRecord != null;
        assertEquals(eventPayload, consumerRecord.value());
    }

    private void configureKafkaConsumer(String groupName) {

        var config = new HashMap<>(KafkaTestUtils.consumerProps(groupName, "true", embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(config, new IntegerDeserializer(), new StringDeserializer())
                .createConsumer();

        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

}