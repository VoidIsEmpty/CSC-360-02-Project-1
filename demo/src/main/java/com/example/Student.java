package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A student and the courses currently registered by that student.
 */
public class Student {

    /** The maximum number of credits a student may register for. */
    public static final int MAX_CREDITS = 17;

    private String id;
    private String firstName;
    private String lastName;
    private List<Course> registeredCourses;

    /**
     * Creates an empty student for Jackson deserialization.
     */
    public Student() {
        registeredCourses = new ArrayList<>();
    }

    /**
     * Creates a student with the supplied identity.
     *
     * @param id the student's identifier
     * @param firstName the student's first name
     * @param lastName the student's last name
     */
    public Student(String id, String firstName, String lastName) {
        this();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Attempts to add a course after checking registration constraints.
     *
     * @param course the course to add
     * @return true if the course was added
     */
    public boolean addCourse(Course course) {
        if (course == null) {
            System.out.println("Course was not added: no course was selected.");
            return false;
        }
        if (registeredCourses.stream().anyMatch(existing -> isSameCourse(existing, course))) {
            System.out.println("Course was not added: this course is already registered.");
            return false;
        }
        if (course.seatsAvailable() != null && course.seatsAvailable() <= 0) {
            System.out.println("Course was not added: the section is full.");
            return false;
        }
        if (getTotalCredits() + course.getCredits() > MAX_CREDITS) {
            System.out.printf(
                    "Course was not added: registration would exceed the %d-credit limit.%n",
                    MAX_CREDITS);
            return false;
        }
        if (hasTimeConflict(course)) {
            System.out.println("Course was not added: its meeting time conflicts with your schedule.");
            return false;
        }
        registeredCourses.add(course);
        System.out.println("Course added: " + course.formatCourse());
        return true;
    }

    /**
     * Drops a registered course.
     *
     * @param course the course to drop
     * @return true if a matching course was removed
     */
    public boolean dropCourse(Course course) {
        if (course == null) {
            System.out.println("Course was not dropped: no course was selected.");
            return false;
        }
        boolean removed = registeredCourses.removeIf(existing -> isSameCourse(existing, course));
        if (!removed) {
            System.out.println("Course was not dropped: it is not in your schedule.");
        }
        return removed;
    }

    /**
     * Drops every course from the student's schedule.
     */
    public void dropAllCourses() {
        registeredCourses.clear();
    }

    /**
     * Determines whether the supplied course overlaps a currently registered
     * course on any common day.
     *
     * @param newCourse the course to check
     * @return true if an overlapping meeting exists
     */
    public boolean hasTimeConflict(Course newCourse) {
        if (newCourse == null || newCourse.meetingsFaculty() == null) {
            return false;
        }
        for (Course registeredCourse : registeredCourses) {
            if (registeredCourse == null || registeredCourse.meetingsFaculty() == null) {
                continue;
            }
            for (Course.MeetingFaculty newMeeting : newCourse.meetingsFaculty()) {
                if (newMeeting == null || newMeeting.meetingTime() == null) {
                    continue;
                }
                for (Course.MeetingFaculty registeredMeeting : registeredCourse.meetingsFaculty()) {
                    if (registeredMeeting == null || registeredMeeting.meetingTime() == null) {
                        continue;
                    }
                    if (meetingsOverlap(newMeeting.meetingTime(), registeredMeeting.meetingTime())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Calculates the total number of credits currently registered.
     *
     * @return the sum of the registered courses' credits
     */
    public int getTotalCredits() {
        return registeredCourses.stream()
                .filter(Objects::nonNull)
                .mapToInt(Course::getCredits)
                .sum();
    }

    /**
     * Gets the student's identifier.
     *
     * @return the student's identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the student's identifier.
     *
     * @param id the student's identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the student's first name.
     *
     * @return the student's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the student's first name.
     *
     * @param firstName the student's first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the student's last name.
     *
     * @return the student's last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the student's last name.
     *
     * @param lastName the student's last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the student's registered courses.
     *
     * @return the registered courses
     */
    public List<Course> getRegisteredCourses() {
        return registeredCourses;
    }

    /**
     * Replaces the student's registered courses.
     *
     * @param courses the courses to register, or null to clear the schedule
     */
    public void setRegisteredCourses(List<Course> courses) {
        registeredCourses = courses == null ? new ArrayList<>() : new ArrayList<>(courses);
    }

    /**
     * Compares courses using their CRN, internal identifier, or subject/section.
     *
     * @param first the first course
     * @param second the second course
     * @return true when both values identify the same course
     */
    private static boolean isSameCourse(Course first, Course second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }
        if (hasText(first.courseReferenceNumber()) && hasText(second.courseReferenceNumber())) {
            return first.courseReferenceNumber().equalsIgnoreCase(second.courseReferenceNumber());
        }
        if (hasText(first.id()) && hasText(second.id())) {
            return first.id().equalsIgnoreCase(second.id());
        }
        return Objects.equals(first.subject(), second.subject())
                && Objects.equals(first.courseNumber(), second.courseNumber())
                && Objects.equals(first.sequenceNumber(), second.sequenceNumber());
    }

    /**
     * Checks whether two meetings share a meeting day and overlap in time.
     *
     * @param first the first meeting
     * @param second the second meeting
     * @return true if the meetings overlap
     */
    private static boolean meetingsOverlap(Course.MeetingTime first, Course.MeetingTime second) {
        if (!haveCommonDay(first, second)) {
            return false;
        }
        LocalTime firstStart = parseTime(first.beginTime());
        LocalTime firstEnd = parseTime(first.endTime());
        LocalTime secondStart = parseTime(second.beginTime());
        LocalTime secondEnd = parseTime(second.endTime());
        return firstStart != null && firstEnd != null && secondStart != null && secondEnd != null
                && firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    /**
     * Checks whether two meetings occur on at least one common day.
     *
     * @param first the first meeting
     * @param second the second meeting
     * @return true when the meetings share a day
     */
    private static boolean haveCommonDay(Course.MeetingTime first, Course.MeetingTime second) {
        return (first.monday() && second.monday())
                || (first.tuesday() && second.tuesday())
                || (first.wednesday() && second.wednesday())
                || (first.thursday() && second.thursday())
                || (first.friday() && second.friday())
                || (first.saturday() && second.saturday())
                || (first.sunday() && second.sunday());
    }

    /**
     * Parses Eastern's compact, 24-hour, or 12-hour time representation.
     *
     * @param time the source time
     * @return the parsed time, or null when the value is missing or invalid
     */
    private static LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String normalizedTime = time.trim();
        try {
            if (normalizedTime.matches("\\d{3,4}")) {
                return LocalTime.parse(
                        String.format("%4s", normalizedTime).replace(' ', '0'),
                        DateTimeFormatter.ofPattern("HHmm"));
            }
            if (normalizedTime.matches("\\d{1,2}:\\d{2}")) {
                return LocalTime.parse(normalizedTime, DateTimeFormatter.ofPattern("H:mm"));
            }
            return LocalTime.parse(
                    normalizedTime.toUpperCase(Locale.ROOT),
                    DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /**
     * Determines whether a value contains non-whitespace text.
     *
     * @param value the value to inspect
     * @return true when the value is not null or blank
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
