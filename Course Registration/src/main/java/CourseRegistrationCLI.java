import java.util.*;

/**
 * UNIVERSITY COURSE REGISTRATION SYSTEM (MVP)
 * * CORE CONCEPTS IMPLEMENTED:
 * 1. Binary Search Tree (BST): Stores courses sorted by Course Code.
 * 2. Queue: Manages registration requests (FIFO).
 * 3. Hash Table: quick access to Student records via ID.
 * 4. Sorting: Dynamic sorting for display (Credits/Availability).
 */

public class CourseRegistrationCLI {

    // DATA MODELS

    static class Course implements Comparable<Course> {
        String code;
        String name;
        int credits;
        int capacity;
        int enrolled;

        public Course(String code, String name, int credits, int capacity) {
            this.code = code.toUpperCase();
            this.name = name;
            this.credits = credits;
            this.capacity = capacity;
            this.enrolled = 0;
        }

        public int getAvailableSeats() {
            return capacity - enrolled;
        }

        public boolean isFull() {
            return enrolled >= capacity;
        }

        @Override
        public String toString() {
            return String.format("| %-8s | %-45s | %2d Credits | Seats: %3d/%3d |",
                    code,
                    name.length() > 45 ? name.substring(0, 42) + "..." : name,
                    credits,
                    enrolled,
                    capacity
            );
        }

        // Default natural ordering by Course Code (for BST)
        @Override
        public int compareTo(Course other) {
            return this.code.compareTo(other.code);
        }
    }

    static class Student {
        String id;
        String name;
        List<String> registeredCourses;

        public Student(String id, String name) {
            this.id = id;
            this.name = name;
            this.registeredCourses = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - Courses: %s", id, name, registeredCourses);
        }
    }

    static class RegistrationRequest {
        String studentId;
        String courseCode;

        public RegistrationRequest(String studentId, String courseCode) {
            this.studentId = studentId;
            this.courseCode = courseCode;
        }
    }

    // CUSTOM DATA STRUCTURE: BINARY SEARCH TREE
    static class CourseBST {

        class Node {
            Course course;
            Node left, right;

            public Node(Course item) {
                course = item;
                left = right = null;
            }
        }

        Node root;

        public void insert(Course course) {
            root = insertRec(root, course);
        }

        private Node insertRec(Node root, Course course) {
            if (root == null) {
                root = new Node(course);
                return root;
            }
            if (course.compareTo(root.course) < 0)
                root.left = insertRec(root.left, course);
            else if (course.compareTo(root.course) > 0)
                root.right = insertRec(root.right, course);
            return root;
        }

        public Course search(String code) {
            return searchRec(root, code.toUpperCase());
        }

        private Course searchRec(Node root, String code) {
            if (root == null) return null;
            if (root.course.code.equals(code)) return root.course;

            if (code.compareTo(root.course.code) < 0)
                return searchRec(root.left, code);

            return searchRec(root.right, code);
        }

        // Helper to get all courses into a list for custom sorting
        public List<Course> toList() {
            List<Course> list = new ArrayList<>();
            inOrderRec(root, list);
            return list;
        }

        private void inOrderRec(Node root, List<Course> list) {
            if (root != null) {
                inOrderRec(root.left, list);
                list.add(root.course);
                inOrderRec(root.right, list);
            }
        }
    }

    // SYSTEM MANAGER
    static class RegistrationSystem {
        CourseBST courseTree;
        Map<String, Student> studentDatabase; // Hash Table
        Queue<RegistrationRequest> requestQueue; // Queue

        public RegistrationSystem() {
            courseTree = new CourseBST();
            studentDatabase = new HashMap<>();
            requestQueue = new LinkedList<>();
            seedData();
        }

        private void seedData() {
            // Seeding Courses (BST)
            courseTree.insert(new Course("CSC215", "Data Structures and Algorithms", 3, 40));
            courseTree.insert(new Course("COM202", "Business and Professional Speech", 3, 50));
            courseTree.insert(new Course("CSC211", "Computer Organization and Assembly Language", 3, 30));
            courseTree.insert(new Course("MTH204", "Linear Algebra", 3, 5)); // Low capacity to test queue
            courseTree.insert(new Course("REL101", "Islamic Studies", 3, 40));

            // Seeding Students (Hash Table)
            studentDatabase.put("20241-35751", new Student("20241-35751", "Muhammad Haseeb Haroon"));
            studentDatabase.put("20241-12345", new Student("20241-12345", "Abdul Samad"));
            studentDatabase.put("20241-67890", new Student("20241-67890", "Mohammad Arslan"));
        }

