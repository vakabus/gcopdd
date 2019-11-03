package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Utility methods for dumping.
 */
public final class DumpHelpers {

    /**
     * File suffixes that can be removed from the arguments.
     */
    public static final String[] SUFFIXES = {".java", ".class", ".jar"};
    /**
     * Default name of the dumps directory. Relative to PWD. Without trailing slash.
     */
    public static final String DEFAULT_DUMPS_DIR_NAME = "dumps";
    private static File cachedReportDir = null;

    /**
     * Disabling creation of instances of this class.
     *
     * @throws UnsupportedOperationException always
     */
    private DumpHelpers() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    /**
     * Extracts name of currently running application from the UN*X commandline.
     *
     * @return name of the application with its positional arguments with
     * non-alphanumeric characters replaced by underscores and joined with
     * underscore ("_")
     */
    public static String getTestName() {
        StringBuilder testName = new StringBuilder();
        Pattern unsafe = Pattern.compile("[^0-9A-Z_a-z]", Pattern.MULTILINE);

        try (Scanner cmdlineScanner = new Scanner(new File("/proc/self/cmdline")).useDelimiter("\u0000")) {
            cmdlineScanner.next(); // ignore java command
            while (cmdlineScanner.hasNext()) {
                String arg = cmdlineScanner.next();
                if (arg.startsWith("-")) {
                    continue; // ignore option arguments
                }
                // strip suffix, if any
                for (String suffix : SUFFIXES) {
                    if (arg.endsWith(suffix)) {
                        arg = arg.substring(0, arg.length() - suffix.length());
                        break; // strip no more than one suffix
                    }
                }
                arg = arg.substring(arg.lastIndexOf('/') + 1); // use only base name
                arg = unsafe.matcher(arg).replaceAll("_");
                testName.append(arg);
                testName.append('_');
            }
            testName.deleteCharAt(testName.length() - 1); // delete the last underscore ("_")
        } catch (IOException | StringIndexOutOfBoundsException ex) {
            System.err.println("Error reading /proc/self/cmdline. Falling back to \"unknown\" (sic).");
            testName.replace(0, testName.length(), "unknown");
        }

        return String.format(System.getProperty("blood.rename", "%s"), testName);
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
     * Constructs name of the subdirectory that is to be put in the dumps directory.
     *
     * @return the name of the directory without any parent directories or slashes
     */
    public static final String getReportDirBaseName() {
        return getDateString() + "." + getTestName();
    }

    public static final File getReportDir() {
        if (cachedReportDir != null) {
            return cachedReportDir;
        }

        File dumpDir = new File(System.getProperty("blood.dumpsdir", DEFAULT_DUMPS_DIR_NAME));
        cachedReportDir = new File(dumpDir, getReportDirBaseName());
        cachedReportDir.mkdirs();
        return cachedReportDir;
    }

    public static final NtarOutputStream createDumpFile(File reportDir, String id) throws IOException {
        File dumpFile = new File(reportDir, id);

        // make sure the filename is available
        if (!dumpFile.createNewFile()) {
            throw new RuntimeException("File name is not available: " + dumpFile.getPath());
        }

        return new NtarOutputStream(new FileOutputStream(dumpFile));
    }
}
