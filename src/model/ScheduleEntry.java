package model;

import java.io.Serializable;

public class ScheduleEntry implements Serializable {
    private final TimeSlot slot;       // displays merged time range, e.g. "08:00 - 10:00"
    private final Subject subject;
    private final Teacher teacher;
    private final StudentGroup group;
    private final Room room;
    private final int durationHours;   // how many 1-h base slots this entry occupies

    public ScheduleEntry(TimeSlot slot, Subject subject, Teacher teacher,
                         StudentGroup group, Room room) {
        this(slot, subject, teacher, group, room, 1);
    }

    public ScheduleEntry(TimeSlot slot, Subject subject, Teacher teacher,
                         StudentGroup group, Room room, int durationHours) {
        this.slot = slot;
        this.subject = subject;
        this.teacher = teacher;
        this.group = group;
        this.room = room;
        this.durationHours = (durationHours < 1) ? 1 : durationHours;
    }

    public TimeSlot getSlot() { return slot; }
    public Subject getSubject() { return subject; }
    public Teacher getTeacher() { return teacher; }
    public StudentGroup getGroup() { return group; }
    public Room getRoom() { return room; }
    public int getDurationHours() { return durationHours; }

    /** First hour (0-based 24 h) this entry occupies, derived from the time string. */
    public int getStartHour() {
        String time = slot.getTime(); // "HH:mm - HH:mm"
        try { return Integer.parseInt(time.substring(0, 2)); }
        catch (NumberFormatException e) { return 0; }
    }

    /** Start minute this entry occupies, derived from the time string. */
    public int getStartMinute() {
        String time = slot.getTime(); // "HH:mm - HH:mm"
        try { return Integer.parseInt(time.substring(3, 5)); }
        catch (NumberFormatException e) { return 0; }
    }
}