        public void addStudent(String id, String name) {
            if (studentDatabase.containsKey(id)) {
                System.out.println("Error: Student ID already exists.");
                return;
            }
            studentDatabase.put(id, new Student(id, name));
            System.out.println("Student added successfully.");
        }

        public void addCourse(String code, String name, int credits, int cap) {
            if (courseTree.search(code) != null) {
                System.out.println("Error: Course code already exists.");
                return;
            }
            courseTree.insert(new Course(code, name, credits, cap));
            System.out.println("Course added to BST.");
        }

        // Add a request to the Queue
        public void queueRegistration(String sId, String cCode) {
            if (!studentDatabase.containsKey(sId)) {
                System.out.println("Error: Student not found.");
                return;
            }
            if (courseTree.search(cCode) == null) {
                System.out.println("Error: Course not found.");
                return;
            }
            requestQueue.add(new RegistrationRequest(sId, cCode));
            System.out.println("Request added to processing queue. Position: " + requestQueue.size());
        }

        // Process the Queue (FIFO)
        public void processQueue() {
            if (requestQueue.isEmpty()) {
                System.out.println("No pending requests.");
                return;
            }

            System.out.println("\n--- Processing Queue ---");
            while (!requestQueue.isEmpty()) {
                RegistrationRequest req = requestQueue.poll();
                Student student = studentDatabase.get(req.studentId);
                Course course = courseTree.search(req.courseCode);

                System.out.print("Processing " + student.name + " for " + course.code + "... ");

                if (course.isFull()) {
                    System.out.println("FAILED. Course Full.");
                } else if (student.registeredCourses.contains(course.code)) {
                    System.out.println("FAILED. Already Enrolled.");
                } else {
                    course.enrolled++;
                    student.registeredCourses.add(course.code);
                    System.out.println("SUCCESS. Enrolled.");
                }
            }
            System.out.println("------------------------\n");
        }

        // Display Logic with Sorting
        public void displayCourses(int sortMode) {
            List<Course> courses = courseTree.toList();

            switch (sortMode) {
                case 1: // By Code (Default BST Order)
                    // Already sorted by BST traversal
                    break;
                case 2: // By Credits
                    courses.sort(Comparator.comparingInt(c -> c.credits));
                    break;
                case 3: // By Available Seats (High to Low)
                    courses.sort((c1, c2) -> Integer.compare(c2.getAvailableSeats(), c1.getAvailableSeats()));
                    break;
            }

            System.out.println("\n=== COURSE LIST ===");
            System.out.println("---------------------------------------------------------------------------------------------");
            for (Course c : courses) {
                System.out.println(c);
            }
            System.out.println("---------------------------------------------------------------------------------------------");
        }

        public void displayStudents() {
            System.out.println("\n=== STUDENT RECORDS ===");
            for (Student s : studentDatabase.values()) {
                System.out.println(s);
            }
        }
    }

    // MAIN CLI LOOP
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RegistrationSystem system = new RegistrationSystem();
        boolean running = true;

        System.out.println("Welcome to the University Registration System");

        while (running) {
            System.out.println("\nMENU:");
            System.out.println("1. List Courses (Default/BST Code Order)");
            System.out.println("2. List Courses (Sorted by Credits)");
            System.out.println("3. List Courses (Sorted by Availability)");
            System.out.println("4. Add New Course");
            System.out.println("5. Add New Student");
            System.out.println("6. Submit Registration Request (Add to Queue)");
            System.out.println("7. Process Registration Queue");
            System.out.println("8. View Students");
            System.out.println("9. Exit");
            System.out.print("Select Option: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        system.displayCourses(1);
                        break;
                    case "2":
                        system.displayCourses(2);
                        break;
                    case "3":
                        system.displayCourses(3);
                        break;
                    case "4":
                        System.out.print("Code: "); String code = scanner.nextLine();
                        System.out.print("Name: "); String name = scanner.nextLine();
                        System.out.print("Credits: "); int credits = Integer.parseInt(scanner.nextLine());
                        System.out.print("Capacity: "); int cap = Integer.parseInt(scanner.nextLine());
                        system.addCourse(code, name, credits, cap);
                        break;
                    case "5":
                        System.out.print("ID: "); String sid = scanner.nextLine();
                        System.out.print("Name: "); String sname = scanner.nextLine();
                        system.addStudent(sid, sname);
                        break;
                    case "6":
                        System.out.print("Student ID: "); String regSid = scanner.nextLine();
                        System.out.print("Course Code: "); String regCode = scanner.nextLine();
                        system.queueRegistration(regSid, regCode);
                        break;
                    case "7":
                        system.processQueue();
                        break;
                    case "8":
                        system.displayStudents();
                        break;
                    case "9":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error processing input. Please try again.");
            }
        }
        scanner.close();
        System.out.println("Request Processed...\nExited");
    }
}