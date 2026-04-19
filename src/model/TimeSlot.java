package model;

import java.io.Serializable;
import java.util.Objects;

public class TimeSlot implements Serializable {
    private final String day;
    private final String time;

    public TimeSlot(String day, String time) {
        this.day = day;
        this.time = time;
    }

    public String getDay() { return day; }
    public String getTime() { return time; }

    @Override
    public String toString() { return day + " " + time; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot t = (TimeSlot) o;
        return Objects.equals(day, t.day) && Objects.equals(time, t.time);
    }

    @Override
    public int hashCode() { return Objects.hash(day, time); }
}
