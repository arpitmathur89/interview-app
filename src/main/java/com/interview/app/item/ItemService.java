package com.interview.app.item;

import com.interview.app.exception.NotFoundException;
import com.interview.app.item.ItemDtos.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICE layer: the business rules. Sits between the controller (HTTP) and the repositories (database).
 * <pre>
 *   ItemController  --calls-->  ItemService  --calls-->  ItemRepository
 *    (HTTP only)              (rules, transactions)       (SQL)
 * </pre>
 * Keeping rules here means the controller stays thin, the repositories stay simple, and the same rule
 * applies no matter which endpoint, test or future job calls it.
 *
 * <p><b>{@code @Service}</b> marks this class as a Spring bean, so Spring creates one shared instance and injects it
 * into {@code ItemController}.
 *
 * <p><b>{@code @Transactional}</b> on a method means everything it does in the database is one unit: if the
 * method finishes normally the changes are committed; if it throws an exception, they are all rolled back.
 * {@code readOnly = true} tells Spring and Hibernate the method only reads, which skips change-tracking work.
 *
 * <p><b>Errors</b> are reported by throwing exceptions (e.g. {@link NotFoundException}); the service knows nothing
 * about HTTP status codes. {@code ApiExceptionHandler} turns them into JSON error responses.
 *
 * <p>Methods take and return DTOs ({@code ItemDtos}), and entities never leave this class. That conversion happens
 * inside the transaction, while the data is still loaded.
 */
@Service
public class ItemService {

    private final ItemRepository items;

    /**
     * Constructor injection: Spring sees this constructor needs the repository, finds the bean it generated, and
     * passes it in. {@code final} fields can't be reassigned, which makes the class safer.
     */
    public ItemService(ItemRepository items) {
        this.items = items;
    }

    /** Returns every item, ordered by id. */
    @Transactional(readOnly = true)
    public List<ItemResponse> list() {
        return items.findAll(Sort.by("id")).stream().map(ItemResponse::from).toList();
    }

    /** Returns one item. Throws {@link NotFoundException} (becomes 404) if the id doesn't exist. */
    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return ItemResponse.from(find(id));
    }

    /**
     * Creates a new item.
     *
     * <p>Duplicate names are <b>not</b> checked with a SELECT first. The insert simply runs, and if the name already
     * exists the database's unique constraint rejects it, Spring throws {@code DataIntegrityViolationException},
     * and the handler returns 409. This can't be beaten by two requests arriving at the same moment.
     *
     * <p>{@code saveAndFlush} (instead of {@code save}) sends the INSERT immediately, so the duplicate error happens
     * here and the generated id is available for the response.
     */
    @Transactional
    public ItemResponse create(CreateItemRequest req) {
        Item saved = items.saveAndFlush(new Item(req.name().trim(), req.description()));
        return ItemResponse.from(saved);
    }

    /**
     * Replaces name and description of an existing item (PUT semantics). 404 if missing; 409 if the new name is
     * already used by another item. {@code updatedAt} is refreshed by the entity's {@code @PreUpdate} hook.
     */
    @Transactional
    public ItemResponse update(Long id, UpdateItemRequest req) {
        Item item = find(id);
        item.setName(req.name().trim());
        item.setDescription(req.description());
        return ItemResponse.from(items.saveAndFlush(item));
    }

    /** Deletes an item (404 if missing). */
    @Transactional
    public void delete(Long id) {
        items.delete(find(id));
    }

    /**
     * Shared helper: load an item or fail with 404. {@code findById} returns an {@code Optional}, which is a box that is
     * either empty or holds the item; {@code orElseThrow} unwraps it or throws the given exception.
     */
    private Item find(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("Item " + id + " not found"));
    }
}
