package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Linha de {@code patient_procedure_teeth}, coleção de {@link PatientProcedureJpaEntity}. */
@Embeddable
public class PatientProcedureToothJpaEntity {

    @Column(name = "company_id", nullable = false)
    private java.util.UUID companyId;

    @Column(name = "tooth", nullable = false)
    private short tooth;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "faces", columnDefinition = "varchar(1)[]", nullable = false)
    private String[] faces;

    public java.util.UUID getCompanyId() { return companyId; }
    public void setCompanyId(java.util.UUID companyId) { this.companyId = companyId; }
    public short getTooth() { return tooth; }
    public void setTooth(short tooth) { this.tooth = tooth; }
    public String[] getFaces() { return faces; }
    public void setFaces(String[] faces) { this.faces = faces; }
}
