package cz.cuni.mff.d3s.blood.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Utility methods for dumping.
 */
public final class Dumping {

    /**
     * Disabling creation of instances of this class.
     *
     * @throws UnsupportedOperationException always
     */
    private Dumping() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    /**
     * File suffixes that can be removed from the arguments.
     */
    public static final String[] SUFFIXES = {".java", ".class", ".jar"};

    /**
     * Name of the dumps directory. Relative to PWD. Without trailing slash.
     */
    public static final String DUMPS_DIR_NAME = "dumps";

    /**
     * Extracts name of currently running application from the UN*X commandline.
     *
     * @return name of the application with its positional arguments joined with
     * underscore ("_")
     */
    public static String getTestName() {
        //Pattern argPattern = Pattern.compile("\u0000");
        StringBuilder testName = new StringBuilder();

        try (Scanner cmdlineScanner = new Scanner(new File("/proc/self/cmdline")).useDelimiter("\u0000")) {
            cmdlineScanner.next(); // ignore java command
            while (cmdlineScanner.hasNext()) {
                String arg = cmdlineScanner.next();
                if (arg.startsWith("-")) {
                    continue; // ignore option arguments
                }                    // strip suffix, if any
                for (String suffix : SUFFIXES) {
                    if (arg.endsWith(suffix)) {
                        arg = arg.substring(0, arg.length() - suffix.length());
                        break; // strip no more than one suffix
                    }
                }
                arg = arg.substring(arg.lastIndexOf('/') + 1); // use only base name
                testName.append(arg);
                testName.append('_');
            }
        } catch (IOException ex) {
            System.err.println("Error reading /proc/self/cmdline.");
            System.err.println("Note that this is UN*X-specific.");
            System.err.println("Falling back to \"unknown\" (sic).");
            return "unknown";
        }

        testName.deleteCharAt(testName.length() - 1); // delete the last underscore ("_")
        return testName.toString();
    }

    /**
     * Returns current date and time as string in format to be used in dump
     * names.
     *
     * @return ISO-8601-formatted local date and time, such as
     * 2011-12-03T10:15:30
     */
    public static final String getDateString() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now().withNano(0));
    }

    /**
     * Constructs name of the file that is to be put in the dumps directory.
     *
     * @param type type of the dumped data, used as suffix
     * @return the name without any directories
     */
    public static final String getDumpBaseFileName(String type) {
        return getTestName() + "." + getDateString() + "." + type;
    }

    /**
     * Creates the dump file using the name created by
     * {@link #getDumpBaseFileName(java.lang.String)} in directory
     * {@link #DUMPS_DIR_NAME}
     *
     * @param type type of the dumped data, used as suffix
     * @return an OutputStreamWriter to the file
     * @throws IOException if thrown when creating or opening the file
     */
    public static final Writer getDumpFileWriter(String type) throws IOException {
        File dumpsDirectory = new File(DUMPS_DIR_NAME);
        dumpsDirectory.mkdir();
        File dumpFile = new File(dumpsDirectory, getDumpBaseFileName(type));
        System.out.println(dumpFile.getName());
        dumpFile.createNewFile();
        return new OutputStreamWriter(new FileOutputStream(dumpFile));
    }
}
