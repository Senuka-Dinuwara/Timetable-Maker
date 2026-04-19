package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Room implements Serializable {
    private String id;
    private int capacity;
    private String type; // Lecture, Lab, Classroom
    // Optional assignment targets based on room-type mode.
    private String assignedGroup;
    private String assignedSubject;
    private List<String> blockedSlots = new ArrayList<>(); // e.g. "Monday 08:00 - 10:00"

    public Room(String id, int capacity, String type) {
        this.id = id;
        this.capacity = capacity;
        this.type = type;
    }

    public String getId() { return id; }
    public int getCapacity() { return capacity; }
    public String getType() { return type; }
    public String getAssignedGroup() { return assignedGroup; }
    public String getAssignedSubject() { return assignedSubject; }
    public List<String> getBlockedSlots() { return blockedSlots; }

    public void setId(String id) { this.id = id; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setType(String type) { this.type = type; }
    public void setAssignedGroup(String assignedGroup) { this.assignedGroup = assignedGroup; }
    public void setAssignedSubject(String assignedSubject) { this.assignedSubject = assignedSubject; }
    public void setBlockedSlots(List<String> blockedSlots) { this.blockedSlots = blockedSlots != null ? blockedSlots : new ArrayList<>(); }

    public boolean isBlockedAt(String day, int startHour, int dur) {
        // Each entry: "Day HH:mm - HH:mm"
        int schedStartMins = startHour * 60;
        int schedEndMins   = schedStartMins + dur * 60;
        for (String entry : blockedSlots) {
            int sp = entry.indexOf(' ');
            if (sp < 0) continue;
            String entryDay = entry.substring(0, sp);
            if (!entryDay.equals(day)) continue;
            String times = entry.substring(sp + 1); // "HH:mm - HH:mm"
            String[] parts = times.split("-");
            if (parts.length < 2) continue;
            try {
                String[] s = parts[0].trim().split(":");
                String[] e = parts[1].trim().split(":");
                int rs = Integer.parseInt(s[0]) * 60 + (s.length > 1 ? Integer.parseInt(s[1]) : 0);
                int re = Integer.parseInt(e[0]) * 60 + (e.length > 1 ? Integer.parseInt(e[1]) : 0);
                if (schedStartMins < re && schedEndMins > rs) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    @Override
    public String toString() { return id + " (" + type + ", cap:" + capacity + ")"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        return Objects.equals(id, ((Room) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
