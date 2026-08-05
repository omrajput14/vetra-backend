package app.vetra.appointment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new veterinary appointment is booked.
 *
 * @param appointmentId appointment UUID
 * @param farmerId farmer user UUID
 * @param vetId veterinarian user UUID
 * @param appointmentTime scheduled appointment timestamp
 * @param bookedAt timestamp of booking creation
 */
public record AppointmentBookedEvent(
    UUID appointmentId, UUID farmerId, UUID vetId, Instant appointmentTime, Instant bookedAt) {}
