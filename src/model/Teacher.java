package model;

import java.io.Serializable;
import java.util.*;

public class Teacher implements Serializable {
    private String name;
    private final List<Subject> subjects;
    /** Days on which this teacher is entirely unavailable (e.g. "Monday"). */
    private final Set<String> blockedDays;

    public Teacher(String name) {
        this.name = name;
        this.subjects = new ArrayList<>();
        this.blockedDays = new LinkedHashSet<>();
    }

    public String getName() { return name; }
    public List<Subject> getSubjects() { return subjects; }
    public Set<String> getBlockedDays() { return blockedDays; }

    public void setName(String name) { this.name = name; }
    public void addSubject(Subject s) { subjects.add(s); }
    public void removeSubject(Subject s) { subjects.remove(s); }

    /** Block an entire day (teacher unavailable all day). */
    public void blockDay(String day) { blockedDays.add(day); }
    public void unblockDay(String day) { blockedDays.remove(day); }
    public void clearBlockedDays() { blockedDays.clear(); }

    /** Returns false if the given day is blocked for this teacher. */
    public boolean isAvailable(String day) { return !blockedDays.contains(day); }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher)) return false;
        return Objects.equals(name, ((Teacher) o).name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
