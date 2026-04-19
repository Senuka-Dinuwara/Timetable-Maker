package util;

import java.util.ArrayList;
import java.util.List;
import model.*;

public class DataStore {
    private static final List<String> DEFAULT_SUBJECT_TYPES = java.util.List.of("Lecture", "Tutorial", "Lab", "Theory", "Practical");
    private static final List<String> DEFAULT_ROOM_TYPES = java.util.List.of("Classroom", "Lecture Hall", "Lab");
    private static final List<String> DEFAULT_MULTI_GROUP_SUBJECT_TYPES = java.util.List.of();

    private static final List<Subject> subjects = new ArrayList<>();
    private static final List<Teacher> teachers = new ArrayList<>();
    private static final List<Room> rooms = new ArrayList<>();
    private static final List<StudentGroup> groups = new ArrayList<>();
    private static final List<Student> students = new ArrayList<>();
    private static final List<TimeSlot> timeSlots = new ArrayList<>();
    private static final List<ScheduleEntry> schedule = new ArrayList<>();
    /** Rotational weeks: index 0 = Week 1, index 1 = Week 2, etc. */
    private static final List<List<ScheduleEntry>> weekSchedules = new ArrayList<>();
    /** Break slot start times (total minutes from midnight) most recently used during generation. */
    private static java.util.Set<Integer> lastBreakMinutes = java.util.Collections.emptySet();
    /** Configurable subject types shown in UI. */
    private static final List<String> subjectTypeOptions = new ArrayList<>(DEFAULT_SUBJECT_TYPES);
    /** Configurable room types shown in UI. */
    private static final List<String> roomTypeOptions = new ArrayList<>(DEFAULT_ROOM_TYPES);
    /** Room types that can be assigned to groups (exclusive mode). */
    private static final java.util.Set<String> roomTypesAssignableToGroups = new java.util.LinkedHashSet<>();
    /** Room types that can be assigned to subjects (exclusive mode). */
    private static final java.util.Set<String> roomTypesAssignableToSubjects = new java.util.LinkedHashSet<>();
    /** Subject types that should be generated as shared multi-group sessions. */
    private static final java.util.Set<String> multiGroupSubjectTypes = new java.util.LinkedHashSet<>(DEFAULT_MULTI_GROUP_SUBJECT_TYPES);

    // ---- Subjects ----
    public static List<Subject> getSubjects() { return subjects; }
    public static void addSubject(Subject s) {
        subjects.add(s);
        String t = s.getType();
        if (t != null) {
            for (String part : t.split(";")) addSubjectTypeOption(part);
        }
    }
    public static void removeSubject(Subject s) { subjects.remove(s); }

    // ---- Teachers ----
    public static List<Teacher> getTeachers() { return teachers; }
    public static void addTeacher(Teacher t) { teachers.add(t); }
    public static void removeTeacher(Teacher t) { teachers.remove(t); }

    // ---- Rooms ----
    public static List<Room> getRooms() { return rooms; }
    public static void addRoom(Room r) {
        rooms.add(r);
        addRoomTypeOption(r.getType());
    }
    public static void removeRoom(Room r) { rooms.remove(r); }

    // ---- Type Options ----
    public static List<String> getSubjectTypeOptions() {
        return java.util.Collections.unmodifiableList(subjectTypeOptions);
    }

    public static List<String> getRoomTypeOptions() {
        return java.util.Collections.unmodifiableList(roomTypeOptions);
    }

    public static void addSubjectTypeOption(String type) {
        String t = normalizeType(type);
        if (t == null) return;
        if (subjectTypeOptions.stream().noneMatch(x -> x.equalsIgnoreCase(t))) {
            subjectTypeOptions.add(t);
        }
    }

    public static void removeSubjectTypeOption(String type) {
        String t = normalizeType(type);
        if (t == null) return;
        // Keep defaults unless full reset is requested.
        if (DEFAULT_SUBJECT_TYPES.stream().anyMatch(x -> x.equalsIgnoreCase(t))) return;
        subjectTypeOptions.removeIf(x -> x.equalsIgnoreCase(t));
        multiGroupSubjectTypes.removeIf(x -> x.equalsIgnoreCase(t));
    }

