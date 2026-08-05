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
}
