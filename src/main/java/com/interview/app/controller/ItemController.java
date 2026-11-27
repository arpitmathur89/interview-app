package com.interview.app.controller;

import com.interview.app.service.ItemService;
import com.interview.app.dto.ItemDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * CONTROLLER layer: turns HTTP requests into service calls, and service results back into HTTP responses.
 *
 * <p>Endpoints (and the status codes each can return):
 * <pre>
 *   POST   /api/items                     create          201 | 400 | 409 | 422
 *   GET    /api/items                     list all        200
 *   GET    /api/items/{id}                get one         200 | 400 | 404
 *   PUT    /api/items/{id}                update          200 | 400 | 404 | 409 | 422
 *   DELETE /api/items/{id}                delete          204 | 400 | 404
 * </pre>
 * (400 = unparseable JSON or id; 422 = a field failed validation; see {@code ApiExceptionHandler}.)
 *
 * <p>How Spring finds this code: every request goes to Spring MVC's {@code DispatcherServlet}, which matches the HTTP
 * method and path against the mapping annotations below and calls the matching method.
 * <ul>
 *   <li>{@code @RestController}: a controller whose return values are written as JSON (by Jackson), not
 *       rendered as an HTML template.</li>
 *   <li>{@code @RequestMapping("/api/items")}: prefix shared by every method in this class.</li>
 *   <li>{@code @GetMapping}, {@code @PostMapping}, ...: which HTTP method + sub-path a method handles.</li>
 *   <li>{@code @PathVariable}: takes a value from the URL ({@code /api/items/5} gives {@code id = 5}).</li>
 *   <li>{@code @RequestBody}: turns the JSON body into a DTO record.</li>
 *   <li>{@code @Valid}: runs the DTO's validation annotations first; on failure the method is never called.</li>
 * </ul>
 *
 * <p>Rule of thumb: <b>no business logic here</b>, and never call a repository directly. Each method is one line that
 * delegates to {@link ItemService}. Errors thrown below are turned into JSON by {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    /** Constructor injection: Spring passes in the {@code ItemService} bean. */
    public ItemController(ItemService service) {
        this.service = service;
    }

    /** {@code GET /api/items}: list all items, ordered by id. */
    @GetMapping
    public List<ItemResponse> list() {
        return service.list();
    }

    /**
     * {@code GET /api/items/{id}}: one item. Returning the object directly means 200 OK with it as JSON.
     * {@code /api/items/abc} can't be converted to a {@code Long}, so that is a 400.
     */
    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /**
     * {@code POST /api/items}: create. Returns <b>201 Created</b> (not 200) with a {@code Location} header pointing
     * at the new resource, e.g. {@code Location: /api/items/7}. {@code ResponseEntity} is used here because, unlike
     * the other methods, we need to control the status code and headers.
     */
    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest body) {
        ItemResponse created = service.create(body);
        return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
    }

    /** {@code PUT /api/items/{id}}: replace name and description. 200 with the updated item. */
    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest body) {
        return service.update(id, body);
    }

    /**
     * {@code DELETE /api/items/{id}}: delete. {@code @ResponseStatus(NO_CONTENT)} makes a successful call return
     * <b>204 No Content</b> with an empty body, the usual answer for a delete.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