    public static void resetSubjectTypeOptions() {
        subjectTypeOptions.clear();
        subjectTypeOptions.addAll(DEFAULT_SUBJECT_TYPES);
        // Keep existing subject data usable in forms.
        for (Subject s : subjects) {
            if (s.getType() == null) continue;
            for (String part : s.getType().split(";")) addSubjectTypeOption(part);
        }
    }

    public static void renameSubjectTypeOption(String oldName, String newName) {
        String old = normalizeType(oldName);
        String neu = normalizeType(newName);
        if (old == null || neu == null) return;
        for (int i = 0; i < subjectTypeOptions.size(); i++) {
            if (subjectTypeOptions.get(i).equalsIgnoreCase(old)) {
                subjectTypeOptions.set(i, neu);
                // Update multi-group mapping if needed
                if (multiGroupSubjectTypes.stream().anyMatch(x -> x.equalsIgnoreCase(old))) {
                    multiGroupSubjectTypes.removeIf(x -> x.equalsIgnoreCase(old));
                    multiGroupSubjectTypes.add(neu);
                }
                return;
            }
        }
    }

    public static void addRoomTypeOption(String type) {
        String t = normalizeType(type);
        if (t == null) return;
        if (roomTypeOptions.stream().noneMatch(x -> x.equalsIgnoreCase(t))) {
            roomTypeOptions.add(t);
        }
        // Default mode for new room type: disabled.
        setRoomTypeAssignableToGroups(t, false);
        setRoomTypeAssignableToSubjects(t, false);
    }

    public static void removeRoomTypeOption(String type) {
        String t = normalizeType(type);
        if (t == null) return;
        if (DEFAULT_ROOM_TYPES.stream().anyMatch(x -> x.equalsIgnoreCase(t))) return;
        roomTypeOptions.removeIf(x -> x.equalsIgnoreCase(t));
        roomTypesAssignableToGroups.removeIf(x -> x.equalsIgnoreCase(t));
        roomTypesAssignableToSubjects.removeIf(x -> x.equalsIgnoreCase(t));
    }

    public static void resetRoomTypeOptions() {
        roomTypeOptions.clear();
        roomTypeOptions.addAll(DEFAULT_ROOM_TYPES);
        for (Room r : rooms) addRoomTypeOption(r.getType());
        roomTypesAssignableToGroups.clear();
        roomTypesAssignableToSubjects.clear();
    }

    public static void renameRoomTypeOption(String oldName, String newName) {
        String old = normalizeType(oldName);
        String neu = normalizeType(newName);
        if (old == null || neu == null) return;
        boolean groupAssignable = isRoomTypeAssignableToGroups(old);
        boolean subjectAssignable = isRoomTypeAssignableToSubjects(old);
        for (int i = 0; i < roomTypeOptions.size(); i++) {
            if (roomTypeOptions.get(i).equalsIgnoreCase(old)) {
                roomTypeOptions.set(i, neu);
                roomTypesAssignableToGroups.removeIf(x -> x.equalsIgnoreCase(old));
                roomTypesAssignableToSubjects.removeIf(x -> x.equalsIgnoreCase(old));
                if (groupAssignable) roomTypesAssignableToGroups.add(neu);
                if (subjectAssignable) roomTypesAssignableToSubjects.add(neu);
                return;
            }
        }
    }

    public static boolean isRoomTypeAssignableToGroups(String type) {
        String t = normalizeType(type);
        if (t == null) return false;
        for (String x : roomTypesAssignableToGroups) {
            if (x.equalsIgnoreCase(t)) return true;
        }
        return false;
    }

    public static boolean isRoomTypeAssignableToSubjects(String type) {
        String t = normalizeType(type);
        if (t == null) return false;
        for (String x : roomTypesAssignableToSubjects) {
            if (x.equalsIgnoreCase(t)) return true;
        }
        return false;
    }

