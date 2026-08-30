package app.vetra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Direct consultation chat messages and structured treatment instructions for appointments. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointment_chat_messages")
public class AppointmentChatMessage extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appointment_id", nullable = false)
  private Appointment appointment;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_id", nullable = false)
  private User recipient;

  @Column(name = "message_type", nullable = false, length = 30)
  private String messageType = "TEXT";

  @NotNull
  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "treatment_payload_json", columnDefinition = "TEXT")
  private String treatmentPayloadJson;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  public Appointment getAppointment() {
    return appointment;
  }

  public void setAppointment(Appointment appointment) {
    this.appointment = appointment;
  }

  public User getSender() {
    return sender;
  }

  public void setSender(User sender) {
    this.sender = sender;
  }

  public User getRecipient() {
    return recipient;
  }

  public void setRecipient(User recipient) {
    this.recipient = recipient;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getTreatmentPayloadJson() {
    return treatmentPayloadJson;
  }

  public void setTreatmentPayloadJson(String treatmentPayloadJson) {
    this.treatmentPayloadJson = treatmentPayloadJson;
  }

  public boolean isRead() {
    return isRead;
  }

  public void setRead(boolean read) {
    isRead = read;
  }

  public static AppointmentChatMessageBuilder builder() {
    return new AppointmentChatMessageBuilder();
  }

  public static class AppointmentChatMessageBuilder {
    private Appointment appointment;
    private User sender;
    private User recipient;
    private String messageType = "TEXT";
    private String content;
    private String treatmentPayloadJson;
    private boolean isRead = false;

    public AppointmentChatMessageBuilder appointment(Appointment appointment) {
      this.appointment = appointment;
      return this;
    }

    public AppointmentChatMessageBuilder sender(User sender) {
      this.sender = sender;
      return this;
    }

    public AppointmentChatMessageBuilder recipient(User recipient) {
      this.recipient = recipient;
      return this;
    }

    public AppointmentChatMessageBuilder messageType(String messageType) {
      this.messageType = messageType;
      return this;
    }

    public AppointmentChatMessageBuilder content(String content) {
      this.content = content;
      return this;
    }

    public AppointmentChatMessageBuilder treatmentPayloadJson(String treatmentPayloadJson) {
      this.treatmentPayloadJson = treatmentPayloadJson;
      return this;
    }

    public AppointmentChatMessageBuilder isRead(boolean isRead) {
      this.isRead = isRead;
      return this;
    }

    public AppointmentChatMessage build() {
      AppointmentChatMessage msg = new AppointmentChatMessage();
      msg.setAppointment(this.appointment);
      msg.setSender(this.sender);
      msg.setRecipient(this.recipient);
      msg.setMessageType(this.messageType != null ? this.messageType : "TEXT");
      msg.setContent(this.content);
      msg.setTreatmentPayloadJson(this.treatmentPayloadJson);
      msg.setRead(this.isRead);
      return msg;
    }
  }
}
