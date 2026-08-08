package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Profile information for livestock farmers. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "farmer_profiles")
public class FarmerProfile extends BaseEntity {

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @NotNull
  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "farm_name")
  private String farmName;

  @Column(name = "village")
  private String village;

  @Column(name = "district")
  private String district;

  @Column(name = "state")
  private String state;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "animal_count")
  private Integer animalCount;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFarmName() {
    return farmName;
  }

  public void setFarmName(String farmName) {
    this.farmName = farmName;
  }

  public String getVillage() {
    return village;
  }

  public void setVillage(String village) {
    this.village = village;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String district) {
    this.district = district;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Integer getAnimalCount() {
    return animalCount;
  }

  public void setAnimalCount(Integer animalCount) {
    this.animalCount = animalCount;
  }

  public static FarmerProfileBuilder builder() {
    return new FarmerProfileBuilder();
  }

  public static class FarmerProfileBuilder {
    private User user;
    private String fullName;
    private String farmName;
    private String village;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private Integer animalCount;

    public FarmerProfileBuilder user(User user) {
      this.user = user;
      return this;
    }

    public FarmerProfileBuilder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public FarmerProfileBuilder farmName(String farmName) {
      this.farmName = farmName;
      return this;
    }

    public FarmerProfileBuilder village(String village) {
      this.village = village;
      return this;
    }

    public FarmerProfileBuilder district(String district) {
      this.district = district;
      return this;
    }

    public FarmerProfileBuilder state(String state) {
      this.state = state;
      return this;
    }

    public FarmerProfileBuilder latitude(Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public FarmerProfileBuilder longitude(Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public FarmerProfileBuilder animalCount(Integer animalCount) {
      this.animalCount = animalCount;
      return this;
    }

    public FarmerProfile build() {
      FarmerProfile profile = new FarmerProfile();
      profile.setUser(this.user);
      profile.setFullName(this.fullName);
      profile.setFarmName(this.farmName);
      profile.setVillage(this.village);
      profile.setDistrict(this.district);
      profile.setState(this.state);
      profile.setLatitude(this.latitude);
      profile.setLongitude(this.longitude);
      profile.setAnimalCount(this.animalCount);
      return profile;
    }
  }
}
