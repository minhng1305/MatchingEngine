package com.project.matchingengine.service.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides the list of Kafka topics this server instance should consume from.
 * Each server deployment configures different symbols in application-server1.properties.
 */
@Component("topicListProvider")
public class TopicListProvider {

    @Value("${app.kafka.topics.order-submission-prefix}")
    private String orderTopicPrefix;

    @Value("${app.kafka.topics.assigned-symbols}")
    private String assignedSymbols;

    /**
     * Converts assigned symbols (e.g., "AAPL,MSFT,GOOGL") into topic names
     * (e.g., ["order-aapl", "order-msft", "order-googl"])
     */
    public String[] getTopics() {
        if (assignedSymbols == null || assignedSymbols.trim().isEmpty()) {
            throw new IllegalStateException("No symbols assigned to this server instance. " +
                    "Please configure app.kafka.topics.assigned-symbols in application-server1.properties");
        }

        List<String> topics = Arrays.stream(assignedSymbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .map(symbol -> orderTopicPrefix + symbol.toLowerCase())
                .collect(Collectors.toList());

        System.out.println("====================================");
        System.out.println("Server will consume from topics: " + topics);
        System.out.println("====================================");

        return topics.toArray(new String[0]);
    }
}
