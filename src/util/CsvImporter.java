package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import model.*;

/**
 * Imports Subjects, Teachers, Rooms, and StudentGroups from CSV files.
 *
 * Expected formats (first row is always a header and is skipped):
 *
 *  subjects.csv   →  Name, WeeklyHours, Type          (Type: Lec / Tut / Lab)
 *  teachers.csv   →  Name, Subjects                   (Subjects: semicolon-separated names)
 *  rooms.csv      →  ID, Capacity, Type               (Type: Classroom / Lecture Hall / Lab)
 *  groups.csv     →  Name, Size, Subjects             (Subjects: semicolon-separated names)
 *
 * Any CSV produced by Excel (comma-separated, UTF-8 or system encoding) is accepted.
 * Lines that are blank or start with '#' are skipped.
 */
public class CsvImporter {

    public static class ImportResult {
        public final int imported;
        public final List<String> errors;

        ImportResult(int imported, List<String> errors) {
            this.imported = imported;
            this.errors = errors;
        }
    }

    // -----------------------------------------------------------------------
    // Public file-based entry points
    // -----------------------------------------------------------------------
    public static ImportResult importSubjects(File file) {
        List<String> errors = new ArrayList<>();
        return importSubjects(file, readCsv(file, errors));
    }

    public static ImportResult importTeachers(File file) {
        List<String> errors = new ArrayList<>();
        return importTeachers(file, readCsv(file, errors));
    }

    public static ImportResult importRooms(File file) {
        List<String> errors = new ArrayList<>();
        return importRooms(file, readCsv(file, errors));
    }

    public static ImportResult importGroups(File file) {
        List<String> errors = new ArrayList<>();
        return importGroups(file, readCsv(file, errors));
    }

    public static ImportResult importStudents(File file) {
        List<String> errors = new ArrayList<>();
        return importStudents(file, readCsv(file, errors));
    }

    // -----------------------------------------------------------------------
    // Text-based (paste) import
    // -----------------------------------------------------------------------

    public static ImportResult importSubjectsFromText(String csvText) {
        return importSubjects(null, readCsvFromText(csvText, new ArrayList<>()));
    }

    public static ImportResult importTeachersFromText(String csvText) {
        return importTeachers(null, readCsvFromText(csvText, new ArrayList<>()));
    }

    public static ImportResult importRoomsFromText(String csvText) {
        return importRooms(null, readCsvFromText(csvText, new ArrayList<>()));
    }

    public static ImportResult importGroupsFromText(String csvText) {
        return importGroups(null, readCsvFromText(csvText, new ArrayList<>()));
    }

    public static ImportResult importStudentsFromText(String csvText) {
        return importStudents(null, readCsvFromText(csvText, new ArrayList<>()));
    }

    // Delegate variant that accepts pre-parsed rows (null file = text mode)
    private static ImportResult importSubjects(@SuppressWarnings("unused") File file, List<String[]> rows) {
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (String[] row : rows) {
            if (row.length < 3) { errors.add("Skipped row (need 3 columns): " + String.join(",", row)); continue; }
            String name = row[0].trim();
            String type = row[2].trim();
            if (name.isEmpty()) { errors.add("Empty subject name, row skipped."); continue; }
            if (type.isBlank()) {
                errors.add("Empty type for subject '" + name + "', skipped.");
                continue;
            }
            int hours;
            try { hours = Integer.parseInt(row[1].trim()); }
            catch (NumberFormatException e) {
                errors.add("Invalid hours '" + row[1].trim() + "' for subject '" + name + "', skipped."); continue;
            }
            String preferredDay = (row.length >= 4 && !row[3].trim().isEmpty()) ? row[3].trim() : null;
            int sessionDur = 1;
            if (row.length >= 5 && !row[4].trim().isEmpty()) {
                try { sessionDur = Math.max(1, Integer.parseInt(row[4].trim())); }
                catch (NumberFormatException ignored) {}
            }
            boolean mandatory = true;
            if (row.length >= 6 && !row[5].trim().isEmpty()) {
                mandatory = !row[5].trim().equalsIgnoreCase("false");
            }
            DataStore.addSubject(new Subject(name, hours, type, preferredDay, false, sessionDur, mandatory));
            count++;
        }
        return new ImportResult(count, errors);
    }

    private static ImportResult importTeachers(@SuppressWarnings("unused") File file, List<String[]> rows) {
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (String[] row : rows) {
            if (row.length < 1) continue;
            String name = row[0].trim();
            if (name.isEmpty()) { errors.add("Empty teacher name, row skipped."); continue; }
            Teacher teacher = new Teacher(name);
            if (row.length >= 2 && !row[1].trim().isEmpty()) {
                for (String sName : row[1].split(";")) {
                    Subject found = findSubjectByName(sName.trim());
                    if (found != null) teacher.addSubject(found);
                    else errors.add("Subject '" + sName.trim() + "' not found for teacher '" + name + "'.");
                }
            }
            DataStore.addTeacher(teacher);
            count++;
        }
        return new ImportResult(count, errors);
    }

