package model;

import java.io.Serializable;
import java.util.Objects;

public class Subject implements Serializable {
    private String name;
    private int weeklyHours;
    private String type; // Lec, Tut, Lab, Theory, Practical  (semicolon-separated when multiple)
    private String preferredDay;   // null = no day constraint
    private boolean preferredDayOnly; // true = hard day lock, false = normal scheduling
    private boolean allGroups;     // true = all groups share the same slot
    private int sessionDuration;   // hours per session (1, 2, 3…)
    private boolean mandatory;     // true = all students in group attend; false = elective
    private int priority;           // 1 (low) to 5 (high), default 3

    public Subject(String name, int weeklyHours, String type) {
        this(name, weeklyHours, type, null, false, 1, true, 3);
    }

    public Subject(String name, int weeklyHours, String type, String preferredDay, boolean allGroups) {
        this(name, weeklyHours, type, preferredDay, allGroups, 1, true, 3);
    }

    public Subject(String name, int weeklyHours, String type,
                   String preferredDay, boolean allGroups, int sessionDuration) {
        this(name, weeklyHours, type, preferredDay, allGroups, sessionDuration, true, 3);
    }

    public Subject(String name, int weeklyHours, String type,
                   String preferredDay, boolean allGroups, int sessionDuration, boolean mandatory) {
        this(name, weeklyHours, type, preferredDay, allGroups, sessionDuration, mandatory, 3);
    }

    public Subject(String name, int weeklyHours, String type,
                   String preferredDay, boolean allGroups, int sessionDuration, boolean mandatory, int priority) {
        this(name, weeklyHours, type, preferredDay, false, allGroups, sessionDuration, mandatory, priority);
    }

    public Subject(String name, int weeklyHours, String type,
                   String preferredDay, boolean preferredDayOnly,
                   boolean allGroups, int sessionDuration, boolean mandatory, int priority) {
        this.name = name;
        this.weeklyHours = weeklyHours;
        this.type = type;
        this.preferredDay = (preferredDay == null || preferredDay.isBlank()) ? null : preferredDay;
        this.preferredDayOnly = preferredDayOnly;
        this.allGroups = allGroups;
        this.sessionDuration = (sessionDuration < 1) ? 1 : sessionDuration;
        this.mandatory = mandatory;
        this.priority = (priority < 1) ? 1 : (priority > 5) ? 5 : priority;
    }

    public String getName() { return name; }
    public int getWeeklyHours() { return weeklyHours; }
    public String getType() { return type; }
    public String getPreferredDay() { return preferredDay; }
    public boolean isPreferredDayOnly() { return preferredDayOnly; }
    public boolean isAllGroups() { return allGroups; }
    public int getSessionDuration() { return sessionDuration; }
    public boolean isMandatory() { return mandatory; }

    public void setName(String name) { this.name = name; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; }
    public void setType(String type) { this.type = type; }
    public void setPreferredDay(String preferredDay) {
        this.preferredDay = (preferredDay == null || preferredDay.isBlank()) ? null : preferredDay;
    }
    public void setPreferredDayOnly(boolean preferredDayOnly) { this.preferredDayOnly = preferredDayOnly; }
    public void setAllGroups(boolean allGroups) { this.allGroups = allGroups; }
    public void setSessionDuration(int d) { this.sessionDuration = (d < 1) ? 1 : d; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public int getPriority() { return priority; }
    public void setPriority(int p) { this.priority = (p < 1) ? 1 : (p > 5) ? 5 : p; }

    @Override
    public String toString() { return name + " (" + type + ", " + weeklyHours + "h)"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject)) return false;
        Subject s = (Subject) o;
        return Objects.equals(name, s.name) && Objects.equals(type, s.type);
    }

    @Override
    public int hashCode() { return Objects.hash(name, type); }
}
