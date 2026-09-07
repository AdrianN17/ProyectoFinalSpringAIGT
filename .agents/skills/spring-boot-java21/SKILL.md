---
name: spring-boot-java21
description: >-
  Best practices for building Spring Boot 4 applications with Java 21 in this repository.
  Use when writing Spring Boot services, configuring dependency injection, JPA entities,
  REST controllers/error handling, or leveraging Java 21 features (records, sealed types,
  pattern matching, virtual threads).
---

# Spring Boot 4 on Java 21

## Overview

This project (`iga-scanner`) targets **Spring Boot 4.1.x** on **Java 21** (see [build.gradle](../../../build.gradle)). Use modern Java 21 language features and Spring Boot 4 conventions instead of older Spring/Java patterns.

## Java 21 Features to Use

| Feature | Use For |
|---|---|
| `record` | DTOs, `@ConfigurationProperties`, value objects, immutable projections |
| Sealed interfaces + pattern-matching `switch` | Modeling closed result/error hierarchies (e.g. scan outcome types) |
| Pattern matching `instanceof` | Replacing manual casts |
| Text blocks | Multi-line SQL/JSON/prompt literals |
| Virtual threads | High-throughput blocking I/O (JDBC, HTTP clients) — enable via `spring.threads.virtual.enabled: true` |

## Dependency Injection

- Use **constructor injection only**. Never use field `@Autowired`.
- With Lombok already on the classpath, use `@RequiredArgsConstructor` on classes with `private final` fields rather than writing/using `@Autowired` setters.
- Prefer a single public constructor per Spring-managed class — Spring uses it implicitly, no `@Autowired` annotation needed.

## Configuration Properties

Bind config with immutable records, not mutable POJOs:

```java
@ConfigurationProperties(prefix = "iga.scanner")
public record ScannerProperties(String storagePath, Duration timeout) {}
```

Enable with `@EnableConfigurationProperties(ScannerProperties.class)` or a `@ConfigurationPropertiesScan`. Keep `application.yaml` (already in use) over `.properties`.

## Domain & Persistence (JPA)

- **Avoid Lombok `@Data`/`@Value` on `@Entity` classes** — generated `equals`/`hashCode`/`toString` over lazy associations causes `LazyInitializationException` and broken collection semantics. Use `@Getter`/`@Setter` individually, and hand-write `equals`/`hashCode` based on a stable business key or ID only.
- Keep JPA entities out of the API surface; map to/from `record` DTOs at the controller boundary.
- Use constructor binding / builder pattern for entities with required fields instead of no-arg `@Setter`-driven construction where possible.

## REST Layer

- `@RestController` with constructor-injected services; request/response bodies as `record` DTOs.
- Validate input with `jakarta.validation` annotations (`@Valid` on the parameter, constraints on the record components).
- Use `ProblemDetail` (RFC 7807, built into Spring Boot) for error responses via `@ExceptionHandler` in a `@RestControllerAdvice`, instead of ad-hoc error DTOs.

```java
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
```

## Concurrency

Set `spring.threads.virtual.enabled: true` to run the embedded server and `@Async` executors on virtual threads — beneficial for blocking JDBC/JPA and outbound HTTP calls under load. Don't combine with reactive (WebFlux) code paths in the same request.

## Testing

- Unit test domain/service logic with plain JUnit 5 + Mockito — no Spring context.
- Slice tests: `@DataJpaTest` for repositories, `@WebMvcTest` for controllers.
- Full integration tests: `@SpringBootTest` with Testcontainers for SQL Server (and Qdrant, if exercising the vector store).
- Prefer AssertJ (`assertThat`) over JUnit's basic assertions.

## Best Practices

1. Constructor injection everywhere; no field/setter injection.
2. Records for DTOs and configuration properties; reserve mutable classes for JPA entities.
3. Never put Lombok `@Data`/`@EqualsAndHashCode` on `@Entity` classes.
4. Centralize error handling with `ProblemDetail` + `@RestControllerAdvice`.
5. Keep entities internal; expose only DTOs across module/API boundaries.
6. Use `@Transactional` at the service layer, not on controllers or repositories.
7. Favor `Optional<T>` return types on repository lookups over returning `null`.
8. Enable virtual threads for blocking I/O-heavy workloads; benchmark before assuming a win.
