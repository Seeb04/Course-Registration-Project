import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


public class CourseRegistrationGUI extends JFrame {


    //color palette
    static final Color BG_DARK = new Color(24, 24, 27);
    static final Color BG_SURFACE = new Color(39, 39, 42);

    // Text
    static final Color TEXT_PRIMARY = new Color(244, 244, 245);
    static final Color TEXT_SECONDARY = new Color(161, 161, 170);

    // Accents
    static final Color ACCENT_GOLD = new Color(212, 175, 55);
    static final Color ACCENT_GREY = new Color(82, 82, 91);
    static final Color ACCENT_GREY_DARK = new Color(63, 63, 70);

    static final Color BORDER_COLOR = new Color(63, 63, 70);

    //Data Models

    //comparable interfaces allow us to specify our own rule to sort objects
    //crucial for binary search logic, because it defines the natural order for selection
    static class Course implements Comparable<Course> {
        String code, name;
        int credits, capacity, enrolled;

        public Course(String code, String name, int credits, int capacity) {
            this.code = code.toUpperCase();
            this.name = name;
            this.credits = credits;
            this.capacity = capacity;
            this.enrolled = 0;
        }
        public int getAvailable() { return capacity - enrolled; }
        public boolean isFull() { return enrolled >= capacity; }
        //compares the course code of the courses
        @Override public int compareTo(Course o) { return this.code.compareTo(o.code); }
        @Override public String toString() { return code + ": " + name; }
    }

    //helper class to store student information
    static class Student {
        String id, name;
        List<String> registeredCourses = new ArrayList<>();
        public Student(String id, String name) { this.id = id; this.name = name; }
    }

    //wrapper for queue
    //queues only store *ONE* type of data but for our scenario we need to process data that has 2 attributes
    static class RegistrationRequest {
        String studentId, courseCode;
        public RegistrationRequest(String s, String c) { studentId = s; courseCode = c; }
    }

    // Main data structure
    static class CourseBST {
        class Node {
            Course course;
            Node left, right;
            Node(Course c) {
                this.course = c;
            }
        }
        Node root;

        void insert(Course c) { root = insertRec(root, c); }

        private Node insertRec(Node root, Course c) {
            if (root == null) { //base case; empty spot found
                return new Node(c);
            }
            if (c.compareTo(root.course) < 0) { //if alphabetically smaller than root
                root.left = insertRec(root.left, c);
            }
            else if (c.compareTo(root.course) > 0) { //if alphabetically greater than root
                root.right = insertRec(root.right, c);
            }
            return root;
        }

        //clean method, just to retrieve the course code
        Course search(String code) {
            return searchRec(root, code.toUpperCase());
        }

        //does all the hard work of actually finding it
        private Course searchRec(Node root, String code) {
            if (root == null) {
                return null;
            }
            if (code.equals(root.course.code)) {
                return root.course;
            }
            return code.compareTo(root.course.code) < 0 ?
                    searchRec(root.left, code) :
                    searchRec(root.right, code);
        }

        List<Course> toList() {
            List<Course> l = new ArrayList<>();
            inOrder(root, l);
            return l;
        }
        private void inOrder(Node r, List<Course> l) {
            if (r!=null){
                inOrder(r.left, l);
                l.add(r.course);
                inOrder(r.right, l);
            }
        }
    }

    //Controls everything
    static class BackendSystem {
        private CourseBST courseTree; //binary search tree
        private Map<String, Student> studentDatabase; //hashmap
        private Queue<RegistrationRequest> requestQueue; //queue
        private Consumer<String> logger;

        public BackendSystem(Consumer<String> logger) {
            this.courseTree = new CourseBST();
            this.studentDatabase = new HashMap<>();
            this.requestQueue = new LinkedList<>();
            this.logger = logger;
            seedData();
        }

        //Pre-inserting data
        private void seedData() {
            addCourse("CSC215", "Data Structures and Algorithms", 3, 40);
            addCourse("COM202", "Business and Professional Speech", 3, 30);
            addCourse("CSC211", "Computer Organisation and Assembly Language", 3, 5);
            addCourse("MTH204", "Linear Algebra", 3, 50);
            addCourse("REL101", "Islamic Studies", 3, 20);
            addStudent("20241-35751", "Muhammad Haseeb Haroon");
            addStudent("20241-12345", "Abdul Samad");
            addStudent("20241-54321", "Mohammad Arslan");
            logger.accept("System initialized with seed data.");
        }

