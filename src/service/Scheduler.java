package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import model.*;
import util.DataStore;

public class Scheduler {

    private int maxDaysPerGroup = 0;

    public void setMaxDaysPerGroup(int max) {
        this.maxDaysPerGroup = max;
    }

    /**
     * Generates a conflict-free schedule using a greedy algorithm.
     *
     * Respects: teacher conflicts, room conflicts, group conflicts,
     * room capacity, room type for Labs, per-subject preferred weekday,
     * all-groups shared-slot constraint, and per-subject session duration.
     *
     * A subject with sessionDuration=2 and weeklyHours=4 produces 2 sessions
     * of 2 consecutive hours each.  The timetable shows the merged time range,
     * e.g. "08:00 - 10:00".
     */
    public List<ScheduleEntry> generate() {
        return generate(msg -> {});
    }

    public List<ScheduleEntry> generate(Consumer<String> log) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        List<StudentGroup> groups   = DataStore.getGroups();
        List<Teacher>      teachers = DataStore.getTeachers();
        List<Room>         rooms    = DataStore.getRooms();
        List<TimeSlot>     slots    = DataStore.getTimeSlots();

        if (groups.isEmpty() || teachers.isEmpty() || rooms.isEmpty() || slots.isEmpty()) {
            return schedule;
        }

        int totalGroups = groups.size();
        int groupIdx    = 0;
        Map<Subject, Map<StudentGroup, Integer>> sharedSessionsByGroup = new HashMap<>();
        Map<StudentGroup, java.util.Set<String>> groupDayUsage = new HashMap<>();
        List<Student> students = DataStore.getStudents();

        // Build map: per group, reserved hours needed on each preferred locked day.
        Map<StudentGroup, Map<String, Integer>> lockedDayRequiredHours = new HashMap<>();
        for (StudentGroup grp : groups) {
            for (Subject s : grp.getSubjects()) {
                if (s.isPreferredDayOnly() && s.getPreferredDay() != null) {
                    lockedDayRequiredHours
                            .computeIfAbsent(grp, k -> new HashMap<>())
                            .merge(s.getPreferredDay(), requiredScheduledHours(s), Integer::sum);
                }
            }
        }

