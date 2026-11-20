package com.interview.app.item;

import com.interview.app.common.ConflictException;
import com.interview.app.common.NotFoundException;
import com.interview.app.item.ItemDtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business rules + transaction boundaries. Controllers stay thin; repositories stay dumb. */
@Service
public class ItemService {

    private final ItemRepository items;
    private final ItemEventRepository events;

    public ItemService(ItemRepository items, ItemEventRepository events) {
        this.items = items;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(ItemStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("id"));
        var result = (status == null) ? items.findAll(pageable) : items.findByStatus(status, pageable);
        return result.map(ItemResponse::from).getContent();
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return ItemResponse.from(find(id));
    }

    /** Duplicate names hit the DB unique constraint -> DataIntegrityViolationException -> 409. */
    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        Item saved = items.saveAndFlush(new Item(req.name().trim(), req.description()));
        return ItemResponse.from(saved);
    }

    @Transactional
    public ItemResponse update(Long id, UpdateItemRequest req) {
        Item item = find(id);
        item.setName(req.name().trim());
        item.setDescription(req.description());
        return ItemResponse.from(items.saveAndFlush(item));
    }

    /**
     * Status change + audit row in one transaction: both commit or neither does.
     * Same status again = idempotent no-op (safe for client retries). Illegal move = 409.
     */
    @Transactional
    public ItemResponse changeStatus(Long id, StatusChangeRequest req) {
        Item item = find(id);
        ItemStatus from = item.getStatus();
        if (from == req.status()) {
            return ItemResponse.from(item);
        }
        if (!from.canTransitionTo(req.status())) {
            throw new ConflictException("Cannot move item " + id + " from " + from + " to " + req.status());
        }
        item.setStatus(req.status());
        events.save(new ItemEvent(item, from, req.status(), req.reason()));
        return ItemResponse.from(items.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public List<ItemEventResponse> history(Long id) {
        find(id); // 404 if the item doesn't exist, rather than an empty list
        return events.findByItem_IdOrderByIdAsc(id).stream().map(ItemEventResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        items.delete(find(id));
    }

    private Item find(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("Item " + id + " not found"));
    }
}
