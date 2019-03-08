package com.ift.toolchain.configuration;

import org.orekit.data.DataProvidersManager;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class Autoconfiguration {

    /** This is a utility class so its constructor is private.
     */
    private Autoconfiguration() {
    }
    /** Configure the library.
     * <p>Several configuration components are used here. They have been
     * chosen in order to simplify running the tutorials in either a
     * user home or local environment or in the development environment.
     *   <ul>
     *     <li>use a "orekit-data.zip" directory in current directory</li>
     *     <li>use a "orekit-data" directory in current directory</li>
     *     <li>use a ".orekit-data" directory in current directory</li>
     *     <li>use a "orekit-data.zip" directory in user home directory</li>
     *     <li>use a "orekit-data" directory in user home directory</li>
     *     <li>use a ".orekit-data" directory in user home directory</li>
     *     <li>use the "regular-data" directory from the test resources</li>
     *   </ul>
     * </p>
     */
    public static void configureOrekit() {
        final File home    = new File(System.getProperty("user.home"));
        final File current = new File(System.getProperty("user.dir"));
        StringBuffer pathBuffer = new StringBuffer();
        appendIfExists(pathBuffer, new File(current, "orekit-data.zip"));
        appendIfExists(pathBuffer, new File(current, "orekit-data"));
        appendIfExists(pathBuffer, new File(current, ".orekit-data"));
        appendIfExists(pathBuffer, new File(home,    "orekit-data.zip"));
        appendIfExists(pathBuffer, new File(home,    "orekit-data"));
        appendIfExists(pathBuffer, new File(home,    ".orekit-data"));
//        appendIfExists(pathBuffer, "regular-data");
        appendIfExists(pathBuffer, "/var/upload/orekit-data.zip");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, pathBuffer.toString());
//        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, "/Users/lastcow_chen/tmp/orekit-data.zip");
//        System.out.println("==========" + pathBuffer.toString());
    }
    /** Append a directory/zip archive to the path if it exists.
     * @param path placeholder where to put the directory/zip archive
     * @param file file to try
     */
    private static void appendIfExists(final StringBuffer path, final File file) {
        if (file.exists() && (file.isDirectory() || file.getName().endsWith(".zip"))) {
            if (path.length() > 0) {
                path.append(System.getProperty("path.separator"));
            }
            path.append(file.getAbsolutePath());
        }
    }
    /** Append a classpath-related directory to the path if the directory exists.
     * @param path placeholder where to put the directory
     * @param directory directory to try
     */
    private static void appendIfExists(final StringBuffer path, final String directory) {
        try {
            final URL url = Autoconfiguration.class.getClassLoader().getResource(directory);
            if (url != null) {
                if (path.length() > 0) {
                    path.append(System.getProperty("path.separator"));
                }
                path.append(url.toURI().getPath());
            }
        } catch (URISyntaxException use) {
            // display an error message and simply ignore the path
            System.err.println(use.getLocalizedMessage());
        }
    }

}
