package com.becommerce.crm.masterdata.importing.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** CSV RFC 4180 (vírgula, aspas duplas, quebras de linha dentro de aspas) e leitura do zip exportado. */
public record CsvTable(List<String> header, List<Map<String, String>> rows) {

    public static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;

    public record ZipContents(CsvTable patients, List<String> otherFiles) {}

    public static ZipContents readZip(byte[] zip) {
        Map<String, CsvTable> tables = new HashMap<>();
        List<String> others = new ArrayList<>();
        scan(zip, Set.of("patients.csv"), tables, others);
        CsvTable patients = tables.get("patients.csv");
        if (patients == null) throw new ImportFileException("O zip não contém o arquivo patients.csv.");
        return new ZipContents(patients, others);
    }

    /** Lê só os CSVs pedidos (nome base, minúsculo); falta algum → ImportFileException. */
    public static Map<String, CsvTable> readTables(byte[] zip, String... names) {
        Map<String, CsvTable> tables = new HashMap<>();
        scan(zip, Set.of(names), tables, new ArrayList<>());
        for (String n : names) {
            if (!tables.containsKey(n)) throw new ImportFileException("O zip não contém o arquivo " + n + ".");
        }
        return tables;
    }

    private static void scan(byte[] zip, Set<String> wanted, Map<String, CsvTable> tables, List<String> others) {
        boolean anyEntry = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                anyEntry = true;
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\', '/');
                String base = name.substring(name.lastIndexOf('/') + 1);
                String lower = base.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".csv")) continue;
                if (wanted.contains(lower)) {
                    tables.put(lower, parse(new String(readLimited(in, base), StandardCharsets.UTF_8)));
                } else {
                    others.add(base);
                }
            }
        } catch (IOException ex) {
            throw new ImportFileException("Arquivo .zip inválido ou corrompido.");
        }
        if (!anyEntry) throw new ImportFileException("Arquivo .zip inválido ou vazio.");
        Collections.sort(others);
    }

    private static byte[] readLimited(ZipInputStream in, String name) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_ENTRY_BYTES) throw new ImportFileException(name + " excede 10 MB.");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    public static CsvTable parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                current.add(field.toString()); field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                current.add(field.toString()); field.setLength(0);
                records.add(current); current = new ArrayList<>();
            } else field.append(c);
        }
        if (field.length() > 0 || !current.isEmpty()) { current.add(field.toString()); records.add(current); }
        records.removeIf(r -> r.size() == 1 && r.get(0).isBlank());
        if (records.isEmpty()) return new CsvTable(List.of(), List.of());
        List<String> header = records.get(0).stream().map(String::trim).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (List<String> r : records.subList(1, records.size())) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) row.put(header.get(i), i < r.size() ? r.get(i) : "");
            rows.add(row);
        }
        return new CsvTable(header, rows);
    }
}
