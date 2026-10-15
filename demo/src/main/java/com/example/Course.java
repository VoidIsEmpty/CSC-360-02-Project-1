package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course information returned by Eastern's registration system.
 *
 * @param id the course's internal identifier
 * @param subject the course subject
 * @param courseNumber the course number
 * @param meetingsFaculty meeting times and any meeting-specific faculty
 * @param sequenceNumber the section sequence number
 * @param courseTitle the course title
 * @param creditHourLow the low credit-hour value, when supplied
 * @param creditHours the credit-hour value, when supplied
 * @param seatsAvailable the number of seats currently available
 * @param openSection whether the section is marked open by the source system
 * @param courseReferenceNumber the course reference number (CRN)
 * @param faculty the faculty associated with the course
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
        String id,
        String subject,
        String courseNumber,
        List<MeetingFaculty> meetingsFaculty,
        @JsonAlias("section") String sequenceNumber,
        String courseTitle,
        Integer creditHourLow,
        Integer creditHours,
        Integer seatsAvailable,
        Boolean openSection,
        String courseReferenceNumber,
        List<Faculty> faculty) {

    /**
     * Creates a course using the original core course fields.
     *
     * @param id the course's internal identifier
     * @param subject the course subject
     * @param courseNumber the course number
     * @param meetingsFaculty meeting times and any meeting-specific faculty
     */
    public Course(String id, String subject, String courseNumber, List<MeetingFaculty> meetingsFaculty) {
        this(id, subject, courseNumber, meetingsFaculty, null, null, null, null, null, null, null, null);
    }

    /**
     * Meeting-specific information for a course.
     *
     * @param meetingTime the meeting days and times
     * @param faculty faculty associated with this meeting
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime, List<Faculty> faculty) {

        /**
         * Creates meeting information without meeting-specific faculty.
         *
         * @param meetingTime the meeting days and times
         */
        public MeetingFaculty(MeetingTime meetingTime) {
            this(meetingTime, null);
        }

        /**
         * Returns the meeting-time summary.
         *
         * @return the meeting-time summary, or an empty string when no time exists
         */
        @Override
        public String toString() {
            return meetingTime == null ? "" : meetingTime.toString();
        }
    }

    /**
     * A faculty member associated with a course or meeting.
     *
     * @param displayName the faculty member's display name
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {
    }

    /**
     * A course meeting's days and times.
     *
     * @param beginTime meeting start time in Eastern's HHmm format
     * @param endTime meeting end time in Eastern's HHmm format
     * @param monday true when the meeting occurs on Monday
     * @param tuesday true when the meeting occurs on Tuesday
     * @param wednesday true when the meeting occurs on Wednesday
     * @param thursday true when the meeting occurs on Thursday
     * @param friday true when the meeting occurs on Friday
     * @param saturday true when the meeting occurs on Saturday
     * @param sunday true when the meeting occurs on Sunday
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
            String beginTime,
            String endTime,
            boolean monday,
            boolean tuesday,
            boolean wednesday,
            boolean thursday,
            boolean friday,
            boolean saturday,
            boolean sunday) {

        /**
         * Creates meeting days without known start or end times.
         *
         * @param monday true when the meeting occurs on Monday
         * @param tuesday true when the meeting occurs on Tuesday
         * @param wednesday true when the meeting occurs on Wednesday
         * @param thursday true when the meeting occurs on Thursday
         * @param friday true when the meeting occurs on Friday
         * @param saturday true when the meeting occurs on Saturday
         * @param sunday true when the meeting occurs on Sunday
         */
        public MeetingTime(
                boolean monday,
                boolean tuesday,
                boolean wednesday,
                boolean thursday,
                boolean friday,
                boolean saturday,
                boolean sunday) {
            this(null, null, monday, tuesday, wednesday, thursday, friday, saturday, sunday);
        }

        /**
         * Returns the ordered day abbreviations for this meeting.
         *
         * @return meeting days, such as {@code MWF}, or {@code TBA}
         */
        public String formatDays() {
            StringJoiner days = new StringJoiner("");
            if (monday) {
                days.add("M");
            }
            if (tuesday) {
                days.add("T");
            }
            if (wednesday) {
                days.add("W");
            }
            if (thursday) {
                days.add("Th");
            }
            if (friday) {
                days.add("F");
            }
            if (saturday) {
                days.add("Sa");
            }
            if (sunday) {
                days.add("Su");
            }
            String formattedDays = days.toString();
            return formattedDays.isEmpty() ? "TBA" : formattedDays;
        }

        /**
         * Determines whether this meeting occurs on the requested day.
         *
         * @param day full day name or its common abbreviation
         * @return true when the meeting occurs on the requested day
         */
        public boolean occursOnDay(String day) {
            if (day == null || day.isBlank()) {
                return false;
            }
            return switch (day.trim().toLowerCase(Locale.ROOT)) {
                case "m", "mon", "monday" -> monday;
                case "t", "tu", "tue", "tues", "tuesday" -> tuesday;
                case "w", "wed", "wednesday" -> wednesday;
                case "th", "r", "thu", "thur", "thurs", "thursday" -> thursday;
                case "f", "fri", "friday" -> friday;
                case "sa", "sat", "saturday" -> saturday;
                case "su", "sun", "sunday" -> sunday;
                default -> false;
            };
        }

        /**
         * Returns the formatted meeting days and times.
         *
         * @return a string such as {@code MWF 11:00-11:50}
         */
        @Override
        public String toString() {
            return formatDays() + " " + formatTime(beginTime) + "-" + formatTime(endTime);
        }
    }

    /**
     * Returns the best available credit-hour value.
     *
     * @return the low credit-hour value, falling back to credit hours or zero
     */
    public int getCredits() {
        if (creditHourLow != null) {
            return creditHourLow;
        }
        return creditHours == null ? 0 : creditHours;
    }

    /**
     * Returns the faculty display names, including names attached to meetings.
     *
     * @return all distinct non-empty instructor names
     */
    public List<String> getInstructorNames() {
        List<String> names = new ArrayList<>();
        addFacultyNames(names, faculty);
        if (meetingsFaculty != null) {
            for (MeetingFaculty meeting : meetingsFaculty) {
                if (meeting != null) {
                    addFacultyNames(names, meeting.faculty());
                }
            }
        }
        return names;
    }

    /**
     * Determines whether any meeting occurs on the requested day.
     *
     * @param day full day name or its common abbreviation
     * @return true when a meeting occurs on the requested day
     */
    public boolean hasMeetingOnDay(String day) {
        if (meetingsFaculty == null) {
            return false;
        }
        for (MeetingFaculty meeting : meetingsFaculty) {
            if (meeting != null && meeting.meetingTime() != null
                    && meeting.meetingTime().occursOnDay(day)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats the course with its section, credits, title, and meeting schedule.
     *
     * @return the formatted course summary
     */
    public String formatCourse() {
        StringJoiner days = new StringJoiner("; ");
        StringJoiner times = new StringJoiner("; ");
        if (meetingsFaculty != null) {
            for (MeetingFaculty meeting : meetingsFaculty) {
                if (meeting != null && meeting.meetingTime() != null) {
                    days.add(meeting.meetingTime().formatDays());
                    times.add(formatTime(meeting.meetingTime().beginTime())
                            + "-" + formatTime(meeting.meetingTime().endTime()));
                }
            }
        }
        if (days.length() == 0) {
            days.add("TBA");
            times.add("TBA-TBA");
        }
        String section = sequenceNumber == null || sequenceNumber.isBlank() ? "?" : sequenceNumber;
        String title = courseTitle == null ? "" : courseTitle;
        return String.format(
                "%s %s-%s, %d credits, %s, %s, %s",
                valueOrEmpty(subject),
                valueOrEmpty(courseNumber),
                section,
                getCredits(),
                title,
                days,
                times);
    }

    /**
     * Returns a formatted course summary.
     *
     * @return the formatted course summary
     */
    @Override
    public String toString() {
        return formatCourse();
    }

    /**
     * Adds non-empty faculty names to a result list without duplicates.
     *
     * @param names the destination list
     * @param facultyMembers faculty entries to inspect
     */
    private static void addFacultyNames(List<String> names, List<Faculty> facultyMembers) {
        if (facultyMembers == null) {
            return;
        }
        for (Faculty facultyMember : facultyMembers) {
            if (facultyMember != null && facultyMember.displayName() != null
                    && !facultyMember.displayName().isBlank()
                    && !names.contains(facultyMember.displayName())) {
                names.add(facultyMember.displayName());
            }
        }
    }

    /**
     * Formats Eastern's compact time representation for display.
     *
     * @param time the source time
     * @return the formatted time, or {@code TBA} when absent
     */
    private static String formatTime(String time) {
        if (time == null || time.isBlank()) {
            return "TBA";
        }
        String trimmedTime = time.trim();
        if (trimmedTime.matches("\\d{4}")) {
            return trimmedTime.substring(0, 2) + ":" + trimmedTime.substring(2);
        }
        return trimmedTime;
    }

    /**
     * Replaces a null display value with an empty string.
     *
     * @param value the value to inspect
     * @return the original value or an empty string
     */
    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
