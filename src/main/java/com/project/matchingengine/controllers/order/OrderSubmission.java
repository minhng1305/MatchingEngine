package com.project.matchingengine.controllers.order;

import com.project.matchingengine.models.order.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * OrderSubmission REST Controller handles incoming HTTP requests
 * for submitting new orders. It publishes these orders to a Kafka topic.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderSubmission {

    // KafkaTemplate for sending Order objects
    private final KafkaTemplate<String, Order> orderKafkaTemplate;

    // Inject the Kafka topic name for order submissions
    @Value("${app.kafka.topics.order-submission}")
    private String orderSubmissionTopic;

    /**
     * Constructor for OrderSubmission. Spring will automatically inject
     * the KafkaTemplate<String, Order> bean.
     * @param orderKafkaTemplate The KafkaTemplate for sending orders.
     */
    public OrderSubmission(KafkaTemplate<String, Order> orderKafkaTemplate) {
        this.orderKafkaTemplate = orderKafkaTemplate;
    }

    /**
     * Handles POST requests to submit a new order.
     * The received Order object is enriched (if necessary, though not strictly required for this example)
     * and then sent to the Kafka order submission topic.
     * @param order The Order object received from the client.
     * @return A confirmation message.
     */
    @PostMapping
    public String submitOrder(@RequestBody Order order) {
        // In a real application, you'd add more robust validation here.
        // You might also generate the orderId and orderTimestamp if they are not
        // reliably provided by the client, ensuring uniqueness and accuracy.
        if (order.getOrderId() == null) {
            order.setOrderId(UUID.randomUUID()); // Ensure orderId is set
        }
        if (order.getOrderTimestamp() == null) {
            order.setOrderTimestamp(new Timestamp(System.currentTimeMillis())); // Ensure timestamp is set
        }

        System.out.println("Received order for submission: " + order.getOrderId() + " for symbol " + order.getSymbol());
        // Send the order to the Kafka topic, using orderId as the key for partitioning
        orderKafkaTemplate.send(orderSubmissionTopic, order.getOrderId().toString(), order);
        return "Order submitted to Kafka successfully! Order ID: " + order.getOrderId();
    }
}
