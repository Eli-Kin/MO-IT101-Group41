package com.netsalarycalculator;

import java.util.Scanner;

public class WeeklyHourTracker {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        String[] days = {
                "Monday","Tuesday","Wednesday",
                "Thursday","Friday","Saturday","Sunday"
        };

        double[] hoursWorked = new double[7];
        double totalHours = 0;

        System.out.println("==== Weekly Hour Tracker ====");

        for(int i=0;i<days.length;i++){
            System.out.print("Enter hours for " + days[i] + ": ");
            hoursWorked[i] = sc.nextDouble();
            totalHours += hoursWorked[i];
        }

        System.out.print("Enter hourly rate: ");
        double hourlyRate = sc.nextDouble();

        double weeklyPay = totalHours * hourlyRate;

        System.out.println("\nWeekly Summary");

        for(int i=0;i<days.length;i++){
            System.out.println(days[i] + ": " + hoursWorked[i] + " hrs");
        }

        System.out.println("Total Hours: " + totalHours);
        System.out.println("Weekly Pay: " + weeklyPay);
    }
}
