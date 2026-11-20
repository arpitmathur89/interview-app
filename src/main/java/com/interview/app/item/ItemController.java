package com.interview.app.item;

import com.interview.app.item.ItemDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * POST   /api/items                 create          201 | 400 | 409
 * GET    /api/items?status=&page=&size=  list       200
 * GET    /api/items/{id}            get             200 | 404
 * PUT    /api/items/{id}            update          200 | 400 | 404 | 409
 * PATCH  /api/items/{id}/status     change status   200 | 400 | 404 | 409
 * GET    /api/items/{id}/events     status history  200 | 404
 * DELETE /api/items/{id}            delete          204 | 404
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) ItemStatus status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest body) {
        ItemResponse created = service.create(body);
        return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}/status")
    public ItemResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest body) {
        return service.changeStatus(id, body);
    }

    @GetMapping("/{id}/events")
    public List<ItemEventResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