        //validates before inserting course
        public void addCourse(String code, String name, int credits, int cap) {
            if (courseTree.search(code) != null) { //if course exists
                logger.accept("Error: Course " + code + " already exists.");
                return;
            }
            courseTree.insert(new Course(code, name, credits, cap));
            logger.accept("Course added: " + code);
        }

        public void addStudent(String id, String name) {
            if (studentDatabase.containsKey(id)) { //if student id already exists
                logger.accept("Error: Student " + id + " already exists.");
                return;
            }
            studentDatabase.put(id, new Student(id, name));
            logger.accept("Student registered: " + name + " (" + id + ")");
        }

        //does NOT register the student, only puts them in "line" or queue
        //validates inputs, then adds them to our custom queue RegistrationRequest
        public void queueRequest(String studentId, String courseCode) {
            if (!studentDatabase.containsKey(studentId)) {
                logger.accept("Error: Student ID " + studentId + " not found.");
                return;
            }
            if (courseTree.search(courseCode) == null) {
                logger.accept("Error: Course " + courseCode + " not found.");
                return;
            }
            requestQueue.add(new RegistrationRequest(studentId, courseCode));
            logger.accept("Request queued: " + studentId + " -> " + courseCode);
        }


        public void processQueue() {
            if (requestQueue.isEmpty()) {
                logger.accept("Queue is empty. No actions taken.");
                return;
            }

            logger.accept("\n--- PROCESSING BATCH ---");
            while (!requestQueue.isEmpty()) {
                RegistrationRequest req = requestQueue.poll(); //dequeues element
                Student student = studentDatabase.get(req.studentId); //separates the *student* from the dequeued element
                Course course = courseTree.search(req.courseCode); //separates the *course* from the dequeued element

                String logPrefix = "Processing " + student.name + " for " + course.code + "... ";

                if (course.isFull()) {
                    logger.accept(logPrefix + "FAILED (Course Full)");
                } else if (student.registeredCourses.contains(course.code)) {
                    logger.accept(logPrefix + "FAILED (Already Enrolled)");
                } else {
                    course.enrolled++; //student enrolled, hence one less space in the course
                    student.registeredCourses.add(course.code); //store the course in student's profile
                    logger.accept(logPrefix + "SUCCESS");
                }
            }
            logger.accept("--- BATCH COMPLETE ---\n");
        }

        //GETTERS
        public List<Course> getAllCourses(int sortMode) {
            List<Course> list = courseTree.toList();
            switch (sortMode) {
                case 2: list.sort(Comparator.comparingInt(c -> c.credits)); break; //resorts the list by credit hours
                case 3: list.sort((c1, c2) -> c2.getAvailable() - c1.getAvailable()); break; // sorts the list by availability: cap - availability
                default: break; //default, returns the list sorted by course code
            }
            return list;
        }

        public List<Student> getAllStudents() {
            return new ArrayList<>(studentDatabase.values());
        }

        public List<RegistrationRequest> getQueue() {
            return new ArrayList<>(requestQueue);
        }
    }

    //Front end
    private BackendSystem backend;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private DefaultTableModel courseModel, studentModel, queueModel;
    private JTextArea logArea;

    public CourseRegistrationGUI() {
        setTitle("Course Registration System");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BG_DARK);
        mainContentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        mainContentPanel.add(createCoursePanel(), "COURSES");
        mainContentPanel.add(createStudentPanel(), "STUDENTS");
        mainContentPanel.add(createQueuePanel(), "QUEUE");
        add(mainContentPanel, BorderLayout.CENTER);

        backend = new BackendSystem(msg -> {
            if (logArea != null) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });

