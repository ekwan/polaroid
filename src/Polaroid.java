import java.util.*;
import java.io.*;
import java.net.*;

/** Sends commands to IGV to make PNG snapshots of the given genomic coordinates. */
public class Polaroid {

    /** Where to read the configuration data from. */
    public static final String CONFIGURATION_FILENAME = "polaroid.config";

    /** Which BAM files to snapshot. */
    public static final List<String> BAM_FILENAMES = new ArrayList<>();

    /** The directory to output the PNGs to. */
    public static final String SNAPSHOT_DIRECTORY;

    /** The locations to snapshot. */
    public static final List<String> LOCATIONS = new ArrayList<>();

    /** The IP address IGV is at. */
    public static final String IGV_IP;

    /** The port IGV is at. */
    public static final int IGV_PORT;

    /** How long to wait between locations in seconds. */
    public static final int DELAY;

    /** Loads the configuration data. */
    static {
        // read configuration file
        String tempSnapshotDirectory = null;
        String tempIGV_IP = null;
        int tempIGV_PORT = -1;
        int tempDelay = -1;

        try (Scanner scanner = new Scanner(new File(CONFIGURATION_FILENAME)) ) {
            while (scanner.hasNextLine()) {
                // read line and tokenize
                String line = scanner.nextLine().trim();
                if ( line.startsWith("#") || line.length() == 0 )
                    continue;
                String[] fields = line.split("\\s+");

                // parse
                if ( fields.length == 1 ) {
                    if ( ! line.toLowerCase().startsWith("chr") )
                        throw new IllegalArgumentException(String.format("unrecognized location: %s", line));
                    LOCATIONS.add(fields[0]);
                }
                else if ( fields.length == 2) {
                    String fieldName = fields[0].toLowerCase();
                    String fieldValue = fields[1];
                    if ( fieldName.equals("bam") )
                        BAM_FILENAMES.add(fieldValue);
                    else if ( fieldName.equals("snapshot_directory") )
                        tempSnapshotDirectory = fieldValue;
                    else if ( fieldName.equals("igv_ip") )
                        tempIGV_IP = fieldValue;
                    else if ( fieldName.equals("igv_port") )
                        tempIGV_PORT = Integer.parseInt(fieldValue);
                    else if ( fieldName.equals("delay") )
                        tempDelay = Integer.parseInt(fieldValue);
                    else
                        System.out.printf("Warning: ignoring unrecognized configuration line:\n%s\n", line);
                }
                else
                    System.out.printf("Warning: ignoring unexpected number of fields on configuration line:\n%s\n", line);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // check everything has been read
        if ( tempSnapshotDirectory == null )
            throw new NullPointerException("didn't read snapshot directory from the configuration file");
        else if ( tempIGV_IP == null )
            throw new NullPointerException("didn't read the IGV IP address from the configuration file");
        else if ( tempIGV_PORT < 0 )
            throw new NullPointerException("invalid IGV port from the configuration file");
        else if ( tempDelay < 0 )
            throw new IllegalArgumentException("invalid delay");

        // check file locations are valid
        if ( BAM_FILENAMES.size() == 0 )
            throw new IllegalArgumentException("must specify some BAM files to read in the configuration file");
        for (String filename : BAM_FILENAMES) {
            if ( ! filename.toLowerCase().endsWith(".bam") )
                throw new IllegalArgumentException(String.format("invalid file extension for %s", filename));
            File file = new File(filename);
            if ( ! file.isFile() )
                throw new IllegalArgumentException(String.format("invalid BAM file, check filename: %s", filename));
        }

        // check genomic locations are valid
        if ( LOCATIONS.size() == 0 )
            throw new IllegalArgumentException("must specify some genomic coordinates in the configuration file");

        // check snapshot directory is valid
        if ( ! new File(tempSnapshotDirectory).isDirectory() )
            throw new IllegalArgumentException(String.format("check snapshot directory %s", tempSnapshotDirectory));

        // set fields
        SNAPSHOT_DIRECTORY = tempSnapshotDirectory;
        IGV_IP = tempIGV_IP;
        IGV_PORT = tempIGV_PORT;
        DELAY = tempDelay;
    }

    /** Singleton. */
    private Polaroid() {
        throw new IllegalArgumentException("not instantiable");
    }
    
    /** Sends commands to IGV. */
    public static void main(String[] args) {
        // open connection to IGV
        System.out.printf("Opening a socket to IGV (%s:%d)...\n", IGV_IP, IGV_PORT);
        try (Socket socket = new Socket(IGV_IP, IGV_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             ) {
            System.out.println("Connection established.");

            // get a blank slate
            send(out, in, "new");

            // load BAM files
            for (String filename : BAM_FILENAMES) {
                String command = String.format("load %s", filename);
                send(out, in, command);
                //wait(DELAY);
            }

            // set snapshot directory and clear it
            {
                String command = String.format("snapshotDirectory %s", SNAPSHOT_DIRECTORY);
                send(out, in, command);
                
                System.out.printf("\nClearing all .png files in %s/...\n", SNAPSHOT_DIRECTORY);
                File folder = new File(SNAPSHOT_DIRECTORY);
                for (File f : folder.listFiles()) {
                    if ( f.getName().endsWith(".png") )
                        f.delete();
                }
                System.out.println("Cleared.");
            }

            // snapshot each file
            for (int i=0; i < LOCATIONS.size(); i++) {
                String location = LOCATIONS.get(i);
                System.out.printf("\nLocation %d of %d:\n", i+1, LOCATIONS.size());
                String command = String.format("goto %s", location);
                send(out, in, command);
                wait(DELAY);
                if ( i == 0 )
                    send(out, in, "collapse");
                command = String.format("sort base %s", location);
                send(out, in, command);
                wait(DELAY);
                String filename = String.format("%03d_%s", i+1, location).replaceAll(":","_");
                command = String.format("snapshot %s", filename);
                send(out, in, command);
                wait(DELAY);
            }
	    }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.printf("\nDone!  Wrote %d snapshots to %s/.\n", LOCATIONS.size(), SNAPSHOT_DIRECTORY);
    }

    /**
     * Sends a command to IGV and prints out the response to stdout.
     * @param out the stream going to IGV
     * @param in the stream coming from IGV
     * @param command the command to send
     * @return the response from IGV
     */
    private static String send(PrintWriter out, BufferedReader in, String command) throws IOException {
        System.out.printf("> %s\n", command);
        out.println(command);
        String response = in.readLine();
        if ( !response.equals("OK") ) {
            System.out.println(response);
            throw new IllegalArgumentException("unexpected response from IGV");
        }
        return response;
    }

    /**
     * Waits.
     * @param seconds how long to wait
     */
    private static void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e) {}
    }
}

