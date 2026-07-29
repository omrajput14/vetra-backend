# Coding Standards & Conventions — Java / Spring Boot
**Document ID:** ENG-12  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`  
**References:** [Engineering Principles](./00-principles.md), [Git Workflow](./13-git-workflow.md)

---

## Overview

This document defines the Java and Spring Boot coding standards for the Vetra backend. All code submitted to this repository must comply with these standards. Compliance is verified by **Checkstyle** (`checkstyle.xml`) — the build will fail if violations are detected.

---

## General Java Standards

### 1. Package Structure

All code lives under `app.vetra.*`. Feature modules are top-level packages:

```
app.vetra.auth.*
app.vetra.animal.*
app.vetra.appointment.*
app.vetra.medicalrecord.*
app.vetra.dashboard.*
app.vetra.infrastructure.*
```

New features must introduce a new top-level package following this pattern.

### 2. Class Naming

| Type | Convention | Example |
|---|---|---|
| Controller | `PascalCase + Controller` | `MedicalRecordController` |
| Service | `PascalCase + Service` | `MedicalRecordService` |
| Repository | `PascalCase + Repository` | `MedicalRecordRepository` |
| JPA Entity | `PascalCase` | `MedicalRecord`, `Appointment` |
| DTO (Request) | `PascalCase + Request` | `CreateMedicalRecordRequest` |
| DTO (Response) | `PascalCase + Response` | `MedicalRecordResponse` |
| Exception | `PascalCase + Exception` | `DuplicateMedicalRecordException` |
| Configuration | `PascalCase + Config` | `SecurityConfig`, `JpaConfig` |

### 3. Method Naming

- Methods are camelCase, verb-first, intention-revealing.
- `getAnimal(UUID id)` — not `findAnimal`, not `fetchData`
- `createMedicalRecord(...)` — not `saveMedicalRecord`, not `newMedicalRecord`
- `validateAppointmentOwnership(...)` — not `checkOwner`

### 4. Variable Naming

- Local variables and parameters: camelCase — `veterinarianId`, `appointmentDate`
- Constants: `UPPER_SNAKE_CASE` — `MAX_RETRY_COUNT = 3`
- Do not use single-letter names except in lambda expressions and for loop counters

---

## Spring Boot Standards

### 1. Dependency Injection

**Constructor injection only.** Field injection (`@Autowired` on fields) is prohibited.

```java
// ✅ Correct
@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
}

// ❌ Prohibited
@Service
public class MedicalRecordService {
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
}
```

Use Lombok's `@RequiredArgsConstructor` to generate the constructor from `final` fields.

### 2. Controller Design

- Controllers are thin — they validate input (via `@Valid`), call the service, and return the response.
- No business logic in controllers.
- No repository access in controllers.
- All methods return `ResponseEntity<T>`.

```java
@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> createRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID vetUserId = UUID.fromString(userDetails.getUsername());
        MedicalRecordResponse response = medicalRecordService.createRecord(request, vetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### 3. Service Design

- Services contain all business logic and authorization enforcement.
- Services are transactional: `@Transactional` on the class or on individual write methods.
- Services must not expose JPA entities — always return DTOs.
- Services throw domain exceptions, not Spring exceptions.

### 4. Repository Design

- Repositories extend `JpaRepository<Entity, UUID>`.
- Custom queries use JPQL (`@Query`) or Spring Data method names.
- Native SQL is used only when JPQL is insufficient (e.g., PostGIS functions).
- All native SQL uses named parameters, never string concatenation.

```java
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    List<MedicalRecord> findByAnimalIdOrderByCreatedAtDesc(UUID animalId);

    boolean existsByAppointmentId(UUID appointmentId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.animalId = :animalId AND m.farmerId = :farmerId ORDER BY m.createdAt DESC")
    List<MedicalRecord> findByAnimalIdAndFarmerId(@Param("animalId") UUID animalId,
                                                   @Param("farmerId") UUID farmerId);
}
```

### 5. DTOs — Records over Classes

Use Java **Records** for DTOs (immutable, no boilerplate):

```java
public record CreateMedicalRecordRequest(
    @NotNull UUID appointmentId,
    @NotBlank String diagnosis,
    @NotBlank String treatment,
    String symptoms,
    String prescription
) {}
```

Use regular classes only when mutability is genuinely needed (rare).

### 6. Exception Handling

Domain exceptions are mapped to HTTP responses in `GlobalExceptionHandler` only:

```java
// ✅ Correct — throw a domain exception from the service
throw new EntityNotFoundException("ANIMAL_001");

// The GlobalExceptionHandler maps it to 404 with the error code
```

Never use `ResponseEntity` with error bodies directly in services. Never use `@ResponseStatus` on exception classes — all mapping is centralized in `GlobalExceptionHandler`.

---

## JPA Entity Standards

- Use `@Entity` and `@Table(name = "snake_case_table_name")` explicitly.
- All entities have `@CreationTimestamp` and `@UpdateTimestamp` on `createdAt` / `updatedAt`.
- Entities for optimistic locking have `@Version` on the `version` field.
- No bidirectional relationships — use unidirectional associations only (FK as `UUID` field, not JPA join).
- Never call `entity.toString()` in logs — it may trigger lazy loading.

```java
// ✅ Correct — FK as UUID, not JPA join
@Column(name = "farmer_id", nullable = false)
private UUID farmerId;

// ❌ Avoid — bidirectional JPA join
@ManyToOne
@JoinColumn(name = "farmer_id")
private FarmerProfile farmer;
```

---

## Testing Standards

Full strategy in [`docs/guides/14-testing-strategy.md`](../guides/14-testing-strategy.md).

- Test class: `MedicalRecordServiceTest` (not `TestMedicalRecord`)
- Test method: `shouldReturn409WhenDuplicateMedicalRecordCreated()` (snake_case inside method ok for readability)
- One assertion concept per test method
- No `Thread.sleep()` in tests — use `@MockBean` and `CompletableFuture` if async
- Mocks via Mockito: `@ExtendWith(MockitoExtension.class)` for unit tests
- Integration tests via `@SpringBootTest` with `@Testcontainers` (PostgreSQL)

---

## Code Review Checklist — Backend

Before requesting review, verify:

- [ ] Constructor injection used (no `@Autowired` on fields)
- [ ] Controller delegates to service — no business logic in controller
- [ ] Service returns DTOs — no JPA entities in responses
- [ ] Authorization ownership check present in service
- [ ] `@Valid` on all `@RequestBody` parameters
- [ ] Unit test for new service method
- [ ] `./mvnw checkstyle:check` passes
- [ ] `./mvnw test` passes
- [ ] No `System.out.println()` or commented-out code
- [ ] New endpoint documented in API specification
- [ ] New error condition in error catalogue
