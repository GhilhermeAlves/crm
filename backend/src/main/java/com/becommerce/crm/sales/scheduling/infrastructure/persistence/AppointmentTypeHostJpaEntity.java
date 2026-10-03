package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment_type_hosts")
@IdClass(AppointmentTypeHostJpaEntity.PK.class)
public class AppointmentTypeHostJpaEntity {

    @Id
    @Column(name = "appointment_type_id")
    private UUID appointmentTypeId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "company_id")
    private UUID companyId;

    public UUID getAppointmentTypeId() { return appointmentTypeId; }
    public void setAppointmentTypeId(UUID appointmentTypeId) { this.appointmentTypeId = appointmentTypeId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public static class PK implements Serializable {
        private UUID appointmentTypeId;
        private UUID userId;

        public PK() {}

        public PK(UUID appointmentTypeId, UUID userId) {
            this.appointmentTypeId = appointmentTypeId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(appointmentTypeId, pk.appointmentTypeId) && Objects.equals(userId, pk.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(appointmentTypeId, userId);
        }
    }
}
