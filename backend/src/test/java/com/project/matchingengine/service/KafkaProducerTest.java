package com.project.matchingengine.service;

import org.mockito.Mock;
import org.mockito.InjectMocks;

import org.springframework.kafka.core.KafkaTemplate;

import com.project.matchingengine.service.kafka.KafkaProducer;




public class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducer myProducer;


}
