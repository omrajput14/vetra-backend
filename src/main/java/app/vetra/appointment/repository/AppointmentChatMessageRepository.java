package app.vetra.appointment.repository;

import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.AppointmentChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository interface for managing direct appointment consultation chat messages. */
@Repository
public interface AppointmentChatMessageRepository extends JpaRepository<AppointmentChatMessage, UUID> {

  /** Finds all chat messages for a specific appointment ordered chronologically. */
  List<AppointmentChatMessage> findByAppointmentOrderByCreatedAtAsc(Appointment appointment);
}