    public static void setRoomTypeAssignableToGroups(String type, boolean allowed) {
        String t = normalizeType(type);
        if (t == null) return;
        roomTypesAssignableToGroups.removeIf(x -> x.equalsIgnoreCase(t));
        if (allowed) {
            roomTypesAssignableToSubjects.removeIf(x -> x.equalsIgnoreCase(t));
            roomTypesAssignableToGroups.add(t);
        }
    }

    public static void setRoomTypeAssignableToSubjects(String type, boolean allowed) {
        String t = normalizeType(type);
        if (t == null) return;
        roomTypesAssignableToSubjects.removeIf(x -> x.equalsIgnoreCase(t));
        if (allowed) {
            roomTypesAssignableToGroups.removeIf(x -> x.equalsIgnoreCase(t));
            roomTypesAssignableToSubjects.add(t);
        }
    }

    public static java.util.Set<String> getMultiGroupSubjectTypes() {
        return java.util.Collections.unmodifiableSet(multiGroupSubjectTypes);
    }

    public static void setMultiGroupSubjectTypes(java.util.Set<String> types) {
        multiGroupSubjectTypes.clear();
        if (types != null) {
            for (String t : types) {
                String normalized = normalizeType(t);
                if (normalized != null) multiGroupSubjectTypes.add(normalized);
            }
        }
    }

    public static void resetMultiGroupSubjectTypes() {
        multiGroupSubjectTypes.clear();
        multiGroupSubjectTypes.addAll(DEFAULT_MULTI_GROUP_SUBJECT_TYPES);
    }

    private static String normalizeType(String type) {
        if (type == null) return null;
        String t = type.trim();
        return t.isEmpty() ? null : t;
    }

    // ---- Student Groups ----
    public static List<StudentGroup> getGroups() { return groups; }
    public static void addGroup(StudentGroup g) { groups.add(g); }
    public static void removeGroup(StudentGroup g) { groups.remove(g); }

    // ---- Students ----
    public static List<Student> getStudents() { return students; }
    public static void addStudent(Student s) { students.add(s); }
    public static void removeStudent(Student s) { students.remove(s); }

    // ---- Time Slots ----
    public static List<TimeSlot> getTimeSlots() { return timeSlots; }
    public static void addTimeSlot(TimeSlot ts) { timeSlots.add(ts); }
    public static void clearTimeSlots() { timeSlots.clear(); }

    // ---- Schedule ----
    public static List<ScheduleEntry> getSchedule() { return schedule; }
    public static void setSchedule(List<ScheduleEntry> entries) {
        schedule.clear();
        schedule.addAll(entries);
    }
    public static void clearSchedule() { schedule.clear(); }

    // ---- Multi-week (rotational) schedules ----
    public static List<List<ScheduleEntry>> getWeekSchedules() { return weekSchedules; }

    /** Store multiple week schedules; also sets schedule = week 1 for backward compatibility. */
    public static void setWeekSchedules(List<List<ScheduleEntry>> weeks) {
        weekSchedules.clear();
        weekSchedules.addAll(weeks);
        schedule.clear();
        if (!weeks.isEmpty()) schedule.addAll(weeks.get(0));
    }

    public static void clearWeekSchedules() {
        weekSchedules.clear();
        schedule.clear();
    }

    /**
     * Generate time slots from working days and hourly intervals with minute precision.
     * startHour/startMinute and endHour/endMinute in 24h format.
     * breakHours contains start-hours of 1-hour blocks to exclude (e.g., {12} skips 12:xx-13:xx).
     */
    public static void generateTimeSlots(String[] days, int startHour, int startMinute,
                                         int endHour, int endMinute,
                                         java.util.Set<Integer> breakMinutes) {
        timeSlots.clear();
        int startTotal = startHour * 60 + startMinute;
        int endTotal   = endHour   * 60 + endMinute;
        for (String day : days) {
            for (int t = startTotal; t + 60 <= endTotal; t += 60) {
                if (breakMinutes.contains(t)) continue;
                int h1 = t / 60,         m1 = t % 60;
                int h2 = (t + 60) / 60,  m2 = (t + 60) % 60;
                String time = String.format("%02d:%02d - %02d:%02d", h1, m1, h2, m2);
                timeSlots.add(new TimeSlot(day, time));
            }
        }
    }

