package com.csk.services.library.events.consumer.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "database_sequence")
class DatabaseSequence {

    @Id
    private String id;
    private int seq;
}
