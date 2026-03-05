package com.angelina.shopping.orderservice.repo;

import com.angelina.shopping.orderservice.entity.Order;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface OrderRepository extends CassandraRepository<Order, UUID> {
}