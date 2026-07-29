# Testing Strategy
**Document ID:** ENG-14  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`, `omrajput14/vetra`  
**References:** [Engineering Principles](./00-principles.md), [Coding Standards — Backend](./12-coding-standards.md)

---

## Philosophy

Testing is not a phase that happens after development — it is a continuous activity that happens alongside development. The goal is not 100% code coverage (a meaningless metric). The goal is **confidence**: confidence that the system behaves correctly under normal conditions and fails gracefully under abnormal ones.

### Testing Pyramid

```
                   ╱───────────╲
                  ╱   E2E Tests  ╲         Few — expensive to write and maintain
                 ╱─────────────────╲
                ╱ Integration Tests  ╲     Moderate — verify layer boundaries
               ╱─────────────────────╲
              ╱      Unit Tests        ╲   Many — fast, isolated, high confidence
             ╱─────────────────────────╲
```

---

## Backend Testing Strategy

### Unit Tests

**What to test:** All service-layer business logic in isolation. Every public service method must have at least one unit test covering the happy path and at least one covering the primary failure path.

**Framework:** JUnit 5 + Mockito

**Test location:** `src/test/java/app/vetra/<module>/service/`

**Example:**

```java
@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private VetProfileRepository vetProfileRepository;

    @InjectMocks private MedicalRecordService medicalRecordService;

    @Test
    void shouldCreateMedicalRecordSuccessfully() {
        // Arrange
        UUID vetUserId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        VetProfile vet = VetProfile.builder().id(UUID.randomUUID()).userId(vetUserId).build();
        Appointment appointment = Appointment.builder()
            .id(appointmentId)
            .status(AppointmentStatus.COMPLETED)
            .veterinarianId(vet.getId())
            .build();

        when(vetProfileRepository.findByUserId(vetUserId)).thenReturn(Optional.of(vet));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(medicalRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        MedicalRecordResponse response = medicalRecordService.createRecord(request, vetUserId);

        // Assert
        assertNotNull(response.id());
        verify(medicalRecordRepository).save(any(MedicalRecord.class));
    }

    @Test
    void shouldReturn409WhenMedicalRecordAlreadyExistsForAppointment() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));
        when(medicalRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        // Act + Assert
        assertThrows(DuplicateMedicalRecordException.class,
            () -> medicalRecordService.createRecord(request, vetUserId));
    }
}
```

### Integration Tests

**What to test:** Full HTTP request → Spring Security → Controller → Service → Repository → Database → Response. This verifies that the wiring is correct and that authorization rules are enforced end-to-end.

**Framework:** `@SpringBootTest` + `@Testcontainers` (PostgreSQL container)

**Test location:** `src/test/java/app/vetra/<module>/controller/`

**Database:** A real PostgreSQL instance (via Testcontainers) with Flyway migrations applied.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MedicalRecordControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("vetra_test")
        .withUsername("test")
        .withPassword("test");

    @Test
    void shouldReturn201WhenVetCreatesValidMedicalRecord() throws Exception {
        String vetToken = loginAndGetToken("vet@example.com", "password");

        mockMvc.perform(post("/api/v1/medical-records")
                .header("Authorization", "Bearer " + vetToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "appointmentId": "%s",
                      "diagnosis": "Fever",
                      "treatment": "Antibiotics prescribed"
                    }
                    """.formatted(completedAppointmentId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.diagnosis").value("Fever"));
    }

    @Test
    void shouldReturn403WhenFarmerAttemptsToCreateMedicalRecord() throws Exception {
        String farmerToken = loginAndGetToken("farmer@example.com", "password");

        mockMvc.perform(post("/api/v1/medical-records")
                .header("Authorization", "Bearer " + farmerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ ... }"))
            .andExpect(status().isForbidden());
    }
}
```

### Test Coverage Targets

| Layer | Coverage Target |
|---|---|
| Service (business logic) | ≥ 90% line coverage |
| Controller (HTTP layer) | Happy path + auth failure for every endpoint |
| Repository | Not directly tested — covered by integration tests |
| Domain entities | N/A — simple data containers |

---

## Flutter Testing Strategy

### Unit Tests

**What to test:** Riverpod providers and notifiers; data model serialization; utility functions.

**Framework:** `flutter_test` + `mockito`

```dart
void main() {
  group('MedicalRecordNotifier', () {
    late MockMedicalRecordRepository mockRepository;
    late ProviderContainer container;

    setUp(() {
      mockRepository = MockMedicalRecordRepository();
      container = ProviderContainer(
        overrides: [
          medicalRecordRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() => container.dispose());

    test('should emit loaded state on successful fetch', () async {
      when(mockRepository.getMedicalRecord(any))
          .thenAnswer((_) async => fakeMedicalRecord);

      final notifier = container.read(medicalRecordProvider.notifier);
      await notifier.load(fakeId);

      expect(
        container.read(medicalRecordProvider),
        const MedicalRecordState.loaded(fakeMedicalRecord),
      );
    });
  });
}
```

### Widget Tests

**What to test:** Individual widgets render correctly given their input. Interaction callbacks are triggered correctly.

```dart
testWidgets('MedicalRecordCard shows diagnosis and date', (tester) async {
  await tester.pumpWidget(
    MaterialApp(
      home: MedicalRecordCard(record: fakeMedicalRecord),
    ),
  );

  expect(find.text('Fever'), findsOneWidget);
  expect(find.text('2026-07-28'), findsOneWidget);
});
```

### Golden Tests (Planned)

Widget visual regression tests using `flutter_test` golden files. Planned for the design system components in Stage 8.

### Integration Tests (Planned)

End-to-end tests using `integration_test` package against a running backend on a test environment.

---

## Test Naming Convention

| Language | Pattern | Example |
|---|---|---|
| Java | `should<Expected>When<Condition>()` | `shouldReturn409WhenDuplicateMedicalRecordCreated()` |
| Dart (test) | `'<expected> when <condition>'` | `'should emit loaded state on successful fetch'` |
| Dart (testWidgets) | `'<widget> renders <what>'` | `'MedicalRecordCard shows diagnosis and date'` |

---

## Running Tests

### Backend

```bash
# Unit tests only
./mvnw test

# Unit + integration tests (requires Docker for Testcontainers)
./mvnw verify

# With coverage report
./mvnw verify jacoco:report
# Report at: target/site/jacoco/index.html
```

### Flutter

```bash
# All tests
flutter test

# With coverage
flutter test --coverage

# Single test file
flutter test test/features/medical_record/presentation/providers/medical_record_provider_test.dart

# Widget tests with golden update
flutter test --update-goldens
```
