//Scorer for the Tone Monitoring Task
//using sensitivity index (d')
//will read and process .CSV files in the directory
//and return output.csv

/*
@author Juan M. Alzate Vanegas
@version 1.1
2-12-2018
TRG (UCF)
*/

//Package statement
package ToneMonitoringScorer;

//Import statements
import core.OutputLog;
import core.ProcessedData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.JOptionPane;


public class ToneMonitoringScorer {
	//Toggles
	private static final Boolean LOGGING = true;
        private static final Boolean MANUAL_LOGGING = true;
	private static final Boolean EXCLUDE_INVALID = true;
        private final static Boolean HEADER_ON = true;

	//Constants
        private static final String HEADER = "File,Participant,Hits,Misses,CorrectRejections,FalseAlarms,Sensitivity,FalseErrorRate,d',%Accuracy,Average RT";     //Output file header
	private static final int HEADER_LINES = 17;	//# of lines for the header, until reaching data
	private static final String VALIDATION_CHECK = "*Experiment Finished Normally";	//Check for successful trial
	public static final int TRIAL_INTERVAL = 4;	//Count every TRIAL_INTERVALth trial
	private static final double DELTA = 0.00000001; //Threshold for calculating inverseNorm
	private static final int SD_THRESHOLD = 8;      //Threshold for calculating inverseNorm
        private static final int MAX_LINES = 150;       //Maximum number of lines for a .CSV input file
        private static final int N_COLS = 5;            //# of columns in a participant file
        public static final int NUM_TRIALS = 99;        //Number of trials 
        public static final int NUM_TONES = 3;          //Number of available tones
        public static final int ZERO_RATE_METHOD = 2;  //Method for accounting for 0 or 100% sensitivity/false error
                                                       //1: Replace rate with 1 - (1/2n) for 100%, 1/2n for 0% (MacMillan & Kaplan, 1985)
                                                       //2: Loglinear method (Hautus, 1995)
        public static final int DPRIME_FORMULA = 1;   //Formula for calculating d'
                                                      //1: d' (Homogeneity of variances assumed)
                                                      //2: da (Unequal variances assumed)
        public static final Boolean POOL_VARIANCE = false;  //Whether or not to pool variances
        
	//Fields
	private static String[][] rawData;              //Array of raw data
        private static ArrayList<String> missingFiles;
	private static ProcessedData processedData;	//Processed data
	private static String participantNumber;		//Participant number

	//Messages
	private static final String[] ERRORS = {"Error reading file: ",
                                                "File skipped: experiment did not finish normally.",
                                                "Failed to produce output file.",
                                                "Failed to log properly.",
                                                "File skipped: file is not a .csv.",
                                                "File skipped: number of lines is too long! [",
                                                "Error: File was corrupted at line ",
                                                "Error: There is already a",
                                                "Coding terminated unexpectedly.",
                                                "File skipped: file is probably not a raw data file."};
        private static final String STARTING_MESSAGE = "Initiating!";
        private static final String PADDING = "****";
	private static ArrayList<String> LOGS;
        
        //Timing
        private static long programStartTime;
	private static long startTime;
	private static long endTime;

	//Program execution
	public static void main(String Args[]) {
            //Initiate
             init();
             
            //Read in directory
            String directory = System.getProperty("user.dir");
            log("Opening directory: " + directory + "\n");
            File folder = new File(directory);
            File[] listOfFiles = folder.listFiles();

            //Read each file
            List <OutputLog> unsortedResults = new ArrayList<OutputLog>();
                
	    for (int i = 0; i < listOfFiles.length; i++) {
	    	//Only read .CSV files
                log("Reading directory file [" + (i + 1) + "]: "  + listOfFiles[i].getName().toLowerCase());
	        if (listOfFiles[i].isFile() && listOfFiles[i].getName().toLowerCase().endsWith(".csv")) {
	        	//Start timing process
	        	startTime = System.currentTimeMillis();
                        
                        //Found participant file
                        log("\tFound a .csv file!");
	            //Process the file
	           if (processFile(listOfFiles[i])) {
	           		//Add to output file
	           		unsortedResults.add(new OutputLog(listOfFiles[i].getName().toLowerCase(), participantNumber, processedData));
		       		//Time the end
                                endTime = System.currentTimeMillis();
	           		log("\tSuccess! [" + (endTime - startTime) + " ms]");                 
	           } 
                   //File was not successfully processed
                    else missingFiles.add(listOfFiles[i].getName().toLowerCase());
                  //Failure reading file
	        } else {
                    //Log error
                    log("\t" + ERRORS[4]);
                    //Add to list of non-processed files
                    missingFiles.add(listOfFiles[i].getName().toLowerCase());
                }
                
                log("\n");
	    }           
            //Print out missing files and results
            printMissingFiles();
            
            //Print reuslts
            if (!printResults(unsortedResults)) { 
                log("\n" + ERRORS[8]);
                    //Pop-up error
                    JOptionPane.showMessageDialog(null, ERRORS[8]);
            }
        }