    /**
     * Generate time slots from working days and hourly intervals (whole-hour precision).
     * startHour / endHour in 24h format (e.g., 8, 17).
     * breakHours contains start-hours of 1-hour blocks to exclude (e.g., {12} skips 12:00-13:00).
     */
    public static void generateTimeSlots(String[] days, int startHour, int endHour,
                                         java.util.Set<Integer> breakHours) {
        // Convert hour-based set to total-minute starts for the new engine
        java.util.Set<Integer> breakMinutes = breakHours.stream()
                .map(h -> h * 60)
                .collect(java.util.stream.Collectors.toSet());
        generateTimeSlots(days, startHour, 0, endHour, 0, breakMinutes);
    }

    /** Convenience overload with no break hours. */
    public static void generateTimeSlots(String[] days, int startHour, int endHour) {
        generateTimeSlots(days, startHour, 0, endHour, 0, java.util.Collections.emptySet());
    }

    // ---- Break Minutes ----
    public static java.util.Set<Integer> getLastBreakMinutes() { return lastBreakMinutes; }
    public static void setLastBreakMinutes(java.util.Set<Integer> minutes) {
        lastBreakMinutes = java.util.Collections.unmodifiableSet(new java.util.HashSet<>(minutes));
    }

    // ---- Persistence ----

