package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Utility methods for dumping.
 */
public final class DumpHelpers {

    /**
     * File suffixes that can be removed from the arguments.
     */
    public static final String[] SUFFIXES = {".java", ".class", ".jar"};
    /**
     * Name of the dumps directory. Relative to PWD. Without trailing slash.
     */
    public static final String DUMPS_DIR_NAME = "dumps";
    private static final boolean ENABLE_DUMP_COMPRESSION = true;
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
    public static final String getReportDirBaseName(String type) {
        return getDateString() + "." + getTestName();
    }

    public static final File getReportDir() {
        if (cachedReportDir != null) {
            return cachedReportDir;
        }

        File dumpDir = new File(DUMPS_DIR_NAME);
        cachedReportDir = new File(dumpDir, getReportDirBaseName(DUMPS_DIR_NAME));
        cachedReportDir.mkdirs();
        return cachedReportDir;
    }

    public static final OutputStream createDumpFile(File reportDir, String dumpType, String id) throws IOException {
        if (ENABLE_DUMP_COMPRESSION)
            dumpType = dumpType + ".gz";

        File dumpFile = new File(getReportDir(), id + "." + dumpType);

        // make sure the filename is available
        if (!dumpFile.createNewFile()) {
            throw new RuntimeException("File name is not available: " + dumpFile.getPath());
        }

        if (ENABLE_DUMP_COMPRESSION) {
            return new GZIPOutputStream(new FileOutputStream(dumpFile));
        } else {
            return new FileOutputStream(dumpFile);
        }
    }
}
