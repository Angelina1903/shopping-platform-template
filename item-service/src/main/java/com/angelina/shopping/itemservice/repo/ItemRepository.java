package com.angelina.shopping.itemservice.repo;

import com.angelina.shopping.itemservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}