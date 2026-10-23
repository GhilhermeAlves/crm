package com.becommerce.crm.clinical.procedure.domain;

import java.util.Set;

/** Dente (FDI) atingido pelo procedimento e as faces envolvidas (pode ser vazio). */
public record ProcedureTooth(int tooth, Set<ToothFace> faces) {

    public ProcedureTooth {
        faces = faces == null ? Set.of() : Set.copyOf(faces);
    }
}
