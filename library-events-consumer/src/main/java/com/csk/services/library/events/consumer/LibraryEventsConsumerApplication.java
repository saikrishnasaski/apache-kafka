package com.csk.services.library.events.consumer;

import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryEventsConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryEventsConsumerApplication.class, args);
	}

}
