package com.csk.services.library.events.producer.controller;

import com.csk.services.library.events.producer.producer.LibraryEventsProducer;
import com.csk.services.library.events.producer.util.TestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryController.class)
class LibraryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryEventsProducer libraryEventsProducer;

    @Test
    void works_createLibraryEvent() throws Exception {
        //given
        var eventPayload = TestUtil.eventPayload();
        var request = objectMapper.writeValueAsString(eventPayload);

        doNothing().when(libraryEventsProducer).publishLibraryEvent(eventPayload);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                .content(request)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void throws400_whenBookIdIsNull() throws Exception {
        //given
        var eventPayload = TestUtil.invalidEventPayload();
        var request = objectMapper.writeValueAsString(eventPayload);

        doNothing().when(libraryEventsProducer).publishLibraryEvent(eventPayload);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("book.bookId must not be null"));
    }
}