        refreshTables();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));
        sidebar.setPreferredSize(new Dimension(260, 0));

        // Styled Title with Gold Accent
        JLabel title = new JLabel("<html>COURSE REGISTRATION<br><span style='color:#D4AF37'>SYSTEM</span></html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 10, 40, 0));
        sidebar.add(title);

        sidebar.add(createNavButton("Course Catalog", "COURSES"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Student Directory", "STUDENTS"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Queue & Process", "QUEUE"));

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new ModernButton(text, ACCENT_GREY_DARK, true);
        btn.setMaximumSize(new Dimension(240, 45));
        btn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, cardName);
            refreshTables();
        });
        return btn;
    }

    // --- VIEW 1: COURSES ---
    private JPanel createCoursePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);

        // Header & Tools
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Course Catalog (BST)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        lbl.setForeground(TEXT_PRIMARY);
        top.add(lbl, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tools.setBackground(BG_DARK);
        // Minimalist Grey Sorting Buttons
        JButton sortCode = new ModernButton("Sort: Code", ACCENT_GREY, false);
        JButton sortCred = new ModernButton("Sort: Credits", ACCENT_GREY, false);
        JButton sortSeat = new ModernButton("Sort: Availability", ACCENT_GREY, false);

        sortCode.addActionListener(e -> refreshCourseTable(1));
        sortCred.addActionListener(e -> refreshCourseTable(2));
        sortSeat.addActionListener(e -> refreshCourseTable(3));

        tools.add(sortCode); tools.add(sortCred); tools.add(sortSeat);
        top.add(tools, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // Table
        String[] cols = {"Code", "Course Name", "Credits", "Capacity", "Enrolled", "Availability"};
        courseModel = new DefaultTableModel(cols, 0);
        JTable table = new ModernTable(courseModel);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        // Add Form
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBackground(BG_SURFACE);
        addPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField txtCode = new ModernTextField("Code (e.g. CS101)");
        JTextField txtName = new ModernTextField("Course Name");
        JTextField txtCred = new ModernTextField("Credits");
        JTextField txtCap = new ModernTextField("Capacity");

        // Premium Gold Action Button
        JButton btnAdd = new ModernButton("Add Course", ACCENT_GOLD, false);
        btnAdd.setForeground(Color.BLACK); // Black text on Gold for readability/premium feel

        btnAdd.addActionListener(e -> {
            try {
                backend.addCourse(
                        txtCode.getText(),
                        txtName.getText(),
                        Integer.parseInt(txtCred.getText()),
                        Integer.parseInt(txtCap.getText())
                );
                refreshCourseTable(1);
                txtCode.setText(""); txtName.setText(""); txtCred.setText(""); txtCap.setText("");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid Input: Ensure Credits/Cap are numbers."); }
        });

        addPanel.add(txtCode); addPanel.add(txtName); addPanel.add(txtCred); addPanel.add(txtCap); addPanel.add(btnAdd);
        panel.add(addPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- VIEW 2: STUDENTS ---
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Student Directory (HashMap)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Registered Courses"};
        studentModel = new DefaultTableModel(cols, 0);
        JTable table = new ModernTable(studentModel);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        // Add Form
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBackground(BG_SURFACE);
        addPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField txtId = new ModernTextField("Student ID (e.g. S003)");
        JTextField txtName = new ModernTextField("Full Name");
        JButton btnAdd = new ModernButton("Register Student", ACCENT_GOLD, false);
        btnAdd.setForeground(Color.BLACK);

        btnAdd.addActionListener(e -> {
            if(!txtId.getText().isEmpty() && !txtName.getText().isEmpty()) {
                backend.addStudent(txtId.getText(), txtName.getText());
                refreshTables();
                txtId.setText(""); txtName.setText("");
            }
        });

        addPanel.add(txtId); addPanel.add(txtName); addPanel.add(btnAdd);
        panel.add(addPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- VIEW 3: QUEUE & LOGS ---
    private JPanel createQueuePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(BG_DARK);

        // Left Column
        JPanel left = new JPanel(new BorderLayout(0, 20));
        left.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Registration Queue (FIFO)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        lbl.setForeground(TEXT_PRIMARY);
        left.add(lbl, BorderLayout.NORTH);

        String[] cols = {"Student ID", "Target Course"};
        queueModel = new DefaultTableModel(cols, 0);
        JTable table = new ModernTable(queueModel);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        left.add(scroll, BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(BG_DARK);

        JPanel reqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reqPanel.setBackground(BG_SURFACE);
        reqPanel.setMaximumSize(new Dimension(2000, 60));

        JTextField txtSid = new ModernTextField("Student ID");
        JTextField txtCid = new ModernTextField("Course Code");
        JButton btnQueue = new ModernButton("Queue Request", ACCENT_GREY, false);

        btnQueue.addActionListener(e -> {
            if(!txtSid.getText().isEmpty() && !txtCid.getText().isEmpty()) {
                backend.queueRequest(txtSid.getText(), txtCid.getText());
                refreshTables();
            }
        });

        reqPanel.add(txtSid); reqPanel.add(txtCid); reqPanel.add(btnQueue);

        JPanel procPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        procPanel.setBackground(BG_SURFACE);
        procPanel.setMaximumSize(new Dimension(2000, 60));

        // Primary Action Button
        JButton btnProcess = new ModernButton("PROCESS QUEUE BATCH", ACCENT_GOLD, false);
        btnProcess.setPreferredSize(new Dimension(300, 40));
        btnProcess.setForeground(Color.BLACK);

        btnProcess.addActionListener(e -> {
            backend.processQueue();
            refreshTables();
        });
        procPanel.add(btnProcess);

        controls.add(reqPanel);
        controls.add(Box.createVerticalStrut(10));
        controls.add(procPanel);
        left.add(controls, BorderLayout.SOUTH);

        // Right Column: Logs
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(BG_DARK);
        JLabel logLbl = new JLabel("System Logs");
        logLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        logLbl.setForeground(TEXT_SECONDARY);
        right.add(logLbl, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setBackground(BG_SURFACE);
        logArea.setForeground(TEXT_PRIMARY);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane logScroll = new JScrollPane(logArea);
        styleScrollPane(logScroll);
        logScroll.setBorder(new LineBorder(BORDER_COLOR));
        right.add(logScroll, BorderLayout.CENTER);

        panel.add(left);
        panel.add(right);
        return panel;
    }

    // UI HELPERS & STYLING
    private void refreshTables() {
        if(backend == null) return;
        refreshCourseTable(1);

        studentModel.setRowCount(0);
        for(Student s : backend.getAllStudents()) {
            studentModel.addRow(new Object[]{s.id, s.name, s.registeredCourses.toString()});
        }

        queueModel.setRowCount(0);
        for(RegistrationRequest r : backend.getQueue()) {
            queueModel.addRow(new Object[]{r.studentId, r.courseCode});
        }
    }

    private void refreshCourseTable(int sortMode) {
        if(backend == null) return;
        List<Course> list = backend.getAllCourses(sortMode);
        courseModel.setRowCount(0);
        for(Course c : list) {
            courseModel.addRow(new Object[]{c.code, c.name, c.credits, c.capacity, c.enrolled, c.getAvailable()});
        }
    }

    private void styleScrollPane(JScrollPane scroll) {
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(new LineBorder(BORDER_COLOR));
        scroll.getVerticalScrollBar().setBackground(BG_DARK);
        scroll.getHorizontalScrollBar().setBackground(BG_DARK);
    }

    class ModernButton extends JButton {
        Color baseColor;
        boolean isNav;
        public ModernButton(String text, Color bg, boolean nav) {
            super(text);
            this.baseColor = bg;
            this.isNav = nav;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE); // Default white text
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(baseColor.brighter()); repaint(); }
                public void mouseExited(MouseEvent e) { setBackground(baseColor); repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground() == null ? baseColor : getBackground());
            if(getMousePosition() == null) g2.setColor(baseColor);
            int r = isNav ? 10 : 6;
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            super.paintComponent(g);
        }
    }

    class ModernTextField extends JTextField {
        public ModernTextField(String placeholder) {
            setText(placeholder);
            setForeground(TEXT_SECONDARY);
            setBackground(BG_DARK);
            setCaretColor(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR), new EmptyBorder(5, 10, 5, 10)));
            setPreferredSize(new Dimension(130, 35));
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (getText().equals(placeholder)) { setText(""); setForeground(TEXT_PRIMARY); }
                    setBorder(new LineBorder(ACCENT_GOLD)); // Gold border on focus
                }
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (getText().isEmpty()) { setText(placeholder); setForeground(TEXT_SECONDARY); }
                    setBorder(new LineBorder(BORDER_COLOR));
                }
            });
        }
    }

    class ModernTable extends JTable {
        public ModernTable(DefaultTableModel model) {
            super(model);
            setBackground(BG_DARK);
            setForeground(TEXT_PRIMARY);
            setGridColor(BORDER_COLOR);
            setRowHeight(30);
            setFont(new Font("SansSerif", Font.PLAIN, 13));
            getTableHeader().setBackground(BG_SURFACE);
            getTableHeader().setForeground(ACCENT_GOLD); // Gold headers
            getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            getTableHeader().setBorder(new LineBorder(BORDER_COLOR));
            setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) c.setBackground(row % 2 == 0 ? BG_DARK : new Color(30, 30, 35));
                    else c.setBackground(new Color(63, 63, 70)); // Dark Grey Selection
                    return c;
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CourseRegistrationGUI().setVisible(true));
    }
}