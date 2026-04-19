package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StudentGroup implements Serializable {
    private String name;
    private int size;
    private final List<Subject> subjects;

    public StudentGroup(String name, int size) {
        this.name = name;
        this.size = size;
        this.subjects = new ArrayList<>();
    }

    public String getName() { return name; }
    public int getSize() { return size; }
    public List<Subject> getSubjects() { return subjects; }

    public void setName(String name) { this.name = name; }
    public void setSize(int size) { this.size = size; }
    public void addSubject(Subject s) { subjects.add(s); }
    public void removeSubject(Subject s) { subjects.remove(s); }

    @Override
    public String toString() { return name + " (" + size + " students)"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentGroup)) return false;
        return Objects.equals(name, ((StudentGroup) o).name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
