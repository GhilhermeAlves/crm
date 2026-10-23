package com.becommerce.crm.clinical.procedure.domain;

/** Dentição do odontograma, com os dentes válidos na numeração FDI. */
public enum Dentition {
    PERMANENT(8, 1),
    DECIDUOUS(5, 5);

    private final int teethPerQuadrant;
    private final int firstQuadrant;

    Dentition(int teethPerQuadrant, int firstQuadrant) {
        this.teethPerQuadrant = teethPerQuadrant;
        this.firstQuadrant = firstQuadrant;
    }

    public boolean isValidTooth(int tooth) {
        int quadrant = tooth / 10;
        int position = tooth % 10;
        return quadrant >= firstQuadrant && quadrant < firstQuadrant + 4
                && position >= 1 && position <= teethPerQuadrant;
    }
}
