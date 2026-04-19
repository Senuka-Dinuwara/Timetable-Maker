package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an individual student who belongs to a StudentGroup.
 * Mandatory subjects come from the group; selective subjects are the
 * electives this student has personally enrolled in.
 */
public class Student implements Serializable {
    private String name;
    private StudentGroup group;
    private final List<Subject> electiveSubjects;

    public Student(String name, StudentGroup group) {
        this.name = name;
        this.group = group;
        this.electiveSubjects = new ArrayList<>();
    }

    public String getName() { return name; }
    public StudentGroup getGroup() { return group; }
    public List<Subject> getElectiveSubjects() { return electiveSubjects; }

    public void setName(String name) { this.name = name; }
    public void setGroup(StudentGroup group) { this.group = group; }
    public void addElective(Subject s) { if (!electiveSubjects.contains(s)) electiveSubjects.add(s); }
    public void removeElective(Subject s) { electiveSubjects.remove(s); }

    /** All subjects this student attends: mandatory ones from group + personal electives. */
    public List<Subject> allSubjects() {
        List<Subject> all = new ArrayList<>();
        if (group != null) {
            for (Subject s : group.getSubjects()) {
                if (s.isMandatory()) all.add(s);
            }
        }
        for (Subject s : electiveSubjects) {
            if (!all.contains(s)) all.add(s);
        }
        return all;
    }

    @Override public String toString() { return name + (group != null ? " [" + group.getName() + "]" : ""); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student s = (Student) o;
        return Objects.equals(name, s.name) && Objects.equals(group, s.group);
    }

    @Override public int hashCode() { return Objects.hash(name, group); }
}
