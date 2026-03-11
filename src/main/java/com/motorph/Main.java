package com.motorph;

import java.io.*;
import java.security.Key;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

//TODO: add automatic calculations

public class Main {

    private static List<String> sssCMinRange = new ArrayList<>();
    private static List<String> sssCMaxRange = new ArrayList<>();
    private static List<String> sssContribution = new ArrayList<>();

    public static void main(String[] args) {
        //CSV Files
        String attendanceFile = "src\\data_attendance.csv";
        String infoFile = "src\\data_info.csv";
        String sssCFile = "src\\sss_contribution.csv";
        String attendanceLine;
        String infoLine;
        String sssCLine;
        String[] infoRow = {};
        String[] attendanceRow = {};
        String[] sssCRow = {};

        BufferedReader attendanceReader = null;
        BufferedReader infoReader = null;
        BufferedReader sssCReader = null;

        Scanner sc = new Scanner(System.in);
        String input;

        boolean appRunning = true;
        boolean inEmployees = false;

        //Declaring ArrayList for every chosen column
        List<Integer> ids = new ArrayList<>();
        List<String> infos = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> birthdays = new ArrayList<>();
        List<String> hourlyRate = new ArrayList<>();

        List<String> dateList;
        List<String> inList;
        List<String> outList;

        HashMap<Integer, List<String>> date = new HashMap<>();
        HashMap<Integer, List<String>> in = new HashMap<>();
        HashMap<Integer, List<String>> out = new HashMap<>();

        //Get the data
        try {
            attendanceReader = new BufferedReader(new FileReader(attendanceFile));
            infoReader = new BufferedReader(new FileReader(infoFile));
            sssCReader = new BufferedReader(new FileReader(sssCFile));

            infoReader.readLine(); // skip header
            //assign the row and read the next line every iteration
            while (((infoLine = infoReader.readLine()) != null)) {
                //split the string into an array
                infoRow = infoLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); //new Array every iteration

                //store ids in arraylist every iteration
                ids.add(Integer.parseInt(infoRow[0])); //convert to int

                //store infos in arraylist every iteration
                String name = infoRow[1] + " " + infoRow[2];
                String formatted = String.format("%-25s %-25s", name, infoRow[3]);
                infos.add(formatted);

                //store hourly rate, name and birthday separately
                hourlyRate.add(infoRow[18]);
                names.add(name);
                birthdays.add(infoRow[3]);
            }

            attendanceReader.readLine(); // skip header
            while ((attendanceLine = attendanceReader.readLine()) != null) {
                attendanceRow = attendanceLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                int id = Integer.parseInt(attendanceRow[0]);
                String attendanceDate = attendanceRow[3];
                String attendanceIn = attendanceRow[4];
                String attendanceOut = attendanceRow[5];

                //if id doesn't exist, create a new list, if it does exist then ignore
                date.putIfAbsent(id, new ArrayList<>());
                in.putIfAbsent(id, new ArrayList<>());
                out.putIfAbsent(id, new ArrayList<>());
                //the csv file is sorted with the id.
                //the csv file contain multiple identical id, so using putifabsent, the creation of a new list will be prevented if the id exist.
                //while the id is still the same in the loop, the same list will be used.
                //once a new id appear, a list will be replaced with a new one.

                //store the date to the list for that ID
                date.get(id).add(attendanceDate);
                in.get(id).add(attendanceIn);
                out.get(id).add(attendanceOut);
            }
            sssCReader.readLine(); // skip header
            sssCReader.readLine(); // skip header
            while ((sssCLine = sssCReader.readLine()) != null) {
                sssCRow = sssCLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                for (int i = 0; i < sssCRow.length; i++) {
                    // Remove quotes and commas
                    sssCRow[i] = sssCRow[i].replace("\"", "").replace(",", "");
                }

                sssCMinRange.add(sssCRow[0]);
                sssCMaxRange.add(sssCRow[2]);
                sssContribution.add(sssCRow[3]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                attendanceReader.close();
                infoReader.close();
                sssCReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //store ids as keys and infos as value in hashmap
        HashMap<Integer, String> employees = new HashMap<>();
        HashMap<Integer, String> employeesHourlyRate = new HashMap<>();
        HashMap<Integer, String> employeesNames = new HashMap<>();
        HashMap<Integer, String> employeesBirthdays = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            employees.put(ids.get(i), infos.get(i));
            employeesHourlyRate.put(ids.get(i), hourlyRate.get(i));
            employeesNames.put(ids.get(i), names.get(i));
            employeesBirthdays.put(ids.get(i), birthdays.get(i));
        }
        //After data has been stored and organised output logo and all employee data in desired format
        IntroLogo(employees);

        do {
            System.out.println("-".repeat(100));
            //input line
            System.out.print("Enter Employee's ID: ");
            input = sc.next();

            //start of de-nestify project
            //checks if it's not an integer
            if (!isInteger(input)) {
                System.out.println("Please input a number.");
                continue;
            }

            //checks if input is a valid id number
            if (!employees.containsKey(Integer.parseInt(input))) {
                System.out.println("ID not found in employees");
                continue;
            }


            //this should probably be a method to be honest
            int id = Integer.parseInt(input); // convert input to int
            double HR = Double.parseDouble(employeesHourlyRate.get(id));
            int week = 0;
            long totalSeconds = 0;
            long weeklySeconds = 0;

            // get the list for the specific id
            dateList = date.get(id);
            inList = in.get(id);
            outList = out.get(id);

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

            //could be a method here.
            //already did a check for if it didn't containsKey(id) earlier.
            // if (employees.containsKey(id)) {
            String alert = "";
            do {
                inEmployees = true;
                if (!employeesNames.containsKey(id) || !employeesBirthdays.containsKey(id)) {
                    System.out.println("ID seems to not match with either the Name or the Birthday.");
                    continue; //p sure, sleepy rn
                }

                //note i'm going to need to fill parameters up for this
                //TODO: put all the parameters for displayEmployeeData in
                //TODO: figure out if if line 195 is redundant to line 157
                displayEmployeeData();
                //not sure if alert belongs in the method or not
                System.out.println(alert);
                //end method

                input = sc.next().toLowerCase();

                if (!(input.length() == 1)) {
                    System.out.println("Input too long.");
                    continue;
                }

                switch (input) {
                    case "g":
                        System.out.println("worketh");
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
                                week++;
                                System.out.println("Week " + week);
                                System.out.println("Weekly Gross: " + weekGross);
                                System.out.println("Weekly Net Salary: " + netGrossSalaryCalculator(weekGross));
                                System.out.println("Hours in the Week: " + secondsToTime(weeklySeconds));
                                System.out.println("-".repeat(20));
                                weeklySeconds = 0; //reset weeklyseconds
                            }
                        }
                        break;
                    case "a":
                        if (date.containsKey(id)) {
                            //display header
                            System.out.printf("%-14s %-9s %-9s", "Date", "In", "Out");
                            System.out.println();
                            for (int i = 0; i < dateList.toArray().length; i++) {
                                System.out.printf("%-14s %-9s %-9s", dateList.get(i), inList.get(i), outList.get(i));
                                System.out.println();
                            }
                        }
                        break;
                    case "e":
                        inEmployees = false;
                        alert = "";
                        IntroLogo(employees);
                        break;
                    default:
                        alert = "Please input either \"g\", \"a\" or \"e\".";
                        continue;
                }
                //end method
            } while (inEmployees == true);
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

    static double netGrossSalaryCalculator(double weeklyGross) {

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

            if (monthlyGross > min && monthlyGross <= max) {
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

    static void IntroLogo(HashMap<Integer, String> employees) {
        System.out.flush();
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
    }

    public static void displayEmployeeData(int id, HashMap<Integer, String> employeesNames, HashMap<Integer, String> employeesBirthdays, long totalSeconds, double HR){
        System.out.println("-".repeat(100));
        System.out.println("ID: " + id);
        System.out.println("Name: " + employeesNames.get(id));
        System.out.println("Birthday: " + employeesBirthdays.get(id));
        System.out.println("Total Hours: " + secondsToTime(totalSeconds));
        System.out.println("Total Gross Salary: " + grossSalaryCalculator(totalSeconds, HR));

        System.out.println("-".repeat(100));
        System.out.println("Enter g to display gross salary per week.");
        System.out.println("Enter a to show attendance.");
        System.out.println("Enter e to go back.");
    }
}