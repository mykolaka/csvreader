package com.company.csvreader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Main {

    static final String fileSeparator = FileSystems.getDefault().getSeparator();

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String s;
        do {
            System.out.println("Please enter path to directory with csv files or enter to use directory by default (for exit type exit =)");
            s = in.nextLine();
            if (s.isBlank()) {
                s = new File("").getAbsolutePath() + fileSeparator + "src" + fileSeparator + "resources";
            }
            File directory = new File(s);
            if (directory.exists() && directory.isDirectory()) {
                Long currentMillis = System.currentTimeMillis();
                File outputDir = prepareOutputDirectory(directory.getAbsolutePath() + fileSeparator + "output");
                AtomicInteger ordinal = new AtomicInteger(0);
                Stream.of(directory.list()).parallel().forEach(fileName -> {
                    if (isCSV(fileName)) {
                        ordinal.incrementAndGet();
                        dealWithFile(new File(directory.getAbsolutePath() + fileSeparator + fileName), outputDir);
                    }
                });
                System.out.println("This takes: " + (System.currentTimeMillis() - currentMillis) + " milliseconds. It was " + ordinal.get() + " csv files consumed. Results in " + outputDir.getAbsolutePath() + " folder");
            } else {
                System.out.println("No such directory(" + directory.getAbsolutePath() + ")found");
            }
        } while (!s.equalsIgnoreCase("exit"));

    }

    private static boolean isCSV(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".csv") ? true : false;
    }

    private static File prepareOutputDirectory(String outputDirectoryPath) {
        File outputDirectory = new File(outputDirectoryPath);
        if (outputDirectory.exists()) {
            Stream.of(outputDirectory.listFiles()).parallel().forEach(file -> file.delete());
        } else {
            outputDirectory.mkdir();
        }
        return outputDirectory;
    }

    private static void dealWithFile(File file, File outputDirectory) {
        if (file.exists() && file.isFile()) {
            try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {
                List<String> result = new ArrayList<>();
                stream.parallel().forEach(fileLine -> {
                    if (Arrays.stream(fileLine.split(",")).anyMatch(part -> checkIsCardNumber(part))) {
                        result.add(fileLine);
                    }
                });
                if (result.size() > 0) {
                    Path outputFile = Paths.get(outputDirectory.getAbsolutePath() + fileSeparator + "output-" + file.getName());
                    Files.write(outputFile, result);
                }
            } catch (IOException ex) {
                System.out.println("Dealing with file went wrong");
            }
        }
    }

    private static boolean checkIsCardNumber(String cardNumber) {
        boolean result = false;
        if (cardNumber != null && cardNumber.length() == 16 && cardNumber.matches("\\d+")) {
            result = luhnCheck(cardNumber);
        }
        return result;
    }

    //Algorithm for checking card number
    private static boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = n - 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
