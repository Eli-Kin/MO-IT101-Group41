package com.netsalarycalculator;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static List<String> sssCMinRange = new ArrayList<>();
    private static List<String> sssCMaxRange = new ArrayList<>();
    private static List<String> sssContribution = new ArrayList<>();

    public static void main(String[] args) {

        //CSV Files
        String sssCFile = "src\\sss_contribution.csv";
        String sssCLine;
        String[] sssCRow = {};
        BufferedReader sssCReader = null;

        Scanner sc = new Scanner(System.in);
        String input;
        boolean running = true;
        boolean calculatingHours = true;
        boolean calculatingMinutes = true;


        //Get the data
        try {
            sssCReader = new BufferedReader(new FileReader(sssCFile));

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
                sssCReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        do {
            double netSalary = 0;
            double gross = 0;
            int hourlyRate = 0;
            int result = 0;
            int hours = 0;
            int minutes = 0;

            do {
                System.out.println("Enter Hours: ");
                input = sc.next();
                if (isInteger(input)){
                    hours = Integer.parseInt(input);
                    calculatingHours = false;
                }
                else {
                    System.out.println("Please only input numbers.");
                }
            } while(calculatingHours);

            do {
                System.out.println("Enter Minutes: ");
                input = sc.next();

                if (!isInteger(input)) {
                    System.out.println("Please only input numbers.");
                }
                else {
                    if (Integer.parseInt(input) > 60) {
                        System.out.println("Only input 1 to 60 minutes");
                    } else {
                        minutes = Integer.parseInt(input);
                        calculatingMinutes = false;
                    }
                }

            } while (calculatingMinutes);

            result = hourToSeconds(hours, minutes);

//            System.out.println("Hours: " + hours);
//            System.out.println("Minutes: " + minutes);
//            System.out.println("Result: " + result);

            System.out.println("Enter Hourly Rate: ");
            input = sc.next();
            hourlyRate = Integer.parseInt(input);

            gross = grossSalaryCalculator(hourToSeconds(hours, minutes), hourlyRate);
            netSalary = netGrossSalaryCalculator(gross);

            System.out.println("Gross: " + gross);
            System.out.println("Net Salary: " + netSalary);

        } while (running);

    }
    static int hourToSeconds(int hour, int minute) {
        int hourResult = hour * 60 * 60;
        int minuteResult = minute * 60;

        return hourResult + minuteResult;
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

    static double grossSalaryCalculator(long seconds, double hourlyRate) {
        return (seconds / 3600.0) * hourlyRate;
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
            taxRate = 0.20; excess = 20833;
            withholdingTaxMonthly = (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 33333 && monthlyGross <= 66667) {
            taxRate = 0.25; excess = 33333; plus = 2500;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 66667 && monthlyGross <= 166667) {
            taxRate = 0.30; excess = 66667; plus = 10833;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 166667 && monthlyGross <= 666667) {
            taxRate = 0.32; excess = 166667; plus = 40833.33;
            withholdingTaxMonthly = plus + (monthlyGross - excess) * taxRate;
        } else if (monthlyGross > 666667) {
            taxRate = 0.35; excess = 666667; plus = 200833.33;
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
}