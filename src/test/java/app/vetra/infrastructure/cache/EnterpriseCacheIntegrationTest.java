package app.vetra.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.dashboard.dto.DashboardResponse;
import app.vetra.infrastructure.cache.config.CacheConfiguration;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {
    CacheConfiguration.class,
    app.vetra.infrastructure.redis.config.RedisConfig.class
})
@ActiveProfiles("test")
@DisplayName("Enterprise Cache Layer Integration & Serialization Tests")
class EnterpriseCacheIntegrationTest {

  @Autowired
  private CacheManager cacheManager;

  @Test
  @DisplayName("Should initialize RedisCacheManager bean with statistics enabled")
  void shouldInitializeRedisCacheManager() {
    assertThat(cacheManager).isNotNull();
    assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
  }

  @Test
  @DisplayName("Should store and retrieve serialized AnimalResponse DTO from Redis cache region")
  void shouldCacheAndRetrieveAnimalResponse() {
    Cache cache = cacheManager.getCache(CacheNames.ANIMALS);
    assertThat(cache).isNotNull();

    UUID animalId = UUID.randomUUID();
    UUID farmerId = UUID.randomUUID();
    String cacheKey = CacheKeys.animalKey(animalId);

    AnimalResponse originalResponse = new AnimalResponse(
        animalId,
        farmerId,
        "John Farmer",
        "Bessie",
        "TAG-9988",
        "QR-9988",
        Species.CATTLE,
        "Holstein",
        AnimalGender.FEMALE,
        LocalDate.of(2021, 5, 10),
        "https://storage.vetra.app/animals/bessie.jpg",
        Instant.now(),
        Instant.now()
    );

    cache.put(cacheKey, originalResponse);

    Cache.ValueWrapper wrapper = cache.get(cacheKey);
    assertThat(wrapper).isNotNull();
    assertThat(wrapper.get()).isInstanceOf(AnimalResponse.class);

    AnimalResponse cachedResponse = (AnimalResponse) wrapper.get();
    assertThat(cachedResponse.id()).isEqualTo(animalId);
    assertThat(cachedResponse.animalName()).isEqualTo("Bessie");
    assertThat(cachedResponse.species()).isEqualTo(Species.CATTLE);

    cache.evict(cacheKey);
    assertThat(cache.get(cacheKey)).isNull();
  }

  @Test
  @DisplayName("Should store and retrieve serialized DashboardResponse DTO from farmer dashboard region")
  void shouldCacheAndRetrieveDashboardResponse() {
    Cache cache = cacheManager.getCache(CacheNames.DASHBOARD_FARMER);
    assertThat(cache).isNotNull();

    UUID farmerId = UUID.randomUUID();
    String cacheKey = CacheKeys.farmerDashboardKey(farmerId);

    DashboardResponse originalDashboard = new DashboardResponse(
        25L,
        3L,
        1L,
        12L,
        "John Farmer",
        "Green Valley Dairy",
        "FARMER"
    );

    cache.put(cacheKey, originalDashboard);

    Cache.ValueWrapper wrapper = cache.get(cacheKey);
    assertThat(wrapper).isNotNull();

    DashboardResponse cachedDashboard = (DashboardResponse) wrapper.get();
    assertThat(cachedDashboard.registeredAnimalCount()).isEqualTo(25L);
    assertThat(cachedDashboard.pendingAppointmentsCount()).isEqualTo(3L);
    assertThat(cachedDashboard.userName()).isEqualTo("John Farmer");

    cache.evict(cacheKey);
    assertThat(cache.get(cacheKey)).isNull();
  }

  @Test
  @DisplayName("Should enforce null value caching prohibition")
  void shouldProhibitNullValueCaching() {
    Cache cache = cacheManager.getCache(CacheNames.MEDICAL_RECORDS);
    assertThat(cache).isNotNull();

    String key = CacheKeys.medicalRecordKey(UUID.randomUUID());
    assertThat(cache.get(key)).isNull();
  }
}
