package com.csk.services.library.events.consumer.processor;

import com.csk.services.library.events.consumer.repository.LibraryEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = "library-events")
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
public class EventListenerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    private MongoTemplate mongoTemplate;

    @SpyBean
    private EventListener  eventListener;

    @SpyBean
    private LibraryEventService libraryEventService;

    @BeforeEach
    void setUp() {

        for (MessageListenerContainer messageListenerContainer: endpointRegistry.getListenerContainers()) {

            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void cleanUp() {

        var query = Query.query(Criteria.where("book.bookName").is("test_book"));
        mongoTemplate.remove(query, "library-event");
    }

    @Test
    void publishLibraryEvent() throws ExecutionException, InterruptedException, IOException {
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
    void throwsException_whenEventIdIsNotPassedForUpdateEvent() throws InterruptedException, IOException, ExecutionException {
        //Given
        String eventPayload = "{\"eventId\":null,\"eventType\":\"UPDATE\",\"book\":{\"bookId\":0,\"bookName\":\"test_book\",\"bookAuthor\":\"test_author\"}}";

        kafkaTemplate.send("library-events", eventPayload).get();

        //When
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(3, TimeUnit.SECONDS); // wait for consumer to receive the message.

        //Then
        verify(eventListener, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(libraryEventService, times(3)).persistEvent(isA(ConsumerRecord.class));
    }

}