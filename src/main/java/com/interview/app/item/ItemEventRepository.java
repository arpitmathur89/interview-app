package com.interview.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemEventRepository extends JpaRepository<ItemEvent, Long> {
    List<ItemEvent> findByItem_IdOrderByIdAsc(Long itemId);
}
