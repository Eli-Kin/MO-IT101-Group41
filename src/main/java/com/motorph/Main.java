/**
 * MotorPH Payroll Display System — Main Entry Point
 *
 * This program is a console-based payroll management system for MotorPH employees.
 * It authenticates users via a username/password login, then grants access based on
 * their role within the company.
 *
 * Two access levels are supported:
 *   1. Payroll Staff (Payroll Manager, Team Leader, Rank and File):
 *      - Can browse all employees by ID
 *      - Can view any employee's attendance records and monthly payroll breakdown
 *
 *   2. Regular Employees:
 *      - Can only view their own payroll summary and monthly salary breakdown
 *
 * Payroll computation follows Philippine government contribution rules:
 *   - SSS: Looked up from a contribution table in sss_contribution.csv
 *   - PhilHealth: 3% of monthly gross, capped at PHP 1,800 (employee share = 50%)
 *   - Pag-IBIG: 3–4% of monthly gross, capped at PHP 100
 *   - Withholding Tax: Tiered tax brackets applied to the second-cutoff gross salary
 *
 * Salary is split into two cutoff periods per month:
 *   - 1st Cutoff: Days 1–15 (gross pay only, no deductions)
 *   - 2nd Cutoff: Days 16–End (gross pay minus all government deductions)
 *
 * Work hours are capped at 8 hours per day (8:00 AM–5:00 PM).
 * A 10-minute grace period is applied for late logins.
 * A 1-hour unpaid lunch break is deducted if the employee worked more than 5 hours.
 *
 * Data Sources (CSV files under src/):
 *   - data_info.csv       : Employee personal details, positions, and hourly rates
 *   - data_attendance.csv : Daily time-in and time-out logs per employee
 *   - sss_contribution.csv: SSS contribution bracket table
 *
 * Authors  : MotorPH Development Team
 * Language : Java
 */

package com.motorph;