    private static ImportResult importRooms(@SuppressWarnings("unused") File file, List<String[]> rows) {
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (String[] row : rows) {
            if (row.length < 3) { errors.add("Skipped row (need 3 columns): " + String.join(",", row)); continue; }
            String id   = row[0].trim();
            String type = row[2].trim();
            if (id.isEmpty()) { errors.add("Empty room ID, row skipped."); continue; }
            int capacity;
            try { capacity = Integer.parseInt(row[1].trim()); }
            catch (NumberFormatException e) {
                errors.add("Invalid capacity '" + row[1].trim() + "' for room '" + id + "', skipped."); continue;
            }
            DataStore.addRoom(new Room(id, capacity, type));
            count++;
        }
        return new ImportResult(count, errors);
    }

    private static ImportResult importGroups(@SuppressWarnings("unused") File file, List<String[]> rows) {
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (String[] row : rows) {
            if (row.length < 2) { errors.add("Skipped row (need at least 2 columns): " + String.join(",", row)); continue; }
            String name = row[0].trim();
            if (name.isEmpty()) { errors.add("Empty group name, row skipped."); continue; }
            int size;
            try { size = Integer.parseInt(row[1].trim()); }
            catch (NumberFormatException e) {
                errors.add("Invalid size '" + row[1].trim() + "' for group '" + name + "', skipped."); continue;
            }
            StudentGroup group = new StudentGroup(name, size);
            if (row.length >= 3 && !row[2].trim().isEmpty()) {
                for (String sName : row[2].split(";")) {
                    java.util.List<Subject> found = findAllSubjectsByName(sName.trim());
                    if (!found.isEmpty()) found.forEach(group::addSubject);
                    else errors.add("Subject '" + sName.trim() + "' not found for group '" + name + "'.");
                }
            }
            for (Subject ms : DataStore.getSubjects()) {
                if (ms.isMandatory() && !group.getSubjects().contains(ms)) {
                    group.addSubject(ms);
                }
            }
            DataStore.addGroup(group);
            count++;
        }
        return new ImportResult(count, errors);
    }

    private static ImportResult importStudents(@SuppressWarnings("unused") File file, List<String[]> rows) {
        List<String> errors = new ArrayList<>();
        int count = 0;
        for (String[] row : rows) {
            if (row.length < 1) { errors.add("Skipped empty row."); continue; }
            String name = row[0].trim();
            if (name.isEmpty()) { errors.add("Empty student name, row skipped."); continue; }
            StudentGroup group = null;
            if (row.length >= 2 && !row[1].trim().isEmpty()) {
                String groupName = row[1].trim();
                group = DataStore.getGroups().stream()
                        .filter(g -> g.getName().equalsIgnoreCase(groupName))
                        .findFirst().orElse(null);
                if (group == null)
                    errors.add("Group '" + groupName + "' not found for student '" + name + "' — imported without group.");
            }

            Student existing = DataStore.getStudents().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (existing != null) {
                String existingGroup = existing.getGroup() == null ? null : existing.getGroup().getName();
                String incomingGroup = group == null ? null : group.getName();
                if (existingGroup != null && incomingGroup != null && !existingGroup.equalsIgnoreCase(incomingGroup)) {
                    errors.add("Student '" + name + "' already belongs to group '" + existingGroup
                            + "' — cannot assign to '" + incomingGroup + "'. Row skipped.");
                    continue;
                }
                if (existing.getGroup() == null && group != null) existing.setGroup(group);

                if (row.length >= 3 && !row[2].trim().isEmpty()) {
                    for (String sName : row[2].split(";")) {
                        java.util.List<Subject> found = findAllSubjectsByName(sName.trim());
                        if (!found.isEmpty()) found.forEach(existing::addElective);
                        else errors.add("Elective subject '" + sName.trim() + "' not found for student '" + name + "'.");
                    }
                }
                continue;
            }

            Student student = new Student(name, group);
            if (row.length >= 3 && !row[2].trim().isEmpty()) {
                for (String sName : row[2].split(";")) {
                    java.util.List<Subject> found = findAllSubjectsByName(sName.trim());
                    if (!found.isEmpty()) found.forEach(student::addElective);
                    else errors.add("Elective subject '" + sName.trim() + "' not found for student '" + name + "'.");
                }
            }
            DataStore.addStudent(student);
            count++;
        }
        return new ImportResult(count, errors);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Reads a CSV file and returns each data row as a String array.
     * The header row (first non-blank/non-comment line) is skipped.
     */
    private static List<String[]> readCsv(File file, List<String> errors) {
        List<String[]> rows = new ArrayList<>();
        boolean headerSkipped = false;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Strip UTF-8 BOM if present (Excel adds it)
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!headerSkipped) { headerSkipped = true; continue; }
                rows.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            errors.add("Could not read file: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Splits a single CSV line handling quoted fields.
     */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(String[]::new);
    }

    /**
     * Parses CSV rows from a raw string (header row skipped).
     */
    private static List<String[]> readCsvFromText(String text, @SuppressWarnings("unused") List<String> errors) {
        List<String[]> rows = new ArrayList<>();
        boolean headerSkipped = false;
        for (String line : text.split("\r?\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (!headerSkipped) { headerSkipped = true; continue; }
            rows.add(splitCsvLine(line));
        }
        return rows;
    }

    private static Subject findSubjectByName(String name) {
        for (Subject s : DataStore.getSubjects()) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    private static java.util.List<Subject> findAllSubjectsByName(String name) {
        java.util.List<Subject> result = new java.util.ArrayList<>();
        for (Subject s : DataStore.getSubjects()) {
            if (s.getName().equalsIgnoreCase(name)) result.add(s);
        }
        return result;
    }
}
