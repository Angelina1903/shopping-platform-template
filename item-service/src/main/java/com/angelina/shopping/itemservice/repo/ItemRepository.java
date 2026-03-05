package com.angelina.shopping.itemservice.repo;

import com.angelina.shopping.itemservice.entity.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemRepository extends MongoRepository<Item, String> {
}