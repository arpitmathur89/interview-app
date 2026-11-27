package com.interview.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point: the class that starts the whole application.
 *
 * <p>{@code @SpringBootApplication} turns on three things at once:
 * <ul>
 *   <li><b>Component scanning</b>: Spring looks through this package and every sub-package
 *       ({@code controller}, {@code service}, {@code repository}, ...) for classes marked
 *       {@code @RestController}, {@code @Service}, {@code @Component}, {@code @Entity} etc.</li>
 *   <li><b>Auto-configuration</b>: because H2, Spring Web and Spring Data JPA are on the classpath
 *       (see {@code pom.xml}), Spring Boot sets up a database connection, Hibernate and an embedded
 *       Tomcat web server on port 8080 without any extra code.</li>
 *   <li><b>Configuration</b>: settings are read from {@code src/main/resources/application.properties}.</li>
 * </ul>
 *
 * <p>Spring then creates one instance ("bean") of each class it found and passes them into each
 * other's constructors. This is <b>dependency injection</b>: for example, {@code ItemController}
 * receives an {@code ItemService} without ever calling {@code new ItemService()}.
 *
 * <p>This package is the root, so every class must live in it or below it to be found by the scan.
 */
@SpringBootApplication
public class InterviewApplication {

    /**
     * Standard Java entry point. {@code mvn spring-boot:run} (or {@code make run}) ends up calling this.
     * {@code SpringApplication.run} builds all the beans, creates/updates the DB tables and starts
     * listening for HTTP requests; it keeps running until you press Ctrl+C.
     */
    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class, args);
    }
}