    /** Save all data to a .tmt file. */
    public static void saveProject(java.io.File file) throws java.io.IOException {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.BufferedOutputStream(new java.io.FileOutputStream(file)))) {
            java.util.Map<String, Object> snap = new java.util.LinkedHashMap<>();
            snap.put("subjects", new java.util.ArrayList<>(subjects));
            snap.put("teachers", new java.util.ArrayList<>(teachers));
            snap.put("rooms", new java.util.ArrayList<>(rooms));
            snap.put("groups", new java.util.ArrayList<>(groups));
            snap.put("students", new java.util.ArrayList<>(students));
            snap.put("timeSlots", new java.util.ArrayList<>(timeSlots));
            snap.put("schedule", new java.util.ArrayList<>(schedule));
            snap.put("weekSchedules", new java.util.ArrayList<>(weekSchedules));
            snap.put("lastBreakMinutes", new java.util.HashSet<>(lastBreakMinutes));
            snap.put("subjectTypeOptions", new java.util.ArrayList<>(subjectTypeOptions));
            snap.put("roomTypeOptions", new java.util.ArrayList<>(roomTypeOptions));
            snap.put("roomTypesAssignableToGroups", new java.util.LinkedHashSet<>(roomTypesAssignableToGroups));
            snap.put("roomTypesAssignableToSubjects", new java.util.LinkedHashSet<>(roomTypesAssignableToSubjects));
            snap.put("multiGroupSubjectTypes", new java.util.LinkedHashSet<>(multiGroupSubjectTypes));
            oos.writeObject(snap);
        }
    }

    /** Load all data from a .tmt file, replacing current data. */
    @SuppressWarnings("unchecked")
    public static void loadProject(java.io.File file) throws java.io.IOException, ClassNotFoundException {
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.BufferedInputStream(new java.io.FileInputStream(file)))) {
            Object obj = ois.readObject();
            if (!(obj instanceof java.util.Map<?, ?> mapRaw)) {
                throw new java.io.IOException("Unsupported project file format.");
            }
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> e : mapRaw.entrySet()) {
                if (e.getKey() instanceof String k) map.put(k, e.getValue());
            }

            subjects.clear();
            subjects.addAll((java.util.List<model.Subject>) map.getOrDefault("subjects", java.util.Collections.emptyList()));

            teachers.clear();
            teachers.addAll((java.util.List<model.Teacher>) map.getOrDefault("teachers", java.util.Collections.emptyList()));

            rooms.clear();
            rooms.addAll((java.util.List<model.Room>) map.getOrDefault("rooms", java.util.Collections.emptyList()));

            groups.clear();
            groups.addAll((java.util.List<model.StudentGroup>) map.getOrDefault("groups", java.util.Collections.emptyList()));

            students.clear();
            students.addAll((java.util.List<model.Student>) map.getOrDefault("students", java.util.Collections.emptyList()));

            timeSlots.clear();
            timeSlots.addAll((java.util.List<model.TimeSlot>) map.getOrDefault("timeSlots", java.util.Collections.emptyList()));

            schedule.clear();
            schedule.addAll((java.util.List<model.ScheduleEntry>) map.getOrDefault("schedule", java.util.Collections.emptyList()));

            weekSchedules.clear();
            weekSchedules.addAll((java.util.List<java.util.List<model.ScheduleEntry>>) map.getOrDefault(
                    "weekSchedules", java.util.Collections.emptyList()));

            java.util.Set<Integer> mins = (java.util.Set<Integer>) map.get("lastBreakMinutes");
            if (mins == null) mins = java.util.Collections.emptySet();
            lastBreakMinutes = java.util.Collections.unmodifiableSet(new java.util.HashSet<>(mins));

            subjectTypeOptions.clear();
            subjectTypeOptions.addAll((java.util.List<String>) map.getOrDefault(
                    "subjectTypeOptions", new java.util.ArrayList<>(DEFAULT_SUBJECT_TYPES)));
            for (Subject s : subjects) {
                if (s.getType() == null) continue;
                for (String part : s.getType().split(";")) addSubjectTypeOption(part);
            }

            roomTypeOptions.clear();
            roomTypeOptions.addAll((java.util.List<String>) map.getOrDefault(
                    "roomTypeOptions", new java.util.ArrayList<>(DEFAULT_ROOM_TYPES)));
            for (Room r : rooms) addRoomTypeOption(r.getType());

            boolean hasGroupCaps = map.containsKey("roomTypesAssignableToGroups");
            boolean hasSubjectCaps = map.containsKey("roomTypesAssignableToSubjects");

            roomTypesAssignableToGroups.clear();
            roomTypesAssignableToGroups.addAll((java.util.Set<String>) map.getOrDefault(
                    "roomTypesAssignableToGroups", new java.util.LinkedHashSet<>(roomTypeOptions)));

            roomTypesAssignableToSubjects.clear();
            roomTypesAssignableToSubjects.addAll((java.util.Set<String>) map.getOrDefault(
                    "roomTypesAssignableToSubjects", new java.util.LinkedHashSet<>(roomTypeOptions)));

            // Backward compatibility for older project files without capability flags.
            if (!hasGroupCaps) {
                roomTypesAssignableToGroups.clear();
            }
            if (!hasSubjectCaps) {
                for (String t : roomTypeOptions) roomTypesAssignableToSubjects.add(t);
            }

            // Enforce exclusive mode: if both were true in old snapshots, keep subject-only.
            for (String t : roomTypeOptions) {
                if (isRoomTypeAssignableToGroups(t) && isRoomTypeAssignableToSubjects(t)) {
                    roomTypesAssignableToGroups.removeIf(x -> x.equalsIgnoreCase(t));
                }
            }

                multiGroupSubjectTypes.clear();
                multiGroupSubjectTypes.addAll((java.util.Set<String>) map.getOrDefault(
                    "multiGroupSubjectTypes", new java.util.LinkedHashSet<>(DEFAULT_MULTI_GROUP_SUBJECT_TYPES)));
        }
    }

    /** Clear all data in the store. */
    public static void clearAll() {
        subjects.clear();
        teachers.clear();
        rooms.clear();
        groups.clear();
        students.clear();
        timeSlots.clear();
        schedule.clear();
        weekSchedules.clear();
        lastBreakMinutes = java.util.Collections.emptySet();
        subjectTypeOptions.clear();
        subjectTypeOptions.addAll(DEFAULT_SUBJECT_TYPES);
        roomTypeOptions.clear();
        roomTypeOptions.addAll(DEFAULT_ROOM_TYPES);
        roomTypesAssignableToGroups.clear();
        roomTypesAssignableToSubjects.clear();
        multiGroupSubjectTypes.clear();
        multiGroupSubjectTypes.addAll(DEFAULT_MULTI_GROUP_SUBJECT_TYPES);
    }
}
