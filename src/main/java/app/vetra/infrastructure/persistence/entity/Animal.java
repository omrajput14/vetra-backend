package app.vetra.infrastructure.persistence.entity;

import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Livestock animal entity. */
@Entity
@Table(name = "animals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_id", nullable = false)
  private FarmerProfile farmer;

  @Column(name = "animal_name", length = 100)
  private String animalName;

  @Column(name = "tag_number", nullable = false, unique = true, length = 50)
  private String tagNumber;

  @Column(name = "qr_code_id", unique = true, length = 100)
  private String qrCodeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "species", nullable = false, length = 50)
  private Species species;

  @Column(name = "breed", length = 100)
  private String breed;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", nullable = false, length = 20)
  private AnimalGender gender;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "photo_url")
  private String photoUrl;

  public FarmerProfile getFarmer() {
    return farmer;
  }

  public void setFarmer(FarmerProfile farmer) {
    this.farmer = farmer;
  }

  public String getAnimalName() {
    return animalName;
  }

  public void setAnimalName(String animalName) {
    this.animalName = animalName;
  }

  public String getTagNumber() {
    return tagNumber;
  }

  public void setTagNumber(String tagNumber) {
    this.tagNumber = tagNumber;
  }

  public String getQrCodeId() {
    return qrCodeId;
  }

  public void setQrCodeId(String qrCodeId) {
    this.qrCodeId = qrCodeId;
  }

  public Species getSpecies() {
    return species;
  }

  public void setSpecies(Species species) {
    this.species = species;
  }

  public String getBreed() {
    return breed;
  }

  public void setBreed(String breed) {
    this.breed = breed;
  }

  public AnimalGender getGender() {
    return gender;
  }

  public void setGender(AnimalGender gender) {
    this.gender = gender;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }

  public static AnimalBuilder builder() {
    return new AnimalBuilder();
  }

  public static class AnimalBuilder {
    private FarmerProfile farmer;
    private String animalName;
    private String tagNumber;
    private String qrCodeId;
    private Species species;
    private String breed;
    private AnimalGender gender;
    private LocalDate birthDate;
    private String photoUrl;

    public AnimalBuilder farmer(FarmerProfile farmer) {
      this.farmer = farmer;
      return this;
    }

    public AnimalBuilder animalName(String animalName) {
      this.animalName = animalName;
      return this;
    }

    public AnimalBuilder tagNumber(String tagNumber) {
      this.tagNumber = tagNumber;
      return this;
    }

    public AnimalBuilder qrCodeId(String qrCodeId) {
      this.qrCodeId = qrCodeId;
      return this;
    }

    public AnimalBuilder species(Species species) {
      this.species = species;
      return this;
    }

    public AnimalBuilder breed(String breed) {
      this.breed = breed;
      return this;
    }

    public AnimalBuilder gender(AnimalGender gender) {
      this.gender = gender;
      return this;
    }

    public AnimalBuilder birthDate(LocalDate birthDate) {
      this.birthDate = birthDate;
      return this;
    }

    public AnimalBuilder photoUrl(String photoUrl) {
      this.photoUrl = photoUrl;
      return this;
    }

    public Animal build() {
      Animal animal = new Animal();
      animal.setFarmer(this.farmer);
      animal.setAnimalName(this.animalName);
      animal.setTagNumber(this.tagNumber);
      animal.setQrCodeId(this.qrCodeId);
      animal.setSpecies(this.species);
      animal.setBreed(this.breed);
      animal.setGender(this.gender);
      animal.setBirthDate(this.birthDate);
      animal.setPhotoUrl(this.photoUrl);
      return animal;
    }
  }
}