	//Methods
        public static void init() {
            //Initiate logging
            LOGS = new ArrayList<>();
            log(PADDING + PADDING + PADDING + "INITIALIZING" + PADDING + PADDING + PADDING);
            programStartTime = System.currentTimeMillis();
            
            //Pop-up start message
             JOptionPane.showMessageDialog(null, STARTING_MESSAGE);
            
            //Allocate memory for arrays
            rawData = new String[NUM_TRIALS][N_COLS];
            missingFiles = new ArrayList<>();            
        }
        
        public static Boolean printResults(List<OutputLog> unsortedResults) {     
                //Sort results by participant number
                Collections.sort(unsortedResults);
            
                //Add date
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm");
                Date date = new Date();
                           
                //Output .CSV
                log(PADDING + "PREPARING OUTPUT FILE" + PADDING);
            try {
                File f = new File("[OUTPUT] " + sdf.format(date) + ".csv");
                if(f.exists()) {
                    log(ERRORS[7] + "n output file. Modify the old file and try again.");
                    return false;
                }
                
                    //Print header
                    try (PrintWriter outputWriter = new PrintWriter("[OUTPUT] " + sdf.format(date) + ".csv")) {
                        //Print header
                        if(HEADER_ON) outputWriter.write(HEADER + "\n");
                        
                        for(int i = 0; i < unsortedResults.size(); i++) {
                            if((unsortedResults.get(i) != null) && !unsortedResults.get(i).toString().isEmpty()) outputWriter.println(unsortedResults.get(i).toString()+"");
                        }
                        
                        outputWriter.flush();
                    }  
                
            } catch (IOException ex) {
                //Log failure
                ex.printStackTrace();
                log(ERRORS[2]);
                log("\n");
            }
            
            //Manual logging of results
            if(MANUAL_LOGGING) {
                //Print header
                if(HEADER_ON) log(HEADER + "");
                //Print out results
                for (int i =  0; i < unsortedResults.size(); i++) {
                    if((unsortedResults.get(i) != null) && !unsortedResults.get(i).toString().isEmpty()) log(unsortedResults.get(i).toString());
                }
                log("\n");
            }
            
            //Output log
            long programEndTime = System.currentTimeMillis();
            if(LOGGING) {
                try {                    
                    log(PADDING + "PREPARING LOG FILE [Time elapsed: " + (programEndTime - programStartTime) + " ms]" + PADDING);
                    
                    File f = new File("[LOG] " + sdf.format(date) + ".log");
                    if(f.exists()) {
                        log(ERRORS[7] + " log file. Modify the old file and try again.");
                        return false;
                    }
                    try (PrintWriter logWriter = new PrintWriter("[LOG] " + sdf.format(date) + ".log")) {
                        LOGS.stream().forEach((log) -> {
                            logWriter.println(log);
                        });
                        
                        //Successful
                        logWriter.println("Done!");
                        log("Done!");
                        
                         //Pop-up window
                         JOptionPane.showMessageDialog(null, "Done! [Time elapsed: " + (programEndTime - programStartTime) + " m.s.]");
                        
                        logWriter.flush();
                    } 
                } catch (IOException ex) {
                    //Log failure
                    ex.printStackTrace();
                    log(ERRORS[3]);
                }
            }
            
            //Success
            return true;
        }

