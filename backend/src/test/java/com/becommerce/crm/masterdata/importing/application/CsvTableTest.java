package com.becommerce.crm.masterdata.importing.application;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class CsvTableTest {

    @Test
    void parsesQuotedFieldsCommasAndNewlines() {
        CsvTable t = CsvTable.parse("id,name,obs\n1,\"Silva, Ana\",\"linha1\nlinha2\"\n2,Beto,\"diz \"\"oi\"\"\"\n");
        assertEquals(2, t.rows().size());
        assertEquals("Silva, Ana", t.rows().get(0).get("name"));
        assertEquals("linha1\nlinha2", t.rows().get(0).get("obs"));
        assertEquals("diz \"oi\"", t.rows().get(1).get("obs"));
    }

    @Test
    void stripsBomAndCrLf() {
        CsvTable t = CsvTable.parse("﻿id,name\r\n1,Ana\r\n");
        assertEquals("Ana", t.rows().get(0).get("name"));
    }

    @Test
    void readsPatientsFromNestedFolderAndListsOthers() throws Exception {
        byte[] zip = zip("base/patients.csv", "id,name\n1,Ana\n", "base/budgets.csv", "id\n");
        CsvTable.ZipContents c = CsvTable.readZip(zip);
        assertEquals("Ana", c.patients().rows().get(0).get("name"));
        assertEquals(java.util.List.of("budgets.csv"), c.otherFiles());
    }

    @Test
    void failsWithoutPatientsCsv() throws Exception {
        byte[] zip = zip("base/budgets.csv", "id\n");
        ImportFileException e = assertThrows(ImportFileException.class, () -> CsvTable.readZip(zip));
        assertTrue(e.getMessage().contains("patients.csv"));
    }

    @Test
    void failsOnInvalidZip() {
        assertThrows(ImportFileException.class, () -> CsvTable.readZip("nada".getBytes()));
    }

    private static byte[] zip(String... nameAndContent) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i < nameAndContent.length; i += 2) {
                z.putNextEntry(new ZipEntry(nameAndContent[i]));
                z.write(nameAndContent[i + 1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