        for (StudentGroup group : groups) {
            groupIdx++;
            log.accept("  Group " + groupIdx + "/" + totalGroups + ": " + group.getName()
                    + " (" + group.getSubjects().size() + " subject(s))");

            List<Subject> orderedSubjects = orderSubjects(group.getSubjects());

            for (Subject subject : orderedSubjects) {
                int dur = subject.getSessionDuration();
                int sessionsNeeded = (int) Math.ceil((double) subject.getWeeklyHours() / dur);
                List<TimeSlot> candidates = candidateSlotsFor(subject, slots);

                log.accept("    └ " + subject.getName() + " [" + subject.getType() + "]"
                        + " — " + sessionsNeeded + " session(s) of " + dur + "h needed");

                if (subject.isAllGroups()) {
                    List<StudentGroup> sharedGroups = groups.stream()
                            .filter(grp -> grp.getSubjects().stream().anyMatch(s -> s.equals(subject)))
                            .collect(Collectors.toList());
                    if (sharedGroups.isEmpty()) {
                        log.accept("      ⚠ No groups found for shared subject; skipped");
                        continue;
                    }

                    Map<StudentGroup, Integer> doneMap = sharedSessionsByGroup.computeIfAbsent(subject, k -> new HashMap<>());
                    int doneForCurrent = doneMap.getOrDefault(group, 0);
                    if (doneForCurrent >= sessionsNeeded) {
                        log.accept("      ✓ Already scheduled (shared slot)");
                        continue;
                    }

                    int placed = doneForCurrent;
                    int lastAttemptSeats = actualRequiredSeats(sharedGroups, subject, students);

                    while (placed < sessionsNeeded) {
                        PlacementChoice best = null;
                        List<StudentGroup> pending = sharedGroups.stream()
                                .filter(grp -> doneMap.getOrDefault(grp, 0) < sessionsNeeded)
                                .collect(Collectors.toList());
                        if (pending.isEmpty()) break;

                        List<StudentGroup> activeGroups = new ArrayList<>(pending);
                        Integer maxCap = maxEligibleRoomCapacity(subject, rooms,
                                activeGroups.size() == 1 ? activeGroups.get(0) : null);
                        while (activeGroups.size() > 1) {
                            int seats = actualRequiredSeats(activeGroups, subject, students);
                            if (maxCap != null && seats <= maxCap) break;

                            StudentGroup drop = largestAttendanceGroup(activeGroups, subject, students);
                            activeGroups.remove(drop);
                            int afterSeats = actualRequiredSeats(activeGroups, subject, students);
                            log.accept("      ↺ Capacity exceeded, retrying without " + drop.getName()
                                    + " (required seats now " + afterSeats + ")");
                        }

                        int requiredSeats = actualRequiredSeats(activeGroups, subject, students);
                        lastAttemptSeats = requiredSeats;

                        boolean needPreferredDay = needsPreferredDayPlacement(subject, schedule, activeGroups);
                        List<TimeSlot> primaryCandidates = needPreferredDay
                                ? filterCandidatesByDay(candidates, subject.getPreferredDay())
                                : candidates;

                        for (TimeSlot slotStart : primaryCandidates) {
                            if (!hasConsecutiveSlots(slotStart, dur, slots)) continue;
                            int startHour = parseHour(slotStart.getTime());
                            String day = slotStart.getDay();

                            boolean groupsFree = activeGroups.stream().allMatch(
                                    grp -> isGroupFreeForBlock(schedule, day, startHour, dur, grp));
                            if (!groupsFree) continue;

                                boolean lockedCapacityOk = activeGroups.stream().allMatch(
                                    grp -> hasLockedDayCapacityForPlacement(subject, grp, day, dur, schedule, lockedDayRequiredHours));
                                if (!lockedCapacityOk) continue;

                            if (maxDaysPerGroup > 0) {
                                boolean dayOk = activeGroups.stream().allMatch(grp -> {
                                    java.util.Set<String> used = groupDayUsage.getOrDefault(grp, java.util.Collections.emptySet());
                                    return used.contains(day) || used.size() < maxDaysPerGroup;
                                });
                                if (!dayOk) continue;
                            }

                            Teacher t = findTeacherForBlock(subject, teachers, schedule, day, startHour, dur);
                            if (t == null) continue;

                            StudentGroup roomTarget = activeGroups.size() == 1 ? activeGroups.get(0) : null;
                            Room r = findRoomForBlock(subject, requiredSeats, rooms, schedule, day, startHour, dur, roomTarget);
                            if (r == null) continue;

                            int score = scoreAllGroupsPlacement(schedule, activeGroups, day, startHour, dur);
                            PlacementChoice c = new PlacementChoice(slotStart, t, r, score);
                            if (best == null || c.score < best.score) best = c;
                        }

                        if (best == null && needPreferredDay) {
                            for (TimeSlot slotStart : candidates) {
                                if (subject.getPreferredDay().equals(slotStart.getDay())) continue;
                                if (!hasConsecutiveSlots(slotStart, dur, slots)) continue;
                                int startHour = parseHour(slotStart.getTime());
                                String day = slotStart.getDay();

                                boolean groupsFree = activeGroups.stream().allMatch(
                                        grp -> isGroupFreeForBlock(schedule, day, startHour, dur, grp));
                                if (!groupsFree) continue;

                                boolean lockedCapacityOk = activeGroups.stream().allMatch(
                                    grp -> hasLockedDayCapacityForPlacement(subject, grp, day, dur, schedule, lockedDayRequiredHours));
                                if (!lockedCapacityOk) continue;

                                if (maxDaysPerGroup > 0) {
                                    boolean dayOk = activeGroups.stream().allMatch(grp -> {
                                        java.util.Set<String> used = groupDayUsage.getOrDefault(grp, java.util.Collections.emptySet());
                                        return used.contains(day) || used.size() < maxDaysPerGroup;
                                    });
                                    if (!dayOk) continue;
                                }

                                Teacher t = findTeacherForBlock(subject, teachers, schedule, day, startHour, dur);
                                if (t == null) continue;

                                StudentGroup roomTarget = activeGroups.size() == 1 ? activeGroups.get(0) : null;
                                Room r = findRoomForBlock(subject, requiredSeats, rooms, schedule, day, startHour, dur, roomTarget);
                                if (r == null) continue;

                                int score = scoreAllGroupsPlacement(schedule, activeGroups, day, startHour, dur);
                                PlacementChoice c = new PlacementChoice(slotStart, t, r, score);
                                if (best == null || c.score < best.score) best = c;
                            }
                        }

                        if (best == null) break;

                        int sh = parseHour(best.start.getTime());
                        String day = best.start.getDay();
                        TimeSlot merged = mergedSlot(day, sh, parseMinute(best.start.getTime()), dur);
                        for (StudentGroup grp : activeGroups) {
                            schedule.add(new ScheduleEntry(merged, subject, best.teacher, grp, best.room, dur));
                            doneMap.put(grp, doneMap.getOrDefault(grp, 0) + 1);
                        }
                        for (StudentGroup grp : activeGroups) {
                            groupDayUsage.computeIfAbsent(grp, k -> new java.util.HashSet<>()).add(day);
                        }
                        placed++;
                        log.accept("      ✓ Placed: " + day + " " + merged.getTime()
                            + " | " + best.teacher.getName() + " | " + best.room.getId()
                            + " | shared groups: " + activeGroups.size() + ", students: " + requiredSeats);
                    }

                    if (placed < sessionsNeeded) {
                        StudentGroup roomTarget = sharedGroups.size() == 1 ? sharedGroups.get(0) : null;
                        logPlacementFailure(log,
                            "shared groups: " + sharedGroups.stream()
                                .map(StudentGroup::getName)
                                .collect(Collectors.joining(", ")),
                            subject, candidates, schedule, teachers, rooms, lockedDayRequiredHours,
                            groupDayUsage, sharedGroups, roomTarget, lastAttemptSeats, dur,
                            sessionsNeeded, placed);
                    }
                } else {
                    int placed = 0;

                    while (placed < sessionsNeeded) {
                        PlacementChoice best = null;

                        boolean needPreferredDay = needsPreferredDayPlacement(subject, schedule, java.util.List.of(group));
                        List<TimeSlot> primaryCandidates = needPreferredDay
                                ? filterCandidatesByDay(candidates, subject.getPreferredDay())
                                : candidates;

                        for (TimeSlot slotStart : primaryCandidates) {
                            if (!hasConsecutiveSlots(slotStart, dur, slots)) continue;
                            int startHour = parseHour(slotStart.getTime());
                            String day = slotStart.getDay();

                            if (!isGroupFreeForBlock(schedule, day, startHour, dur, group)) continue;

                            if (!hasLockedDayCapacityForPlacement(subject, group, day, dur, schedule, lockedDayRequiredHours)) {
                                continue;
                            }

                            if (maxDaysPerGroup > 0) {
                                java.util.Set<String> usedDays = groupDayUsage.getOrDefault(group, java.util.Collections.emptySet());
                                if (!usedDays.contains(day) && usedDays.size() >= maxDaysPerGroup) continue;
                            }

                            Teacher t = findTeacherForBlock(subject, teachers, schedule, day, startHour, dur);
                            if (t == null) continue;

                            Room r = findRoomForBlock(subject, group.getSize(), rooms, schedule, day, startHour, dur, group);
                            if (r == null) continue;

                            int score = scorePlacement(schedule, group, day, startHour, dur);
                            PlacementChoice c = new PlacementChoice(slotStart, t, r, score);
                            if (best == null || c.score < best.score) best = c;
                        }

                        if (best == null && needPreferredDay) {
                            for (TimeSlot slotStart : candidates) {
                                if (subject.getPreferredDay().equals(slotStart.getDay())) continue;
                                if (!hasConsecutiveSlots(slotStart, dur, slots)) continue;
                                int startHour = parseHour(slotStart.getTime());
                                String day = slotStart.getDay();

                                if (!isGroupFreeForBlock(schedule, day, startHour, dur, group)) continue;

                                if (!hasLockedDayCapacityForPlacement(subject, group, day, dur, schedule, lockedDayRequiredHours)) {
                                    continue;
                                }

                                if (maxDaysPerGroup > 0) {
                                    java.util.Set<String> usedDays = groupDayUsage.getOrDefault(group, java.util.Collections.emptySet());
                                    if (!usedDays.contains(day) && usedDays.size() >= maxDaysPerGroup) continue;
                                }

                                Teacher t = findTeacherForBlock(subject, teachers, schedule, day, startHour, dur);
                                if (t == null) continue;

                                Room r = findRoomForBlock(subject, group.getSize(), rooms, schedule, day, startHour, dur, group);
                                if (r == null) continue;

                                int score = scorePlacement(schedule, group, day, startHour, dur);
                                PlacementChoice c = new PlacementChoice(slotStart, t, r, score);
                                if (best == null || c.score < best.score) best = c;
                            }
                        }

                        if (best == null) break;

                        int sh = parseHour(best.start.getTime());
                        String day = best.start.getDay();
                        TimeSlot merged = mergedSlot(day, sh, parseMinute(best.start.getTime()), dur);
                        schedule.add(new ScheduleEntry(merged, subject, best.teacher, group, best.room, dur));
                        groupDayUsage.computeIfAbsent(group, k -> new java.util.HashSet<>()).add(day);
                        placed++;
                        log.accept("      ✓ Placed: " + day + " " + merged.getTime()
                                + " | " + best.teacher.getName() + " | " + best.room.getId());
                    }

                    if (placed < sessionsNeeded) {
                        logPlacementFailure(log, "group " + group.getName(),
                                subject, candidates, schedule, teachers, rooms, lockedDayRequiredHours,
                                groupDayUsage, java.util.List.of(group), group, group.getSize(), dur,
                                sessionsNeeded, placed);
                    }
                }
            }
        }