	public static void log(String str) {
		if(LOGGING) {
                    System.out.println(str);
                    LOGS.add(str);
                }
	}
        
        public static void printMissingFiles() {
            //Do not do this if all files were processed
            if(missingFiles.size() < 1) return;
            
            //Log each missing file
            log("Note: the following files were not processed:");
            missingFiles.stream().forEach((str) -> {
                log(str);
            });
            
            //Add new line
            log("\n");
        }

	public static Boolean processFile(File file) {
		//Attempt to read filE
            try (Scanner inputStream = new Scanner(file)) {
                int line = 1;
                while(inputStream.hasNext()){
                    //Read line
                    String data = inputStream.nextLine();
                    
                    //Catch long files                    
                    if(line > MAX_LINES) {
                        log(ERRORS[5] + line + "]");
                        return false;
                    }

                    //Store participant number
                    if(line == 1) {
                        //Catch corrupt files
                        if(data == null || data.equals("")) return false;
                        String[] values = data.split(",");
                        if(!values[0].contains("Subject Name:")) { 
                            log("\t" + ERRORS[9]);
                            return false;
                        }
                        if(values[1] == null || values[1].equals("") || !values[1].matches(".*\\d+.*")) return false;
                        participantNumber = values[1];
                    }
                    
                    //Exclude files where experiment did not end correctly
                    if(line == 15) {
                        if (EXCLUDE_INVALID && !data.contains(VALIDATION_CHECK)) {
                            //Failed to process file
                            log("\t" + ERRORS[1]);
                            return false;
                        }
                    }
                    
                    //Do not read until after header
                    if (line > HEADER_LINES) {
                        //Split lines as .csv
                        String[] lines = data.split(",");
                        int j = 0;
                        for (String str : lines) {
                            if (!str.equals("")) {
                                //log("Trying to add [" + (line - (HEADER_LINES + 1) ) + ", " + j + "]: " + str);
                                rawData[line - (HEADER_LINES + 1)][j] = str;
                                
                                //Catch corrupt files
                                if((j == 1 && !str.contains("no response") && (Integer.parseInt(str) < 0)) || (j == 3 && !str.contains("NA") && Long.parseLong(str) < 0)) {
                                    log("\t" + ERRORS[6] + line + ".");
                                    //Failure to parse
                                    return false;
                                }   
                                j++;
                            }
                        }
                    }
                    //Go onto the next line in the file
                    line++;
                }
                //Convert to processed format
                processedData = new ProcessedData(rawData);
     
             //Success
             return true;
             
          } catch (FileNotFoundException e) {
            log(ERRORS[0] + file.getName());

            //Failure
            return false;
          }
	}

        /*
        Statistical functions
        @author: Evaluating the Normal Distribution by George Marsaglia
        */

        //Compute z such that cdf(z) = y via bisection search
        public static double inverseCDF(double y) {
            return inverseCDF(y, DELTA, (SD_THRESHOLD * - 1), SD_THRESHOLD);
        } 

        //Bisection search
        private static double inverseCDF(double y, double delta, double lo, double hi) {
            double mid = lo + (hi - lo) / 2;
            if (hi - lo < delta) return mid;
            if (cdf(mid) > y) return inverseCDF(y, delta, lo, mid);
            else              return inverseCDF(y, delta, mid, hi);
        }
        
        //Return cdf(z) = standard Gaussian cdf using Taylor approximation
        public static double cdf(double z) {
            if (z < (-1 * SD_THRESHOLD)) return 0.0;
            if (z >  SD_THRESHOLD) return 1.0;
            double sum = 0.0, term = z;
            for (int i = 3; sum + term != sum; i += 2) {
                sum  = sum + term;
                term = term * z * z / i;
            }
            return 0.5 + sum * pdf(z);
        }

        //Return cdf(z, mu, sigma) = Gaussian cdf with mean mu and stddev sigma
        public static double cdf(double z, double mu, double sigma) {
            return cdf((z - mu) / sigma);
        }
        
        //Return pdf(x) = standard Gaussian pdf
        public static double pdf(double x) {
            return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
        }

        //Return pdf(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
        public static double pdf(double x, double mu, double sigma) {
            return pdf((x - mu) / sigma) / sigma;
        }
}