import java.io.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public class Main {

    //------------------------------------------------------------------------------------------------------------------------------
    //VARIABLES
    // These lists hold SSS bracket data loaded from the CSV, declared at class level so they are
    // accessible across multiple computation calls without re-reading the file every time.
    //------------------------------------------------------------------------------------------------------------------------------

    private static List<String> sssCMinRange = new ArrayList<>();
    private static List<String> sssCMaxRange = new ArrayList<>();
    private static List<String> sssContribution = new ArrayList<>();

    private static int employeeCount;
    //ANSI escape codes for colors
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[34m";

    //template for more colours
    //\u001B + [xx, where xx is code.
    public static final String TEMPLATE = "\u001B[34m";

    //------------------------------------------------------------------------------------------------------------------------------
    // EMPLOYEE DATA — loaded once at class initialization from data_info.csv
    // Storing this in static HashMaps avoids re-reading the file on every user interaction,
    // improving responsiveness for a console application that serves multiple sessions.
    ////Gather employee data--------------------------------------------------------------------------------------------------------
    
    private static Map<String, HashMap<Integer, String>> employeeData;

    static {
        try {
            employeeData = parseEmployeeData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Each map is keyed by employee ID (Integer) for O(1) lookups when an ID is entered.
    private static HashMap<Integer, String> employees = employeeData.get("employees");
    private static HashMap<Integer, String> employeeHourlyRate = employeeData.get("hourlyRate");
    private static HashMap<Integer, String> employeeName = employeeData.get("names");
    private static HashMap<Integer, String> employeeBirthdays = employeeData.get("birthdays");
    private static HashMap<Integer, String> employeesPosition = employeeData.get("positions");

    //------------------------------------------------------------------------------------------------------------------------------
    // ATTENDANCE DATA — loaded once at class initialization from data_attendance.csv
    // Split into three parallel maps (date, in, out) so that index i in each list
    // corresponds to the same attendance record for a given employee ID.
    // Gather data regarding employee attendance-------------------------------------------------------------------------------------
    
    private static Map<String, HashMap<Integer, List<String>>> attendanceData;

    static {
        try {
            attendanceData = parseEmployeeAttendance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HashMap<Integer, List<String>> dateMap = attendanceData.get("date");
    private static HashMap<Integer, List<String>> inMap = attendanceData.get("in");
    private static HashMap<Integer, List<String>> outMap = attendanceData.get("out");

    //------------------------------------------------------------------------------------------------------------------------------
    //MAIN METHOD
    //------------------------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        String input = "";

        //After data has been stored and organised output logo and all employee data and declare and initialize appRunning as true
        boolean appRunning = true;
        loginMenu(appRunning, input, sc);
    }

    //------------------------------------------------------------------------------------------------------------------------------
    //LOGIN MENU
    // Builds credential maps from employee data so that no hardcoded passwords exist —
    // each employee's username is their full name (lowercase, no spaces) and their
    // password is that same string with their numeric ID appended.
    //------------------------------------------------------------------------------------------------------------------------------

    static void loginMenu(boolean appRunning, String input, Scanner sc) throws IOException {
        HashMap<String, Integer> passwords = new HashMap<>();
        HashMap<String, Integer> usernames = new HashMap<>();

  
        // These are the three position titles that grant payroll staff (admin) access.
        // Any employee whose position does not match one of these is treated as a regular employee.      
        String[] payrollStaff = {
                "Payroll Manager",
                "Payroll Team Leader",
                "Payroll Rank and File",
        };

        for (int employees : employeeName.keySet()) {
            //Store Passwords
            passwords.put(employeeName.get(employees).toLowerCase().replace(" ", "") + employees, employees);
            //Store Usernames
            usernames.put(employeeName.get(employees).toLowerCase().replace(" ", ""), employees);
        }

        do {
            String inputtedUsername = "";
            String inputtedPassword = "";
            //Display Logo
            displayLogo();
            System.out.println("Enter e to exit the program");
            System.out.println("Enter Username: ");
            input = sc.next();
            if (Objects.equals(input, "e")) {
                //Terminate the program
                System.out.println("Terminating program. Thank you for using the MotorPH payroll display system");
                displayLogo();
                appRunning = false;
                break;
            } else {
                inputtedUsername = input;
            }

            System.out.println("Enter Password: ");
            input = sc.next();
            if (Objects.equals(input, "e")) {
                //Terminate the program
                System.out.println("Terminating program. Thank you for using the MotorPH payroll display system");
                displayLogo();
                appRunning = false;
                break;
            } else {
                inputtedPassword = input;
            }
            
            // Authentication check: both the username AND password must exist and resolve
            // to the same employee ID. This prevents one employee from using another's ID.
            boolean loginConditions = usernames.containsKey(inputtedUsername) && passwords.containsKey(inputtedPassword) && usernames.get(inputtedUsername).equals(passwords.get(inputtedPassword));
            if (loginConditions) {
                int employeeId = usernames.get(inputtedUsername);
                String employeePosition = employeesPosition.get(employeeId);

                // Grant elevated access if the employee holds a payroll staff position;
                // otherwise restrict them to their own records only.
                boolean adminAccess = Arrays.asList(payrollStaff).contains(employeePosition);

                if (adminAccess) {
                    payrollStaffAccess(input, sc);
                } else {
                    employeeAccess(appRunning, String.valueOf(employeeId), sc);
                }
            } else {
                System.out.println("Either username or password are wrong.");
            }
        } while (appRunning);

    }

    //------------------------------------------------------------------------------------------------------------------------------
    //DISPLAY METHODS
    // These methods handle all console output. Separating display logic from computation
    // logic makes it easier to update the UI without touching payroll calculations.
    //------------------------------------------------------------------------------------------------------------------------------

    static void displayLogo() {
        System.out.println("");
        System.out.println("-".repeat(100));
        
// ASCII art logo with alternating BLUE/RED segments to match MotorPH branding color
        System.out.println("███╗   " + BLUE + "███╗ ██████╗ ████████╗ ██████╗ ██████╗ ██████╗ ██╗  ██╗" + RESET);
        System.out.println("████╗ ███" + BLUE + "█║██╔═══██╗╚══██╔══╝██╔═══██╗██╔══██╗██╔══██╗██║  ██║" + RESET);
        System.out.println("██╔████╔██║█" + BLUE + "█║   ██║   ██║   ██║   ██║██████╔╝██████╔╝███████║" + RESET);
        System.out.println("██║╚██╔╝██" + RED + "║██║   ██║   ██║   ██║   ██║██╔══██╗██╔═══╝ ██╔══██║" + RESET);
        System.out.println("██║ ╚═╝ █" + RED + "█║╚██████╔╝   ██║   ╚██████╔╝██║  ██║██║     ██║  ██║" + RESET);
        System.out.println("╚═╝     " + RED + "╚═╝ ╚═════╝    ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝  ╚═╝" + RESET);

        System.out.println("-".repeat(100));

    }

    static void displayIntro(HashMap<Integer, String> employees) throws IOException {
        //Clear console and display the logo
        System.out.flush();

        System.out.println("-".repeat(100));
        //display header - Table header for the employee roster shown to payroll staff.
        System.out.printf("%-8s %-25s %-25s", "ID", "Name", "Birthday");
        System.out.println();
        //display hashmap employees' data - Iterate through all loaded employees and print each row.
        for (Map.Entry<Integer, String> entry : employees.entrySet()) {
            employeeCount++;
            System.out.printf("%-8d %-20s%n", entry.getKey(), entry.getValue());
        }
        System.out.println("-".repeat(100));
        System.out.println("Enter l to logout.");
        System.out.print("Enter Employee's ID: ");
    }

    static void displayEmployeeData(HashMap<Integer, String> employeeHourlyRate, HashMap<Integer, String> employeeName, HashMap<Integer, String> employeeBirthdays, HashMap<Integer, List<String>> inMap, HashMap<Integer, List<String>> outMap, int id) throws IOException {
        List<String> inList = inMap.get(id);
        List<String> outList = outMap.get(id);

        double HR = Double.parseDouble(employeeHourlyRate.get(id)); //get the hourly rate of the id associated
        long totalSeconds = 0;

        for (int i = 0; i < inList.size(); i++) {
            //every iteration a new array is created
            String[] inParts = inList.get(i).split(":"); //split the list data and store in an array
            String[] outParts = outList.get(i).split(":");

            int inHour = Integer.parseInt(inParts[0]);
            int inMinute = Integer.parseInt(inParts[1]);
            int outHour = Integer.parseInt(outParts[0]);
            int outMinute = Integer.parseInt(outParts[1]);

            //sum the seconds every loop
            totalSeconds += secondsBetweenLog(inHour, inMinute, outHour, outMinute);
        }
     // Display the summary card for this employee before offering further menu options.
        System.out.println("-".repeat(100));
        System.out.println("ID: " + id);
        System.out.println("Name: " + employeeName.get(id));
        System.out.println("Birthday: " + employeeBirthdays.get(id));
        System.out.println("Total Hours: " + secondsToTime(totalSeconds));
        System.out.println("Total Gross Salary: " + computeGrossSalary(totalSeconds, HR));
    }

    static void displayAttendance(HashMap<Integer, List<String>> dateMap, List<String> inList, List<String> outList, List<String> dateList, int chosenID) throws IOException {
        if (dateMap.containsKey(chosenID)) {
            //display header - Only display the attendance table if the employee has records on file.
            System.out.println("-".repeat(100));
            System.out.printf("%-14s %-9s %-9s", "Date", "In", "Out");
            System.out.println();
            // Print each attendance record row using parallel list indices.
            for (int i = 0; i < dateList.toArray().length; i++) {
                System.out.printf("%-14s %-9s %-9s", dateList.get(i), inList.get(i), outList.get(i));
                System.out.println();
            }
        }
    }

    static void displayPayrollStaffOptions() {
        // Menu shown inside the per-employee view for payroll staff, offering
        // payroll breakdown, attendance log, or a return to the employee list.
        System.out.println("-".repeat(100));
        System.out.println("Enter g to display payroll.");
        System.out.println("Enter a to show attendance.");
        System.out.println("Enter e to go back.");
    }

    static void displayEmployeeOptions() {
        // Simplified menu for regular employees — they cannot view other employees
        // or attendance logs, so only payroll and logout are offered.
        System.out.println("-".repeat(100));
        System.out.println("Enter g to display payroll.");
        System.out.println("Enter l to logout.\n");
    }

    static void displayTotalNetSalary(List<String> inList, List<String> outList, List<String> dateList, double HR) throws IOException {
        // Build the month-by-cutoff seconds map first so we can pass it into
        // the net salary computation without re-iterating the attendance lists.
        HashMap<Integer, long[]> monthSeconds = buildMonthSeconds(dateList, inList, outList);
        System.out.println("Total Net Salary: PHP " + computeTotalNetSalary(monthSeconds, HR));
    }

    static void displayMonthlySalary(List<String> dateList, List<String> inList, List<String> outList, double HR) throws IOException {
        // Group all attendance records into first and second cutoff seconds per month
        // so we can display a clean per-month, per-cutoff salary breakdown.
        HashMap<Integer, long[]> monthSeconds = buildMonthSeconds(dateList, inList, outList);

        for (int month : monthSeconds.keySet()) {
            long firstCutoffSeconds = monthSeconds.get(month)[0];
            long secondCutoffSeconds = monthSeconds.get(month)[1];

            double firstGross = computeGrossSalary(firstCutoffSeconds, HR);
            double secondGross = computeGrossSalary(secondCutoffSeconds, HR);
            // Government deductions (SSS, PhilHealth, Pag-IBIG, Withholding Tax) are applied
            // only on the second cutoff, matching standard Philippine payroll practice.
            double netSalary = computeNetGrossSalary(secondGross);

            // Total month take-home = full 1st cutoff gross + deduction-reduced 2nd cutoff net.
            BigDecimal totalMonthSalary = BigDecimal.valueOf(firstGross).add(BigDecimal.valueOf(netSalary));

            System.out.println("=".repeat(40));
            System.out.println("Month : " + getMonthName(month));
            System.out.println("-".repeat(40));

            System.out.println("1st Cutoff (Days 1-15)");
            System.out.println("  Hours : " + secondsToTime(firstCutoffSeconds));
            System.out.println("  Gross : PHP " + firstGross);
            System.out.println("  Net   : PHP " + firstGross + " (deductions applied on 2nd cutoff)");
            System.out.println("-".repeat(40));

            System.out.println("2nd Cutoff (Days 16-End)");
            System.out.println("  Hours : " + secondsToTime(secondCutoffSeconds));
            System.out.println("  Gross : PHP " + secondGross);
            System.out.println("  Net   : PHP " + netSalary);
            System.out.println("-".repeat(40));
            System.out.println("Deductions");
            System.out.println("SSS             : " + computeSSS(secondGross));
            System.out.println("PhilHealth      : " + computePhilHealth(secondGross));
            System.out.println("Pagibig         : " + computePagibig(secondGross));
            System.out.println("Withholding Tax : " + computeWithholdingTax(secondGross));
            System.out.println("-".repeat(40));
            System.out.println(getMonthName(month) + "'s Total Month Net Salary: PHP " + totalMonthSalary);
            System.out.println("=".repeat(40));
            System.out.println();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------
    //ACCESS METHODS
    // These methods control what each user type can see and do after login.
    // Keeping staff and employee flows in separate methods prevents role-based
    // logic from becoming tangled in a single large loop.
    //------------------------------------------------------------------------------------------------------------------------------

    static void payrollStaffAccess(String input, Scanner sc) throws IOException {
        boolean inDashboard = true;
        displayLogo();
        displayIntro(employees);
        do {
            //Asks for the employee ID - Read the employee ID the payroll staff wants to inspect.
            input = sc.next();

            if (Objects.equals(input, "l")) {
                inDashboard = false;
                break;
            }

            //checks if it's not an integer
            if (!isInteger(input)) {
                System.out.println("Please enter a number.");
                continue;
            }

            //checks if input is a valid chosenID number
            if (!employees.containsKey(Integer.parseInt(input))) {
                System.out.println("ID not found in employees");
                continue;
            }

            //Convert input to int and store as chosenID
            int chosenID = Integer.parseInt(input);

            //Get time in, out, list of dates and hourly rate of the associated ID
            List<String> inList = inMap.get(chosenID);
            List<String> outList = outMap.get(chosenID);
            List<String> dateList = dateMap.get(chosenID);
            double HR = Double.parseDouble(employeeHourlyRate.get(chosenID));

            //Displays employee data tied to the ID
            displayEmployeeData(employeeHourlyRate, employeeName, employeeBirthdays, inMap, outMap, chosenID);
            displayTotalNetSalary(inList, outList, dateList, HR);

            //Enters a loop asking for further information about an employee
            // Inner loop keeps staff inside a single employee's record until they choose to go back,
            // avoiding a return to the ID-entry prompt after every single action.
            boolean inEmployees = true;
            loop:
            while (inEmployees) {
                displayPayrollStaffOptions();
                input = sc.next().toLowerCase();

                //Checking if input is too long
                if (input.length() != 1) {
                    System.out.println("Input too long.");
                    continue;
                }
                //Allowed inputs are g, a, e and t.
                switch (input) {
                    case "g":
                        //Display weekly gross salary
                        displayMonthlySalary(dateList, inList, outList, HR);
                        break;
                    case "a":
                        //Display attendance
                        displayAttendance(dateMap, inList, outList, dateList, chosenID);
                        break;
                    case "e":
                        //Return to the start of the program
                        displayIntro(employees);
                        break loop;
                    default:
                        System.out.println("Please input either \"g\", \"a\" or \"e\".");
                        continue;
                }
            }
        } while (inDashboard);
    }

    static void employeeAccess(boolean appRunning, String input, Scanner sc) throws IOException {
        do {
            displayLogo();

            //Convert input to int and store as chosenID
            int chosenID = Integer.parseInt(input);

            //Get time in, out, list of dates and hourly rate of the associated ID
            List<String> inList = inMap.get(chosenID);
            List<String> outList = outMap.get(chosenID);
            List<String> dateList = dateMap.get(chosenID);
            double HR = Double.parseDouble(employeeHourlyRate.get(chosenID));

            //Displays employee data tied to the ID
            displayEmployeeData(employeeHourlyRate, employeeName, employeeBirthdays, inMap, outMap, chosenID);
            displayTotalNetSalary(inList, outList, dateList, HR);

            //Enters a loop asking for further information about an employee
            boolean inEmployees = true;
            loop:
            while (inEmployees) {
                displayEmployeeOptions();
                input = sc.next().toLowerCase();

                //Checking if input is too long
                if (input.length() != 1) {
                    System.out.println("Input too long.");
                    continue;
                }
                //Allowed inputs are g, a, e and t.
                switch (input) {
                    case "g":
                        //Display weekly gross salary
                        displayMonthlySalary(dateList, inList, outList, HR);
                        break;
                    case "l":
                        appRunning = false;
                        break loop;
                    default:
                        System.out.println("Please input either \"g\" or \"l\".");
                        continue;
                }
            }
        } while (appRunning);
    }

    //------------------------------------------------------------------------------------------------------------------------------
    //COMPUTE METHODS
    // All monetary calculations are isolated here so that changes to government
    // contribution rules only require edits in one place.
    //------------------------------------------------------------------------------------------------------------------------------

    static double computeGrossSalary(long seconds, double gross) {
        double hour = seconds / 3600.0; //convert seconds to hour - Convert total worked seconds to fractional hours, then multiply by the hourly rate.
        return hour * gross;
    }

    static double computeNetGrossSalary(double monthlyGross) throws IOException {
    // Sum all mandatory Philippine government deductions applied to the second-cutoff gross.
        double sssTotalContribution = computeSSS(monthlyGross);
        double philhealthContribution = computePhilHealth(monthlyGross);
        double pagibigContribution = computePagibig(monthlyGross);
        double withholdingTax = computeWithholdingTax(monthlyGross);

        double totalContribution = sssTotalContribution + philhealthContribution + pagibigContribution + withholdingTax;

        // DEBUG - remove after fixing
//        System.out.println("-".repeat(100));
//        System.out.println("monthlyGross: " + monthlyGross);
//        System.out.println("sssContribute: " + sssContribution);
//        System.out.println("philhealth: " + philhealthContribution);
//        System.out.println("pagibig: " + pagibigContribution);
//        System.out.println("withholdingTax weekly: " + withholdingTax);
//        System.out.println("totalContribution: " + totalContribution);

        return monthlyGross - totalContribution;
    }

    static double computeTotalNetSalary(HashMap<Integer, long[]> monthSeconds, double HR) throws IOException {
        // Iterate over every month's two cutoff periods, compute gross for each,
        // apply deductions only to the second cutoff, then accumulate the running total.
        double total = 0;
        for (int month : monthSeconds.keySet()) {
            double firstGross = computeGrossSalary(monthSeconds.get(month)[0], HR);
            double secondGross = computeGrossSalary(monthSeconds.get(month)[1], HR);
            total += firstGross + computeNetGrossSalary(secondGross);
        }
        return total;
    }

    static double computeSSS(double monthlyGross) throws IOException {
        // Read the SSS bracket table from CSV each time this is called so that
        // the contribution schedule can be updated without recompiling the program.
        Map<String, List<String>> sssCData = parseSSS();
        sssCMinRange = sssCData.get("minRange");
        sssCMaxRange = sssCData.get("maxRange");
        sssContribution = sssCData.get("contribution");

        // SSS Contribution
        // Walk the bracket table to find the row whose min–max range contains the monthly gross.
        // "Over" in the max column means no upper bound, so it maps to Double.MAX_VALUE.
        double sssTotalContribution = 0;
        for (int i = 0; i < sssCMinRange.size(); i++) {
            double min = Double.parseDouble(sssCMinRange.get(i));
            double max;
            if (sssCMaxRange.get(i).equalsIgnoreCase("Over")) {
                max = Double.MAX_VALUE;
            } else {
                max = Double.parseDouble(sssCMaxRange.get(i));
            }
            double con = Double.parseDouble(Main.sssContribution.get(i));

            if (monthlyGross >= min && monthlyGross <= max) {
                sssTotalContribution = con;
            } else if (monthlyGross < 3250) {
            // Floor case: salaries below the lowest bracket use the minimum contribution of PHP 135.
                sssTotalContribution = 135.0;
            }
        }

        return sssTotalContribution;
    }

    static double computePhilHealth(double monthlyGross) {
         // PhilHealth premium = 3% of monthly gross, capped at PHP 1,800 total.
        // The employee pays half (50%) of the total premium.
        double premiumMonthly = Math.min(monthlyGross * 0.03, 1800); //maximum contribution is 1800
        double philhealthContribution = (premiumMonthly * 0.5);

        return philhealthContribution;
    }

    static double computePagibig(double monthlyGross) {
        // Pag-IBIG contribution rate scales with salary:
        //   PHP 1,000–1,499 → 3%
        //   PHP 1,500 and above → 4%
        // The total contribution is capped at PHP 100 regardless of rate.
        double pagibigTotalRate = 0;
        if (monthlyGross >= 1000 && monthlyGross < 1500) {
            pagibigTotalRate = 0.03;
        } else if (monthlyGross > 1500) {
            pagibigTotalRate = 0.04;
        }
        double pagibigContribution = Math.min(monthlyGross * pagibigTotalRate, 100);

        return pagibigContribution;
    }

    static double computeWithholdingTax(double monthlyGross) {
        // Tiered withholding tax based on BIR's monthly income tax table.
        // Each bracket applies a percentage only to the amount exceeding the bracket floor,
        // then adds the fixed tax due for that bracket (the "plus" amount).
        double taxRate = 0;
        double excess = 0;
        double plus = 0;
        double withholdingTax = 0;
        if (monthlyGross > 20833 && monthlyGross <= 33333) {
            taxRate = 0.20;
            excess = 20833;
            withholdingTax = (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 33333 && monthlyGross <= 66667) {
            taxRate = 0.25;
            excess = 33333;
            plus = 2500;
            withholdingTax = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 66667 && monthlyGross <= 166667) {
            taxRate = 0.30;
            excess = 66667;
            plus = 10833;
            withholdingTax = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 166667 && monthlyGross <= 666667) {
            taxRate = 0.32;
            excess = 166667;
            plus = 40833.33;
            withholdingTax = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 666667) {
            taxRate = 0.35;
            excess = 666667;
            plus = 200833.33;
            withholdingTax = plus + (monthlyGross - excess) * taxRate;
        }

        return withholdingTax;
    }

    //------------------------------------------------------------------------------------------------------------------------------
    //CSV PARSERS
    // Each parser reads a single CSV file and returns a structured Map so that
    // the rest of the program never has to deal with raw file I/O or string splitting.
    //------------------------------------------------------------------------------------------------------------------------------

    static Map<String, HashMap<Integer, String>> parseEmployeeData() throws IOException {
        // Read employee personal and salary data from the info CSV.
        // The regex split pattern handles quoted commas inside fields (e.g., "Last, First" style names).
        String infoFile = "src\\data_info.csv";
        BufferedReader infoReader = new BufferedReader(new FileReader(infoFile));
        infoReader.readLine(); //skip header

        //Stores chosen columns in a hashmap
        HashMap<Integer, String> employees = new HashMap<>();
        HashMap<Integer, String> employeesHourlyRate = new HashMap<>();
        HashMap<Integer, String> employeesNames = new HashMap<>();
        HashMap<Integer, String> employeeBirthdays = new HashMap<>();
        HashMap<Integer, String> employeesPosition = new HashMap<>();

        String columnRead;
        while ((columnRead = infoReader.readLine()) != null) {
             // Regex splits on commas that are NOT inside double quotes,
            // which is necessary because some fields (e.g., addresses) contain commas.
            String[] infoRow = columnRead.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            int id = Integer.parseInt(infoRow[0]);

           // Combine first (index 2) and last (index 1) name into a single display string.
            String name = infoRow[1] + " " + infoRow[2];
            String birthday = infoRow[3];
            String formatted = String.format("%-25s %-25s", name, birthday);
            String position = infoRow[11];  // Column 11 holds the employee's job position title.

            employees.put(id, formatted);
            employeesHourlyRate.put(id, infoRow[18]); // Column 18 holds the hourly rate.
            employeesNames.put(id, name);
            employeeBirthdays.put(id, birthday);
            employeesPosition.put(id, position);
        }

        //Store columns required in a Map and store it - Bundle all maps under a single return value to keep the method signature clean.
        Map<String, HashMap<Integer, String>> employeeData = new HashMap<>();
        employeeData.put("employees", employees);
        employeeData.put("hourlyRate", employeesHourlyRate);
        employeeData.put("names", employeesNames);
        employeeData.put("birthdays", employeeBirthdays);
        employeeData.put("positions", employeesPosition);

        return employeeData;
    }

    static Map<String, HashMap<Integer, List<String>>> parseEmployeeAttendance() throws FileNotFoundException, IOException {
        //Provide directory for path and set up BufferedReader - Read daily attendance logs. Each row represents one day's clock-in/out for one employee.
        String attendanceFile = "src\\data_attendance.csv";
        BufferedReader attendanceReader = new BufferedReader(new FileReader(attendanceFile));

        //HashMaps to store data per employee ID
        HashMap<Integer, List<String>> dateMap = new HashMap<>();
        HashMap<Integer, List<String>> inMap = new HashMap<>();
        HashMap<Integer, List<String>> outMap = new HashMap<>();

        //Skip the header
        attendanceReader.readLine();

        String columnRead;
        while ((columnRead = attendanceReader.readLine()) != null) {
            String[] attendanceRow = columnRead.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            int id = Integer.parseInt(attendanceRow[0]);
            String attendanceDate = attendanceRow[3];
            String attendanceIn = attendanceRow[4];
            String attendanceOut = attendanceRow[5];

            //if id doesn't exist, create a new list, if it does exist then ignore
            dateMap.putIfAbsent(id, new ArrayList<>());
            inMap.putIfAbsent(id, new ArrayList<>());
            outMap.putIfAbsent(id, new ArrayList<>());

            //Store the date to the list for that ID
            dateMap.get(id).add(attendanceDate);
            inMap.get(id).add(attendanceIn);
            outMap.get(id).add(attendanceOut);
        }
        //Close reader
        attendanceReader.close();

        //Organise hashmaps into a map and return
        Map<String, HashMap<Integer, List<String>>> attendanceRecords = new HashMap<>();
        attendanceRecords.put("date", dateMap);
        attendanceRecords.put("in", inMap);
        attendanceRecords.put("out", outMap);

        return attendanceRecords;
    }

    static Map<String, List<String>> parseSSS() throws IOException {
        //Provide directory for path and set up BufferedReader
        // Read the SSS contribution bracket table.
        // Two header rows are skipped because the CSV uses a title row followed by a column-label row.
        String sssFile = "src\\sss_contribution.csv";
        BufferedReader sssReader = new BufferedReader(new FileReader(sssFile));

        // Lists to store SSS ranges and contribution
        List<String> minRange = new ArrayList<>();
        List<String> maxRange = new ArrayList<>();
        List<String> contribution = new ArrayList<>();

        // skip header
        sssReader.readLine();
        sssReader.readLine();

        String columnRead;
        while ((columnRead = sssReader.readLine()) != null) {
            String[] sssRow = columnRead.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            // Clean quotes and commas from all columns
            // Strip quotes and embedded commas from numeric fields before parsing,
            // because currency values in the CSV are formatted as e.g. "1,250.00".
            for (int i = 0; i < sssRow.length; i++) {
                sssRow[i] = sssRow[i].replace("\"", "").replace(",", "");
            }
            //Add data to lists
            minRange.add(sssRow[0]);
            maxRange.add(sssRow[2]); // Column 2 is the upper bound of the bracket.
            contribution.add(sssRow[3]); // Column 3 is the fixed employee contribution for that bracket.
        }

        //Close the reader
        sssReader.close();

        //Store data and return as a map
        Map<String, List<String>> sss = new HashMap<>();
        sss.put("minRange", minRange);
        sss.put("maxRange", maxRange);
        sss.put("contribution", contribution);

        return sss;
    }

    //------------------------------------------------------------------------------------------------------------------------------
    //HELPER METHODS
    // Small, reusable utilities that support both display and computation logic.
    //------------------------------------------------------------------------------------------------------------------------------

    //return true if the value can be converted to an int, if not then return false
    static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str); //convert string into an int
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static HashMap<Integer, long[]> buildMonthSeconds(List<String> dateList, List<String> inList, List<String> outList) {
        // Group worked seconds by month and by cutoff period (1–15 vs 16–end).
        // Each map entry is a two-element array: [firstCutoffSeconds, secondCutoffSeconds].
        // This structure lets callers compute gross pay for each half independently.
        HashMap<Integer, long[]> monthSeconds = new HashMap<>();

        for (int i = 0; i < inList.size(); i++) {
            String[] inParts = inList.get(i).split(":");
            String[] outParts = outList.get(i).split(":");
            String[] dateParts = dateList.get(i).split("/");

            int month = Integer.parseInt(dateParts[0]);
            int workDay = Integer.parseInt(dateParts[1]);

            int inHour = Integer.parseInt(inParts[0]);
            int inMinute = Integer.parseInt(inParts[1]);
            int outHour = Integer.parseInt(outParts[0]);
            int outMinute = Integer.parseInt(outParts[1]);

            monthSeconds.putIfAbsent(month, new long[]{0L, 0L});

            long seconds = secondsBetweenLog(inHour, inMinute, outHour, outMinute);
            // Assign seconds to the correct cutoff bin based on the calendar day of the month.
            if (workDay <= 15) {
                monthSeconds.get(month)[0] += seconds;
            } else {
                monthSeconds.get(month)[1] += seconds;
            }
        }
        return monthSeconds;
    }

    //Calculate the hours between log in and log out
    static long secondsBetweenLog(int inHour, int inMinute, int outHour, int outMinute) {
        //convert int into time.
        LocalTime logIn = LocalTime.of(inHour, inMinute);
        LocalTime logOut = LocalTime.of(outHour, outMinute);

        // Enforce an official shift window of 8:00 AM–5:00 PM.
        // A 10-minute grace period means logins before 8:10 AM are treated as exactly 8:00 AM,
        // ensuring employees are not penalized for minor tardiness while also not rewarding early arrivals.
        // only count 8:00 AM to 5:00 PM, 10min Grace period
        if (logIn.isBefore(LocalTime.of(8, 10))) {
            logIn = LocalTime.of(8, 0);
        }
        if (logOut.isAfter(LocalTime.of(17, 0))) {
            logOut = LocalTime.of(17, 0);
        }
        //If invalid or reversed times - Guard against data-entry errors where logout appears before login.
        if (logOut.isBefore(logIn)) {
            return 0;
        }

        //Cap at 8 hours (28800 seconds) - to prevent overtime from inflating computed pay.
        long secondsBetween = Math.min(Duration.between(logIn, logOut).getSeconds(), 28800);

        //Deduct 1-hour lunch if worked more than 5 hours.
        if (secondsBetween > 18000) {
            secondsBetween -= 3600;
        }

        return secondsBetween;
    }

    static String secondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600; //convert second into hour
        long remainingSecondsAfterHours = totalSeconds % 3600; //get the remainder after hour
        long minutes = remainingSecondsAfterHours / 60; //convert the remaining seconds into minutes

        return hours + "h " + minutes + "m ";
    }

    // Maps a numeric month (1–12) to its full English name for use in payroll display headers.
    // Separate method for resolving month names
    static String getMonthName(int month) {
        switch (month) {
            case 1:
                return "January";
            case 2:
                return "February";
            case 3:
                return "March";
            case 4:
                return "April";
            case 5:
                return "May";
            case 6:
                return "June";
            case 7:
                return "July";
            case 8:
                return "August";
            case 9:
                return "September";
            case 10:
                return "October";
            case 11:
                return "November";
            default:
                return "December";
        }
    }

}
