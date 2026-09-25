package app.vetra.disease.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An officer's action (ACKNOWLEDGED / ESCALATED) on a derived operational alert. */
@Entity
@Table(name = "alert_actions")
@Getter
@Setter
@NoArgsConstructor
public class AlertAction extends BaseEntity {

  @Column(name = "alert_id", nullable = false, unique = true)
  private UUID alertId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;
}
