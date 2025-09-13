package com.project.matchingengine.repository.order;

import com.project.matchingengine.models.order.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepo extends JpaRepository<Order, UUID>  {

}