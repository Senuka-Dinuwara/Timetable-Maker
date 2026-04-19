# Timetable Maker

A desktop application for generating conflict-free academic timetables, built with Java Swing. Designed for schools, colleges, and universities to automate the complex process of scheduling classes while respecting teacher availability, room capacity, student groups, and a wide range of configurable constraints.

---

## Features

### Scheduling Engine
- **Greedy conflict-free algorithm** — automatically resolves teacher, room, and group overlaps
- **Multi-hour sessions** — supports configurable session durations (e.g., 2-hour lab blocks displayed as "08:00 - 10:00")
- **Priority-based scheduling** — higher priority subjects are placed first for better slot access
- **Preferred day constraints** — soft preference (include on a day, allow others) or hard lock (only locked subjects on that day)
- **Shared sessions** — subjects marked as "all groups" are scheduled once with all relevant groups attending together
- **Rotational weeks** — generate multiple weekly schedules for rotation-based timetables
- **Automatic break insertion** — configurable break/interval slots between sessions
- **Day limits** — restrict the maximum number of school days per group per week
- **Detailed generation log** — diagnostic messages with specific failure reasons when subjects cannot be placed

### Data Management
- **Subjects** — name, weekly hours, type (Lecture/Lab/Tutorial/Practical/Theory), preferred day, session duration, priority, mandatory/elective flag
- **Teachers** — name, assigned subjects, blocked days for availability control
- **Students** — individual records with group membership and elective subject selections
- **Student Groups** — class cohorts with size and assigned subjects
- **Rooms** — ID, capacity, type, optional assignment to specific subjects or groups, blocked time slots
- **CSV Import** — bulk data import for all entity types with error reporting
- **Project Files** — save and load complete project state (`.tmt` format)

### Timetable Views
- **Three view modes** — view by Class/Group, by Teacher, or by Room
- **Week selector** — switch between rotational weeks
- **Search & filter** — filter the timetable by subject, teacher, room, or group
- **HTML export** — download the generated timetable as an HTML file

### User Interface
- Clean, modern UI with a custom theme and sidebar navigation
- Responsive layout with scrollable panels
- CRUD operations for all entities (Subjects, Teachers, Students, Groups, Rooms)
- Customizable subject types and room types via the Options panel

---

## Screenshots

> _Add screenshots of the application here._

---

## Getting Started

### Prerequisites

- **Java 11** or higher (JDK)

### Build & Run

```bash
# Clone the repository
git clone https://github.com/your-username/timetable-maker.git
cd timetable-maker

# Compile
javac -d out -sourcepath src src/Main.java

# Run
java -cp out Main
```

---

## Usage

### 1. Define Your Data

Use the sidebar to navigate between panels and enter your data:

| Panel        | Purpose                                                  |
|--------------|----------------------------------------------------------|
| **Subjects** | Add courses with hours, type, priority, preferred day    |
| **Teachers** | Add teachers and assign subjects, set blocked days       |
| **Students** | Add individual students with group and elective choices  |
| **Groups**   | Define student groups (classes) with size and subjects    |
| **Rooms**    | Add rooms with capacity, type, and optional assignments  |
| **Options**  | Customize subject types, room types, and assignment modes|

Alternatively, use the **Import** panel to bulk-import data from CSV files.

### 2. Configure Generation Settings

In the **Generate** panel:

- Select working days (Mon–Sat)
- Set daily time range (start/end hours)
- Configure break times and durations
- Set day limits per group
- Choose the number of rotational weeks

### 3. Generate & View

Click **Generate** to run the scheduler. Results appear in the **Timetable View** panel where you can:

- Switch between Group, Teacher, and Room views
- Navigate rotational weeks
- Search and filter entries
- Export to HTML

---

## CSV Import Format

Import data in bulk using CSV files. The first row is always treated as a header and skipped. Lines starting with `#` are ignored.

**Subjects**
```
Name, WeeklyHours, Type [, PreferredDay [, AllGroups [, SessionDuration]]]
```

**Teachers**
```
Name, Subjects (semicolon-separated)
```

**Rooms**
```
ID, Capacity, Type
```

**Groups**
```
Name, Size, Subjects (semicolon-separated)
```

**Students**
```
Name, GroupName, Electives (semicolon-separated)
```

> **Note:** Import Subjects first, as Teachers and Groups reference subject names.

---

## Project Structure

```
src/
├── Main.java                  # Application entry point
├── model/
│   ├── Room.java              # Room entity
│   ├── ScheduleEntry.java     # Single scheduled class
│   ├── Student.java           # Individual student
│   ├── StudentGroup.java      # Class/cohort
│   ├── Subject.java           # Course/module
│   ├── Teacher.java           # Faculty member
│   └── TimeSlot.java          # Day + time pair
├── service/
│   └── Scheduler.java         # Scheduling engine
├── ui/
│   ├── AppTheme.java          # Centralized theme
│   ├── GeneratePanel.java     # Generation settings UI
│   ├── GroupPanel.java        # Group management
│   ├── ImportPanel.java       # CSV import UI
│   ├── MainFrame.java         # Main window + sidebar
│   ├── OptionPanel.java       # Type customization
│   ├── RoomPanel.java         # Room management
│   ├── SettingsPanel.java     # Settings UI
│   ├── StudentPanel.java      # Student management
│   ├── SubjectPanel.java      # Subject management
│   ├── TeacherPanel.java      # Teacher management
│   └── TimetableViewPanel.java# Timetable display + export
└── util/
    ├── CsvImporter.java       # CSV parsing
    └── DataStore.java         # Central data repository
```

---

## License

This project is provided as-is for educational and institutional use.
