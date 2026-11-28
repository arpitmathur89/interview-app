package com.interview.app.item;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * REPOSITORY layer: the only code that talks to the {@code items} table.
 *
 * <p>This is an <b>interface with no implementation</b>. At startup Spring Data JPA generates a class that
 * implements it and registers it as a bean, so {@code ItemService} can simply ask for an {@code ItemRepository}
 * in its constructor.
 *
 * <p>{@code JpaRepository<Item, Long>} means "repository for {@code Item} entities whose id is a {@code Long}".
 * It provides ready-made methods, for example:
 * <ul>
 *   <li>{@code save(item)} / {@code saveAndFlush(item)}: INSERT a new row or UPDATE an existing one</li>
 *   <li>{@code findById(id)}: SELECT one row, returned as an {@code Optional} (empty if not found)</li>
 *   <li>{@code findAll()}: SELECT every row</li>
 *   <li>{@code delete(item)}, {@code count()}, {@code existsById(id)}, {@code deleteAllInBatch()}, ...</li>
 * </ul>
 *
 * <p>All generated queries use bound parameters ({@code WHERE id = ?}), never string concatenation, so user
 * input can't change the SQL. That is what protects against SQL injection.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {
}
