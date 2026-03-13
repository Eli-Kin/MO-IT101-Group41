package com.motorph;

import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

//TODO: add automatic calculations

public class Main {
    private static List<String> sssCMinRange = new ArrayList<>();
    private static List<String> sssCMaxRange = new ArrayList<>();
    private static List<String> sssContribution = new ArrayList<>();
    private static List<Double> totalNetSalary = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        String input;

        Map<String, HashMap<Integer, String>> employeeData = parseEmployeeData();
        HashMap<Integer, String> employees = employeeData.get("employees");
        HashMap<Integer, String> employeeHourlyRate = employeeData.get("hourlyRate");
        HashMap<Integer, String> employeeName = employeeData.get("names");
        HashMap<Integer, String> employeeBirthdays = employeeData.get("birthdays");

        //After data has been stored and organised output logo and all employee data and declare and initialize appRunning as true
        boolean appRunning = true;
        displayIntro(employees, employeeName, employeeBirthdays);
        do {
            //Asks for the employee ID
            input = sc.next();

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

            //Gather data regarding employee attendance
            Map<String, HashMap<Integer, List<String>>> attendanceData = parseEmployeeAttendance();
            HashMap<Integer, List<String>> dateMap = attendanceData.get("date");
            HashMap<Integer, List<String>> inMap = attendanceData.get("in");
            HashMap<Integer, List<String>> outMap = attendanceData.get("out");

            //Convert input to int and store as chosenID
            int chosenID = Integer.parseInt(input);
            //Get time in, out and hourly rate of the associated ID
            List<String> inList = inMap.get(chosenID);
            List<String> outList = outMap.get(chosenID);
            double HR = Double.parseDouble(employeeHourlyRate.get(chosenID));

            //displays employee data tied to the ID
            displayEmployeeData(employeeHourlyRate, employeeName, employeeBirthdays, inMap, outMap, chosenID);
            displayTotalNetSalary(inList, outList, HR);

            //Enters a loop asking for further information about an employee
            boolean inEmployees = true;
            loop: while (inEmployees) {
                displayOptions();
                input = sc.next().toLowerCase();

                //Checking if input is too long
                if (input.length() != 1) {
                    System.out.println("Input too long.");
                    continue;
                }
                //Allowed inputs are g, a, and e.
                switch (input) {
                    case "g":
                        displayWeeklyGrossSalary(inList, outList, HR);
                        break;
                    case "a":
                        displayAttendance(dateMap, inMap, outMap, chosenID);
                        break;
                    case "e":
                        displayIntro(employees, employeeName, employeeBirthdays);
                        break loop;
                    default:
                        System.out.println("Please input either \"g\", \"a\" or \"e\".");
                        continue;
                }
            }
        } while (appRunning);
    }

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

    //Calculate the hours between log in and log out
    static long hourBetweenLog(int inHour, int inMinute, int outHour, int outMinute) {
        //convert int into time.
        LocalTime logIn = LocalTime.of(inHour, inMinute);
        LocalTime logOut = LocalTime.of(outHour, outMinute);

        // only count 8:00 AM to 5:00 PM
        if (logIn.isBefore(LocalTime.of(8, 0))) {
            logIn = LocalTime.of(8, 0);
        }
        if (logOut.isAfter(LocalTime.of(17, 0))) {
            logOut = LocalTime.of(17, 0);
        }

        return Duration.between(logIn, logOut).getSeconds();
    }

    static String secondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600; //convert second into hour
        long remainingSecondsAfterHours = totalSeconds % 3600; //get the remainder after hour
        long minutes = remainingSecondsAfterHours / 60; //convert the remaining seconds into minutes

        return hours + "h " + minutes + "m ";
    }

    static double grossSalaryCalculator(long seconds, double gross) {
        return (seconds / 3600.0) * gross;
    }

    static double netGrossSalaryCalculator(double weeklyGross) throws IOException {
        Map<String, List<String>> sssCData = parseSSS();
        sssCMinRange = sssCData.get("minRange");
        sssCMaxRange = sssCData.get("maxRange");
        sssContribution = sssCData.get("contribution");

        // Convert weekly gross to monthly equivalent
        double monthlyGross = weeklyGross * (52.0 / 12.0); // 4.333 weeks per month, 52 weeks divided by 12 months

        // SSS Contribution
        double sssMonthly = 0;
        for (int i = 0; i < sssCMinRange.size(); i++) {
            double min = Double.parseDouble(sssCMinRange.get(i));
            double max;
            if (sssCMaxRange.get(i).equalsIgnoreCase("Over")) {
                max = Double.MAX_VALUE;
            } else {
                max = Double.parseDouble(sssCMaxRange.get(i));
            }
            double con = Double.parseDouble(sssContribution.get(i));

            if (monthlyGross >= min && monthlyGross <= max) {
                sssMonthly = con;
            } else if (monthlyGross < 3250) {
                sssMonthly = 135.0;
            }
        }
        double sssContribute = sssMonthly / (52.0 / 12.0); // weekly share

        // PhilHealth
        double premiumMonthly = Math.min(monthlyGross * 0.03, 1800); //maximum contribution is 1800
        double philhealthContribution = (premiumMonthly * 0.5) / (52.0 / 12.0);

        // Pag-ibig
        double pgTotalRate = 0;
        if (monthlyGross >= 1000 && monthlyGross < 1500) {
            pgTotalRate = 0.03;
        } else if (monthlyGross > 1500) {
            pgTotalRate = 0.04;
        }
        double pagibigContribution = Math.min(monthlyGross * pgTotalRate, 100) / (52.0 / 12.0);

        // Withholding Tax
        double taxRate = 0;
        double excess = 0;
        double plus = 0;
        double withholdingTaxMonthly = 0;
        if (monthlyGross > 20833 && monthlyGross <= 33333) {
            taxRate = 0.20;
            excess = 20833;
            withholdingTaxMonthly = (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 33333 && monthlyGross <= 66667) {
            taxRate = 0.25;
            excess = 33333;
            plus = 2500;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 66667 && monthlyGross <= 166667) {
            taxRate = 0.30;
            excess = 66667;
            plus = 10833;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 166667 && monthlyGross <= 666667) {
            taxRate = 0.32;
            excess = 166667;
            plus = 40833.33;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 666667) {
            taxRate = 0.35;
            excess = 666667;
            plus = 200833.33;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        }
        double withholdingTax = withholdingTaxMonthly / (52.0 / 12.0);

        double totalContribution = sssContribute + philhealthContribution + pagibigContribution + withholdingTax;

        // DEBUG - remove after fixing
//        System.out.println("-".repeat(100));
//        System.out.println("monthlyGross: " + monthlyGross);
//        System.out.println("sssMonthly: " + sssMonthly);
//        System.out.println("sssContribute: " + sssContribute);
//        System.out.println("philhealth: " + philhealthContribution);
//        System.out.println("pagibig: " + pagibigContribution);
//        System.out.println("withholdingTaxMonthly: " + withholdingTaxMonthly);
//        System.out.println("withholdingTax weekly: " + withholdingTax);
//        System.out.println("totalContribution: " + totalContribution);

        return weeklyGross - totalContribution;
    }

    static void displayIntro(HashMap<Integer, String> employees, HashMap<Integer, String> employeeeNames, HashMap<Integer, String> employeeBirthdays) throws IOException {
        System.out.flush();
        System.out.println("");
        System.out.println("-".repeat(100));

        System.out.println("███╗   ███╗ ██████╗ ████████╗ ██████╗ ██████╗ ██████╗ ██╗  ██╗");
        System.out.println("████╗ ████║██╔═══██╗╚══██╔══╝██╔═══██╗██╔══██╗██╔══██╗██║  ██║");
        System.out.println("██╔████╔██║██║   ██║   ██║   ██║   ██║██████╔╝██████╔╝███████║");
        System.out.println("██║╚██╔╝██║██║   ██║   ██║   ██║   ██║██╔══██╗██╔═══╝ ██╔══██║");
        System.out.println("██║ ╚═╝ ██║╚██████╔╝   ██║   ╚██████╔╝██║  ██║██║     ██║  ██║");
        System.out.println("╚═╝     ╚═╝ ╚═════╝    ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝  ╚═╝");

        System.out.println("-".repeat(100));

        //display header
        System.out.printf("%-8s %-25s %-25s", "ID", "Name", "Birthday");
        System.out.println();
        //display hashmap employees' data
        for (Map.Entry<Integer, String> entry : employees.entrySet()) {
            System.out.printf("%-8d %-20s%n", entry.getKey(), entry.getValue());
        }

        System.out.println("-".repeat(100));
        System.out.print("Enter Employee's ID: ");
    }

    static void displayEmployeeData(HashMap<Integer, String> employeeHourlyRate, HashMap<Integer, String> employeeName, HashMap<Integer, String> employeeBirthdays, HashMap<Integer, List<String>> inMap, HashMap<Integer, List<String>> outMap, int id) throws IOException {

        List<String> inList = inMap.get(id);
        List<String> outList = outMap.get(id);

        double HR = Double.parseDouble(employeeHourlyRate.get(id)); //get the hourly rate of the id associated
        int week = 0;
        long totalSeconds = 0;
        long weeklySeconds = 0;

        for (int i = 0; i < inList.size(); i++) {
            //every iteration a new array is created
            String[] inParts = inList.get(i).split(":"); //split the list data and store in an array
            String[] outParts = outList.get(i).split(":");

            int inHour = Integer.parseInt(inParts[0]);
            int inMinute = Integer.parseInt(inParts[1]);
            int outHour = Integer.parseInt(outParts[0]);
            int outMinute = Integer.parseInt(outParts[1]);

            //sum the seconds every loop
            totalSeconds += hourBetweenLog(inHour, inMinute, outHour, outMinute);
        }

        System.out.println("-".repeat(100));
        System.out.println("ID: " + id);
        System.out.println("Name: " + employeeName.get(id));
        System.out.println("Birthday: " + employeeBirthdays.get(id));
        System.out.println("Total Hours: " + secondsToTime(totalSeconds));
        System.out.println("Total Gross Salary: " + grossSalaryCalculator(totalSeconds, HR));
    }

    static void displayOptions() {
        System.out.println("-".repeat(100));
        System.out.println("Enter g to display gross salary per week.");
        System.out.println("Enter a to show attendance.");
        System.out.println("Enter e to go back. \n");
    }

    static void displayWeeklyGrossSalary(List<String> inList, List<String> outList, double HR) throws IOException {
        long weeklySeconds = 0;
        int week = 0;
        for (int i = 0; i < inList.size(); i++) {
            //every iteration a new array is created
            String[] inParts = inList.get(i).split(":");
            String[] outParts = outList.get(i).split(":");

            int inHour = Integer.parseInt(inParts[0]);
            int inMinute = Integer.parseInt(inParts[1]);
            int outHour = Integer.parseInt(outParts[0]);
            int outMinute = Integer.parseInt(outParts[1]);

            weeklySeconds += hourBetweenLog(inHour, inMinute, outHour, outMinute);
            //every 5 iterations
            if ((i + 1) % 5 == 0) {
                double weekGross = grossSalaryCalculator(weeklySeconds, HR);
                double netSalary = netGrossSalaryCalculator(weekGross);
                week++;
                System.out.println("Week " + week);
                System.out.println("Weekly Gross: " + weekGross);
                System.out.println("Weekly Net Salary: " + netSalary);
                System.out.println("Hours in the Week: " + secondsToTime(weeklySeconds));
                System.out.println("-".repeat(20));
                weeklySeconds = 0; //reset weeklyseconds
            }
        }
    }

    static void displayTotalNetSalary(List<String> inList, List<String> outList, double HR) throws IOException {
        long weeklySeconds = 0;
        double total = 0;
        for (int i = 0; i < inList.size(); i++) {
            //every iteration a new array is created
            String[] inParts = inList.get(i).split(":");
            String[] outParts = outList.get(i).split(":");

            int inHour = Integer.parseInt(inParts[0]);
            int inMinute = Integer.parseInt(inParts[1]);
            int outHour = Integer.parseInt(outParts[0]);
            int outMinute = Integer.parseInt(outParts[1]);

            weeklySeconds += hourBetweenLog(inHour, inMinute, outHour, outMinute);
            //every 5 iterations
            if ((i + 1) % 5 == 0) {
                double weekGross = grossSalaryCalculator(weeklySeconds, HR);
                double netSalary = netGrossSalaryCalculator(weekGross);
                totalNetSalary.add(netSalary);
                weeklySeconds = 0; //reset weeklyseconds
            }
        }

        for (int i = 0; i < totalNetSalary.size(); i++){
            total += totalNetSalary.get(i);
        }

        System.out.println("Total Net Salary: " + total);
    }

    static void displayAttendance(HashMap<Integer, List<String>> dateMap, HashMap<Integer, List<String>> inMap, HashMap<Integer, List<String>> outMap,int id) throws IOException {
        // get the list for the specific id
        List<String> dateList = dateMap.get(id);
        List<String> inList = inMap.get(id);
        List<String> outList = outMap.get(id);

        if (dateMap.containsKey(id)) {
            //display header
            System.out.println("-".repeat(100));
            System.out.printf("%-14s %-9s %-9s", "Date", "In", "Out");
            System.out.println();
            for (int i = 0; i < dateList.toArray().length; i++) {
                System.out.printf("%-14s %-9s %-9s", dateList.get(i), inList.get(i), outList.get(i));
                System.out.println();
            }
        }
    }


    static Map<String, HashMap<Integer, String>> parseEmployeeData() throws IOException {
        String infoFile = "src\\data_info.csv";
        BufferedReader infoReader = new BufferedReader(new FileReader(infoFile));
        infoReader.readLine(); //skip header

        //Stores chosen columns in a hashmap
        HashMap<Integer, String> employees = new HashMap<>();
        HashMap<Integer, String> employeesHourlyRate = new HashMap<>();
        HashMap<Integer, String> employeesNames = new HashMap<>();
        HashMap<Integer, String> employeeBirthdays = new HashMap<>();

        String columnRead;
        while ((columnRead = infoReader.readLine()) != null) {
            String[] infoRow = columnRead.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            int id = Integer.parseInt(infoRow[0]);

            String name = infoRow[1] + " " + infoRow[2];
            String birthday = infoRow[3];
            String formatted = String.format("%-25s %-25s", name, birthday);

            employees.put(id, formatted);
            employeesHourlyRate.put(id, infoRow[18]);
            employeesNames.put(id, name);
            employeeBirthdays.put(id, birthday);
        }

        //Store columns required in a Map and store it
        Map<String, HashMap<Integer, String>> employeeData = new HashMap<>();
        employeeData.put("employees", employees);
        employeeData.put("hourlyRate", employeesHourlyRate);
        employeeData.put("names", employeesNames);
        employeeData.put("birthdays", employeeBirthdays);

        return employeeData;
    }

    static Map<String, HashMap<Integer, List<String>>> parseEmployeeAttendance() throws FileNotFoundException, IOException {
        //Provide directory for path and set up BufferedReader
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
            for (int i = 0; i < sssRow.length; i++) {
                sssRow[i] = sssRow[i].replace("\"", "").replace(",", "");
            }
            //Add data to lists
            minRange.add(sssRow[0]);
            maxRange.add(sssRow[2]);
            contribution.add(sssRow[3]);
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
}