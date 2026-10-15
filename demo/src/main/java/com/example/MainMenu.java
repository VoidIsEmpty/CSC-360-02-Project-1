package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Console user interface for the Eastern Registration System.
 */
public class MainMenu {

    private final Scanner scanner;
    private final EasternSchedule schedule;
    private Student student;

    /**
     * Creates a menu using standard input and the default Eastern schedule.
     */
    public MainMenu() {
        scanner = new Scanner(System.in);
        schedule = new EasternSchedule();
    }

    /**
     * Opens the interactive registration menu.
     *
     * @throws Exception if course or term data cannot be retrieved
     */
    public void open() throws Exception {
        createStudent();
        selectTerm();
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("Eastern Registration System");
            System.out.println("1. Display all courses for a subject");
            System.out.println("2. Search by subject and course number");
            System.out.println("3. Add or drop classes");
            System.out.println("4. Display my schedule");
            System.out.println("5. Exit");
            switch (readLine("Choose an option: ").trim()) {
                case "1" -> displayCoursesBySubject();
                case "2" -> displayCoursesWithCriteria();
                case "3" -> manageRegistration();
                case "4" -> displayStudentSchedule();
                case "5" -> running = false;
                default -> System.out.println("Please enter an option from 1 to 5.");
            }
        }
        System.out.println("Goodbye, " + student.getFirstName() + ".");
    }

    /**
     * Prompts for and creates the current student profile.
     */
    private void createStudent() {
        System.out.println("Create your student profile.");
        String id = readLine("Student ID: ").trim();
        String firstName = readLine("First name: ").trim();
        String lastName = readLine("Last name: ").trim();
        student = new Student(id, firstName, lastName);
    }

    /**
     * Displays available terms and optionally updates the selected term.
     *
     * @throws Exception if available term data cannot be retrieved
     */
    private void selectTerm() throws Exception {
        List<Term> availableTerms = schedule.getAvailableTerms();
        System.out.println("Available terms (current default: " + schedule.getTerm() + "):");
        for (int index = 0; index < availableTerms.size(); index++) {
            Term term = availableTerms.get(index);
            System.out.printf("%d. %s - %s%n", index + 1, term.code(), term.description());
        }

        String selection = readLine(
                "Enter a term code or list number, or press Enter to use " + schedule.getTerm() + ": ")
                .trim();
        if (selection.isEmpty()) {
            return;
        }
        for (int index = 0; index < availableTerms.size(); index++) {
            Term term = availableTerms.get(index);
            if (selection.equalsIgnoreCase(term.code())
                    || selection.equals(Integer.toString(index + 1))) {
                schedule.setTerm(term.code());
                System.out.println("Selected term: " + term.description());
                return;
            }
        }
        System.out.println("That term is not in the available list; using " + schedule.getTerm() + ".");
    }

    /**
     * Retrieves and displays every course in a requested subject.
     *
     * @throws Exception if the course data cannot be retrieved
     */
    private void displayCoursesBySubject() throws Exception {
        String subject = readLine("Subject (for example, CSC): ").trim().toUpperCase(Locale.ROOT);
        List<Course> courses = schedule.queryCourseBySubject(subject);
        displayCourses(courses);
    }

    /**
     * Retrieves and displays courses matching subject, number, and optional filters.
     *
     * @throws Exception if the course data cannot be retrieved
     */
    private void displayCoursesWithCriteria() throws Exception {
        String subject = readLine("Subject (for example, CSC): ").trim();
        String courseNumber = readLine("Course number (for example, 360): ").trim();
        boolean onlyOpen = readYesNo("Only show courses with open seats? (y/n): ");
        String instructor = readLine("Instructor name contains (press Enter to skip): ").trim();
        String excludeDay = readLine("Exclude day (for example, Tuesday or T; Enter to skip): ").trim();
        List<Course> courses = schedule.filterCourses(
                subject,
                courseNumber,
                onlyOpen,
                instructor,
                excludeDay);
        displayCourses(courses);
    }

    /**
     * Runs the add, drop, and clear registration submenu.
     *
     * @throws Exception if the course data cannot be retrieved
     */
    private void manageRegistration() throws Exception {
        boolean managing = true;
        while (managing) {
            System.out.println();
            System.out.println("1. Add one or more courses");
            System.out.println("2. Drop a course");
            System.out.println("3. Drop all courses");
            System.out.println("4. Return to main menu");
            switch (readLine("Choose an option: ").trim()) {
                case "1" -> addCourses();
                case "2" -> dropCourse();
                case "3" -> {
                    student.dropAllCourses();
                    System.out.println("All courses have been dropped.");
                }
                case "4" -> managing = false;
                default -> System.out.println("Please enter an option from 1 to 4.");
            }
        }
    }

    /**
     * Adds courses selected by CRN, internal ID, or section number.
     *
     * @throws Exception if the course data cannot be retrieved
     */
    private void addCourses() throws Exception {
        String subject = readLine("Subject to search: ").trim().toUpperCase(Locale.ROOT);
        List<Course> courses = schedule.queryCourseBySubject(subject);
        displayCourses(courses);
        if (courses.isEmpty()) {
            return;
        }
        System.out.println("Enter one or more CRNs, course IDs, or section numbers separated by commas.");
        String[] identifiers = readLine("Course identifiers: ").split(",");
        for (String identifier : identifiers) {
            String key = identifier.trim();
            if (key.isEmpty()) {
                continue;
            }
            List<Course> matches = findByIdentifier(courses, key);
            if (matches.isEmpty()) {
                System.out.println("No course matched identifier " + key + ".");
            } else if (matches.size() > 1) {
                System.out.println("Section " + key
                        + " matches multiple courses; use a CRN or course ID instead.");
            } else {
                student.addCourse(matches.get(0));
            }
        }
    }

    /**
     * Drops a registered course selected by CRN, internal ID, or section number.
     */
    private void dropCourse() {
        displayStudentSchedule();
        String identifier = readLine("CRN, course ID, or section number to drop: ").trim();
        List<Course> matches = findByIdentifier(student.getRegisteredCourses(), identifier);
        if (matches.size() > 1) {
            System.out.println("That section matches multiple courses; enter a CRN or course ID.");
        } else if (matches.isEmpty()) {
            System.out.println("No registered course matched that identifier.");
        } else {
            student.dropCourse(matches.get(0));
        }
    }

    /**
     * Displays the student's registered courses and total credits.
     */
    private void displayStudentSchedule() {
        System.out.println();
        System.out.printf(
                "Schedule for %s %s (%s) - %d credits%n",
                student.getFirstName(),
                student.getLastName(),
                student.getId(),
                student.getTotalCredits());
        displayCourses(student.getRegisteredCourses());
    }

    /**
     * Displays course summaries and relevant registration identifiers.
     *
     * @param courses the courses to display
     */
    private static void displayCourses(List<Course> courses) {
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }
        for (Course course : courses) {
            String crn = course.courseReferenceNumber() == null ? "N/A" : course.courseReferenceNumber();
            String seats = course.seatsAvailable() == null ? "unknown" : course.seatsAvailable().toString();
            System.out.printf("%s [CRN: %s, seats available: %s]%n", course.formatCourse(), crn, seats);
        }
    }

    /**
     * Finds courses matching a CRN, internal identifier, or section number.
     *
     * @param courses the courses to search
     * @param identifier the requested identifier
     * @return every matching course
     */
    private static List<Course> findByIdentifier(List<Course> courses, String identifier) {
        List<Course> matches = new ArrayList<>();
        String key = identifier.trim();
        for (Course course : courses) {
            if (matchesIdentifier(course.courseReferenceNumber(), key)
                    || matchesIdentifier(course.id(), key)) {
                matches.add(course);
            }
        }
        if (matches.isEmpty()) {
            for (Course course : courses) {
                if (matchesIdentifier(course.sequenceNumber(), key)) {
                    matches.add(course);
                }
            }
        }
        return matches;
    }

    /**
     * Compares a non-null source identifier with a requested value.
     *
     * @param value the source identifier
     * @param identifier the requested identifier
     * @return true when the identifiers are equal without regard to case
     */
    private static boolean matchesIdentifier(String value, String identifier) {
        return value != null && value.equalsIgnoreCase(identifier);
    }

    /**
     * Prompts until the user enters a yes or no response.
     *
     * @param prompt the question to display
     * @return true for an affirmative response
     */
    private boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt).trim().toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please enter y or n.");
        }
    }

    /**
     * Displays a prompt and reads one line from the console.
     *
     * @param prompt the prompt to display
     * @return the line entered by the user
     */
    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}