        return schedule;
    }

    // -----------------------------------------------------------------------
    // Public utility — used by emergency reschedule
    // -----------------------------------------------------------------------

    /** Finds a free substitute teacher for the given subject/day/hour block. */
    public Teacher findSubstitute(Subject subject, List<Teacher> teachers,
                                   List<ScheduleEntry> schedule,
                                   String day, int sh, int dur) {
        return findTeacherForBlock(subject, teachers, schedule, day, sh, dur);
    }

    // -----------------------------------------------------------------------
    // Slot helpers
    // -----------------------------------------------------------------------

    /** Returns candidate slots. Preferred day is hard-filtered when locked, otherwise sorted first. */
    private List<TimeSlot> candidateSlotsFor(Subject subject, List<TimeSlot> allSlots) {
        String day = subject.getPreferredDay();
        List<TimeSlot> candidates;
        if (day != null && subject.isPreferredDayOnly()) {
            candidates = allSlots.stream().filter(s -> s.getDay().equals(day)).collect(Collectors.toList());
        } else {
            candidates = new ArrayList<>(allSlots);
        }
        if (subject.getPriority() >= 4) {
            // High priority → prefer morning slots (sort ascending by hour for consistent early placement)
            candidates.sort(Comparator.comparingInt(s -> parseHour(s.getTime())));
        } else {
            Collections.shuffle(candidates);
        }
        if (day != null && !subject.isPreferredDayOnly()) {
            List<TimeSlot> preferred = candidates.stream()
                    .filter(s -> day.equals(s.getDay()))
                    .collect(Collectors.toList());
            List<TimeSlot> other = candidates.stream()
                    .filter(s -> !day.equals(s.getDay()))
                    .collect(Collectors.toList());
            preferred.addAll(other);
            candidates = preferred;
        }
        return candidates;
    }

    private List<TimeSlot> filterCandidatesByDay(List<TimeSlot> candidates, String day) {
        if (day == null || day.isBlank()) return candidates;
        return candidates.stream().filter(s -> day.equals(s.getDay())).collect(Collectors.toList());
    }

    private boolean needsPreferredDayPlacement(Subject subject, List<ScheduleEntry> schedule,
                                               List<StudentGroup> targetGroups) {
        String preferredDay = subject.getPreferredDay();
        if (preferredDay == null || preferredDay.isBlank() || subject.isPreferredDayOnly()) return false;
        return schedule.stream().noneMatch(e -> e.getSubject().equals(subject)
                && preferredDay.equals(e.getSlot().getDay())
                && targetGroups.stream().anyMatch(g -> g.equals(e.getGroup())));
    }

    private int requiredScheduledHours(Subject subject) {
        int dur = subject.getSessionDuration();
        int sessionsNeeded = (int) Math.ceil((double) subject.getWeeklyHours() / dur);
        return sessionsNeeded * dur;
    }

    private int dayCapacityHours(String day) {
        return (int) DataStore.getTimeSlots().stream()
                .filter(ts -> day.equals(ts.getDay()))
                .count();
    }

    private int usedHoursOnDay(List<ScheduleEntry> schedule, StudentGroup group, String day) {
        return schedule.stream()
                .filter(e -> e.getGroup().equals(group) && day.equals(e.getSlot().getDay()))
                .mapToInt(ScheduleEntry::getDurationHours)
                .sum();
    }

    private int lockedHoursPlacedOnDay(List<ScheduleEntry> schedule, StudentGroup group, String day) {
        return schedule.stream()
                .filter(e -> e.getGroup().equals(group)
                        && day.equals(e.getSlot().getDay())
                        && e.getSubject().isPreferredDayOnly()
                        && day.equals(e.getSubject().getPreferredDay()))
                .mapToInt(ScheduleEntry::getDurationHours)
                .sum();
    }

    private boolean hasLockedDayCapacityForPlacement(Subject subject, StudentGroup group, String day, int dur,
                                                     List<ScheduleEntry> schedule,
                                                     Map<StudentGroup, Map<String, Integer>> lockedDayRequiredHours) {
        int dayCapacity = dayCapacityHours(day);
        if (dayCapacity <= 0) return false;

        int lockedRequiredHours = lockedDayRequiredHours
                .getOrDefault(group, java.util.Collections.emptyMap())
                .getOrDefault(day, 0);

        boolean isLockedToThisDay = subject.isPreferredDayOnly() && day.equals(subject.getPreferredDay());

        // If this day has locked subjects for this group, only locked subjects may be placed here.
        if (lockedRequiredHours > 0 && !isLockedToThisDay) return false;

        int usedHoursAfter = usedHoursOnDay(schedule, group, day) + dur;
        int lockedPlacedAfter = lockedHoursPlacedOnDay(schedule, group, day);
        if (isLockedToThisDay) {
            lockedPlacedAfter += dur;
        }
        int lockedHoursRemainingAfter = Math.max(0, lockedRequiredHours - lockedPlacedAfter);
        return dayCapacity - usedHoursAfter >= lockedHoursRemainingAfter;
    }

    /**
     * Checks that {@code dur} consecutive 1-hour slots exist in the master slot
     * list starting from {@code start} (same day, no gap).
     */
    private boolean hasConsecutiveSlots(TimeSlot start, int dur, List<TimeSlot> slots) {
        if (dur <= 1) return true;
        int idx = slots.indexOf(start);
        if (idx < 0) return false;
        int sh = parseHour(start.getTime());
        for (int i = 1; i < dur; i++) {
            if (idx + i >= slots.size()) return false;
            TimeSlot next = slots.get(idx + i);
            if (!next.getDay().equals(start.getDay())) return false;
            if (parseHour(next.getTime()) != sh + i) return false;
        }
        return true;
    }

    /** Creates a display TimeSlot spanning {@code dur} hours from {@code startHour:startMinute}. */
    private TimeSlot mergedSlot(String day, int startHour, int startMinute, int dur) {
        int endTotal = startHour * 60 + startMinute + dur * 60;
        String time = String.format("%02d:%02d - %02d:%02d",
                startHour, startMinute, endTotal / 60, endTotal % 60);
        return new TimeSlot(day, time);
    }

    /** Parses the start minute from "HH:mm - HH:mm". */
    private int parseMinute(String time) {
        try { return Integer.parseInt(time.substring(3, 5)); }
        catch (NumberFormatException e) { return 0; }
    }

    /** Parses the start hour from "HH:mm - HH:mm". */
    private int parseHour(String time) {
        try { return Integer.parseInt(time.substring(0, 2)); }
        catch (NumberFormatException e) { return 0; }
    }

    // -----------------------------------------------------------------------
    // Conflict detection (overlap-aware)
    // -----------------------------------------------------------------------

    /**
     * Returns true if an existing entry overlaps with the proposed block
     * [startHour, startHour+dur) on the given day.
     */
    private boolean overlaps(ScheduleEntry e, String day, int startHour, int dur) {
        if (!e.getSlot().getDay().equals(day)) return false;
        int es = e.getStartHour();
        int ed = e.getDurationHours();
        return startHour < es + ed && es < startHour + dur;
    }

    private boolean isGroupFreeForBlock(List<ScheduleEntry> schedule,
                                         String day, int sh, int dur, StudentGroup group) {
        for (ScheduleEntry e : schedule) {
            if (e.getGroup().equals(group) && overlaps(e, day, sh, dur)) return false;
        }
        return true;
    }

    private Teacher findTeacherForBlock(Subject subject, List<Teacher> teachers,
                                         List<ScheduleEntry> schedule,
                                         String day, int sh, int dur) {
        for (Teacher t : teachers) {
            if (!teachesSubject(t, subject)) continue;
            if (!t.isAvailable(day)) continue;
            boolean free = true;
            for (ScheduleEntry e : schedule) {
                if (e.getTeacher().equals(t) && overlaps(e, day, sh, dur)) { free = false; break; }
            }
            if (free) return t;
        }
        return null;
    }

    private Room findRoomForBlock(Subject subject, int requiredCapacity,
                                   List<Room> rooms, List<ScheduleEntry> schedule,
                                   String day, int sh, int dur, StudentGroup targetGroup) {
        List<Room> candidates = candidateRoomsFor(subject, targetGroup, rooms);
        for (Room r : candidates) {
            if (r.getCapacity() < requiredCapacity) continue;
            if (subject.getType() != null && subject.getType().contains("Lab") && !"Lab".equals(r.getType())) continue;
            if (r.isBlockedAt(day, sh, dur)) continue;
            boolean free = true;
            for (ScheduleEntry e : schedule) {
                if (e.getRoom().equals(r) && overlaps(e, day, sh, dur)) { free = false; break; }
            }
            if (free) return r;
        }
        return null;
    }

    /**
     * Room assignment policy:
     * 1) If a subject has dedicated room(s), all its groups use those room(s).
     * 2) Else, if a group has dedicated room(s), that group's subjects use those room(s).
     * 3) Else, any room can be used.
     */
    private List<Room> candidateRoomsFor(Subject subject, StudentGroup group, List<Room> rooms) {
        List<Room> bySubject = new ArrayList<>();
        for (Room r : rooms) {
            if (equalsIgnoreCaseSafe(r.getAssignedSubject(), subject.getName())) bySubject.add(r);
        }
        if (!bySubject.isEmpty()) return bySubject;

        if (group != null) {
            List<Room> byGroup = new ArrayList<>();
            for (Room r : rooms) {
                if (equalsIgnoreCaseSafe(r.getAssignedGroup(), group.getName())) byGroup.add(r);
            }
            if (!byGroup.isEmpty()) return byGroup;
        }

        return rooms;
    }

    private boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private int actualRequiredSeats(List<StudentGroup> groups, Subject subject, List<Student> students) {
        int sum = 0;
        for (StudentGroup g : groups) sum += attendeesForGroupSubject(g, subject, students);
        return sum;
    }

    private int attendeesForGroupSubject(StudentGroup group, Subject subject, List<Student> students) {
        int actual = 0;
        for (Student st : students) {
            if (st.getGroup() == null || !st.getGroup().equals(group)) continue;
            if (st.allSubjects().stream().anyMatch(s -> s.equals(subject))) actual++;
        }
        // If no student-level assignment data exists for this group+subject, fall back to expected group size.
        return actual > 0 ? actual : group.getSize();
    }

    private StudentGroup largestAttendanceGroup(List<StudentGroup> groups, Subject subject, List<Student> students) {
        StudentGroup best = groups.get(0);
        int bestSize = attendeesForGroupSubject(best, subject, students);
        for (int i = 1; i < groups.size(); i++) {
            StudentGroup g = groups.get(i);
            int sz = attendeesForGroupSubject(g, subject, students);
            if (sz > bestSize) {
                best = g;
                bestSize = sz;
            }
        }
        return best;
    }

    private Integer maxEligibleRoomCapacity(Subject subject, List<Room> rooms, StudentGroup targetGroup) {
        List<Room> candidates = candidateRoomsFor(subject, targetGroup, rooms);
        int max = -1;
        for (Room r : candidates) {
            if (subject.getType() != null && subject.getType().contains("Lab") && !"Lab".equals(r.getType())) {
                continue;
            }
            if (r.getCapacity() > max) max = r.getCapacity();
        }
        return max < 0 ? null : max;
    }

    private void logPlacementFailure(Consumer<String> log, String targetLabel,
                                     Subject subject, List<TimeSlot> candidates,
                                     List<ScheduleEntry> schedule, List<Teacher> teachers,
                         List<Room> rooms, Map<StudentGroup, Map<String, Integer>> lockedDayRequiredHours,
                                     Map<StudentGroup, java.util.Set<String>> groupDayUsage,
                                     List<StudentGroup> groups, StudentGroup roomTarget,
                                     int requiredCapacity, int dur,
                                     int sessionsNeeded, int placed) {
        FailureStats stats = analyzeFailure(subject, candidates, schedule, teachers, rooms,
            lockedDayRequiredHours, groupDayUsage, groups, roomTarget, requiredCapacity, dur);

        log.accept("      ⚠ Placed " + placed + "/" + sessionsNeeded + " session(s) for " + targetLabel + ".");
        for (String need : buildFailureMessages(targetLabel, subject, stats, requiredCapacity, dur)) {
            log.accept("      ⚠ Need: " + need);
        }
    }

    private FailureStats analyzeFailure(Subject subject, List<TimeSlot> candidates,
                                        List<ScheduleEntry> schedule, List<Teacher> teachers,
                                        List<Room> rooms, Map<StudentGroup, Map<String, Integer>> lockedDayRequiredHours,
                                        Map<StudentGroup, java.util.Set<String>> groupDayUsage,
                                        List<StudentGroup> groups, StudentGroup roomTarget,
                                        int requiredCapacity, int dur) {
        FailureStats stats = new FailureStats();
        stats.totalCandidates = candidates.size();
        stats.qualifiedTeachers = teachers.stream()
                .filter(t -> teachesSubject(t, subject))
                .map(Teacher::getName)
                .sorted()
                .collect(Collectors.toList());

        List<Room> eligibleRooms = candidateRoomsFor(subject, roomTarget, rooms).stream()
                .filter(r -> subject.getType() == null || !subject.getType().contains("Lab") || "Lab".equals(r.getType()))
                .collect(Collectors.toList());
        stats.eligibleRoomCount = eligibleRooms.size();
        for (Room room : eligibleRooms) {
            if (room.getCapacity() > stats.maxEligibleCapacity) stats.maxEligibleCapacity = room.getCapacity();
        }

        for (TimeSlot slotStart : candidates) {
            if (!hasConsecutiveSlots(slotStart, dur, DataStore.getTimeSlots())) {
                stats.nonConsecutiveSlots++;
                continue;
            }

            int startHour = parseHour(slotStart.getTime());
            String day = slotStart.getDay();

                boolean lockedCapacityOk = groups.stream().allMatch(
                    g -> hasLockedDayCapacityForPlacement(subject, g, day, dur, schedule, lockedDayRequiredHours));
                if (!lockedCapacityOk) {
                stats.dayLockedSlots++;
                stats.lockedDays.add(day);
                continue;
            }

            boolean groupsFree = groups.stream().allMatch(g -> isGroupFreeForBlock(schedule, day, startHour, dur, g));
            if (!groupsFree) {
                stats.groupBusySlots++;
                continue;
            }

            if (maxDaysPerGroup > 0) {
                boolean dayOk = groups.stream().allMatch(g -> {
                    java.util.Set<String> used = groupDayUsage.getOrDefault(g, java.util.Collections.emptySet());
                    return used.contains(day) || used.size() < maxDaysPerGroup;
                });
                if (!dayOk) {
                    stats.dayLimitSlots++;
                    continue;
                }
            }

            stats.afterGroupChecks++;

            boolean teacherFree = teachers.stream().anyMatch(t ->
                    teachesSubject(t, subject)
                            && t.isAvailable(day)
                            && schedule.stream().noneMatch(e -> e.getTeacher().equals(t) && overlaps(e, day, startHour, dur)));
            if (!teacherFree) {
                stats.teacherUnavailableSlots++;
                continue;
            }

            stats.afterTeacherChecks++;

            boolean roomFree = eligibleRooms.stream().anyMatch(r ->
                    r.getCapacity() >= requiredCapacity
                            && !r.isBlockedAt(day, startHour, dur)
                            && schedule.stream().noneMatch(e -> e.getRoom().equals(r) && overlaps(e, day, startHour, dur)));
            if (!roomFree) {
                stats.roomUnavailableSlots++;
                continue;
            }

            stats.fullyFeasibleSlots++;
        }

        return stats;
    }

    private List<String> buildFailureMessages(String targetLabel, Subject subject,
                                              FailureStats stats, int requiredCapacity, int dur) {
        List<String> messages = new ArrayList<>();

        if (stats.totalCandidates == 0) {
            messages.add("generate working time slots first; none exist for the current day/time settings");
            return messages;
        }

        if (stats.nonConsecutiveSlots == stats.totalCandidates) {
            messages.add("a free block of " + dur + " consecutive hour(s); current timetable has no such continuous slot");
            return messages;
        }

        if (stats.qualifiedTeachers.isEmpty()) {
            messages.add("at least one teacher assigned to subject '" + subject.getName() + "'");
        }

        if (stats.eligibleRoomCount == 0) {
            String roomType = subject.getType() != null && subject.getType().contains("Lab") ? "Lab room" : "eligible room";
            messages.add(roomType + " matching the current room assignment rules");
        } else if (stats.maxEligibleCapacity > 0 && stats.maxEligibleCapacity < requiredCapacity) {
            messages.add("room capacity >= " + requiredCapacity + " seats; largest eligible room has "
                    + stats.maxEligibleCapacity + "");
        }

        if (stats.dayLockedSlots > 0 && stats.afterGroupChecks == 0) {
            messages.add("an open slot on allowed day(s); blocked by preferred-day locks on "
                    + String.join(", ", stats.lockedDays));
        }

        if (stats.groupBusySlots > 0 && stats.afterGroupChecks == 0) {
            messages.add("a free " + dur + "h slot for " + targetLabel + "; existing sessions already occupy all candidate windows");
        }

        if (stats.dayLimitSlots > 0 && stats.afterGroupChecks == 0) {
            messages.add("either a free slot on the already-used day(s), or a higher 'Limit to' value than " + maxDaysPerGroup);
        }

        if (!stats.qualifiedTeachers.isEmpty() && stats.teacherUnavailableSlots > 0 && stats.afterTeacherChecks == 0) {
            messages.add("one of these teachers free in the remaining windows: " + String.join(", ", stats.qualifiedTeachers));
        }

        if (stats.roomUnavailableSlots > 0 && stats.fullyFeasibleSlots == 0) {
            messages.add("an eligible room free at the same time as the teacher and " + targetLabel);
        }

        if (messages.isEmpty()) {
            messages.add("a common free teacher/room/time combination for " + targetLabel);
        }
        return messages;
    }

    private static class FailureStats {
        int totalCandidates;
        int nonConsecutiveSlots;
        int dayLockedSlots;
        int groupBusySlots;
        int dayLimitSlots;
        int teacherUnavailableSlots;
        int roomUnavailableSlots;
        int afterGroupChecks;
        int afterTeacherChecks;
        int fullyFeasibleSlots;
        int eligibleRoomCount;
        int maxEligibleCapacity;
        java.util.Set<String> lockedDays = new java.util.TreeSet<>();
        List<String> qualifiedTeachers = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // Ordering
    // -----------------------------------------------------------------------

    private List<Subject> orderSubjects(List<Subject> subjects) {
        List<Subject> ordered = new ArrayList<>(subjects);
        // Sort: priority desc (5→1), then type order (Lec→Tut→Lab)
        ordered.sort((a, b) -> {
            int p = Integer.compare(b.getPriority(), a.getPriority());
            if (p != 0) return p;
            return typeOrder(a.getType()) - typeOrder(b.getType());
        });
        return ordered;
    }

    private int typeOrder(String type) {
        if (type == null || type.isBlank()) return 4;
        int min = 4;
        for (String t : type.split(";")) {
            int o = switch (t.trim()) {
                case "Lec", "Theory"     -> 0;
                case "Tut"               -> 1;
                case "Lab", "Practical"  -> 2;
                default                  -> 3;
            };
            if (o < min) min = o;
        }
        return min;
    }

    private boolean teachesSubject(Teacher teacher, Subject subject) {
        for (Subject s : teacher.getSubjects()) {
            if (s.equals(subject)) return true;
        }
        return false;
    }

    /** Candidate placement with a computed compactness score (lower is better). */
    private static class PlacementChoice {
        final TimeSlot start;
        final Teacher teacher;
        final Room room;
        final int score;

        PlacementChoice(TimeSlot start, Teacher teacher, Room room, int score) {
            this.start = start;
            this.teacher = teacher;
            this.room = room;
            this.score = score;
        }
    }

    /** Score a candidate by penalizing fragmented single-slot holes and rewarding adjacency. */
    private int scorePlacement(List<ScheduleEntry> schedule, StudentGroup group, String day, int sh, int dur) {
        java.util.SortedSet<Integer> hours = new java.util.TreeSet<>();
        DataStore.getTimeSlots().stream()
                .filter(ts -> ts.getDay().equals(day))
                .forEach(ts -> hours.add(parseHour(ts.getTime())));

        if (hours.isEmpty()) return Integer.MAX_VALUE / 4;

        int singleHoles = 0;
        int shortHoles = 0;
        int segment = 0;
        int prev = Integer.MIN_VALUE;

        for (int h : hours) {
            boolean occupied = overlapsGroupOrCandidate(schedule, group, day, h, sh, dur);
            if (!occupied) {
                if (segment == 0 || h != prev + 1) {
                    if (segment == 1) singleHoles++;
                    else if (segment == 2) shortHoles++;
                    segment = 1;
                } else {
                    segment++;
                }
            } else {
                if (segment == 1) singleHoles++;
                else if (segment == 2) shortHoles++;
                segment = 0;
            }
            prev = h;
        }
        if (segment == 1) singleHoles++;
        else if (segment == 2) shortHoles++;

        boolean adjacentBefore = overlapsGroupOrCandidate(schedule, group, day, sh - 1, sh, dur);
        boolean adjacentAfter = overlapsGroupOrCandidate(schedule, group, day, sh + dur, sh, dur);
        int adjacencyBonus = (adjacentBefore ? 3 : 0) + (adjacentAfter ? 3 : 0);

        return singleHoles * 12 + shortHoles * 4 + sh - adjacencyBonus;
    }

    private int scoreAllGroupsPlacement(List<ScheduleEntry> schedule, List<StudentGroup> groups,
                                        String day, int sh, int dur) {
        int sum = 0;
        for (StudentGroup g : groups) sum += scorePlacement(schedule, g, day, sh, dur);
        return sum;
    }

    private boolean overlapsGroupOrCandidate(List<ScheduleEntry> schedule, StudentGroup group,
                                             String day, int slotHour,
                                             int candidateStart, int candidateDur) {
        boolean candidateCovers = slotHour >= candidateStart && slotHour < candidateStart + candidateDur;
        if (candidateCovers) return true;
        for (ScheduleEntry e : schedule) {
            if (!e.getGroup().equals(group)) continue;
            if (overlaps(e, day, slotHour, 1)) return true;
        }
        return false;
    }
}
