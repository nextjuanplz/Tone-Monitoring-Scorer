/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import ToneMonitoringScorer.ToneMonitoringScorer;
import static ToneMonitoringScorer.ToneMonitoringScorer.TRIAL_INTERVAL;
import static ToneMonitoringScorer.ToneMonitoringScorer.ZERO_RATE_METHOD;
import static ToneMonitoringScorer.ToneMonitoringScorer.inverseCDF;
import static ToneMonitoringScorer.ToneMonitoringScorer.log;

/**
 *
 * @author nextjuanplz
 */
	public class ProcessedData {
		//Inputs
     		private int[] trialNumber;
		private String[] reactionTime; 
		private int[] tonePlayed;
		private String[] response;

		//Processed
		private int hits;
		private int misses;
		private int falseAlarms;
		private int correctRejections;

		//Scoring
		private int[] toneFrequency;
		private double sensitivity;
		private double falseErrorRate;
                private double sensN;
                private double falseErrorN;

		//Outputs
		private double dPrime;
                private double accuracy;
                private double averageRT;
                
                //Constants
                private static final int TONE_ERROR = -1;

		//Constructor
		public ProcessedData(String[][] list) {  
                        init();                        
			//Read all values
			for (int i = 0; i < list.length; i++) {
                                //log("Trying to record: " + list[i][0]);
                                if(list[i][0].equals("NA") || list[i][0].equals("no response")) continue;
				this.trialNumber[i] = Integer.parseInt(list[i][0]);
				this.reactionTime[i] = list[i][1];
                                if(list[i][2].equals("NA") || list[i][2].equals("no response")) continue;
				this.tonePlayed[i] = Integer.parseInt(list[i][2]);
				this.response[i] = list[i][3];
			}
                        
                        //Process values
                        processValues();
		}

		//Methods
                private void init() {
                    this.trialNumber = new int [ToneMonitoringScorer.NUM_TRIALS];
                    this.reactionTime = new String[ToneMonitoringScorer.NUM_TRIALS];
                    this.tonePlayed = new int [ToneMonitoringScorer.NUM_TRIALS];
                    this.response = new String[ToneMonitoringScorer.NUM_TRIALS];
                    this.toneFrequency = new int [ToneMonitoringScorer.NUM_TONES];
                }
                
		private void processValues() {
			averageTime();
			applySDT();
		}

		private void averageTime() {
			double sumTime = 0;
                        double instances = 0;
                        for (String str : reactionTime) {
                            //Do not count non-responses
                            if (str == null || str.contains("no response") || str.contains("NA")) continue;
                            
                            //Add times from string   
                            double currTime = Double.parseDouble(str);
                            if(currTime >= 0.0) { 
                                sumTime += currTime;
                                //Count as instance of response
                                instances++;
                            }
                        }

			//Update average RT
			this.averageRT = (sumTime / instances);
		}


		private void applySDT() {
                        //Init all frequencies
                        for (int i = 0; i < toneFrequency.length; i++) toneFrequency[i] = 0;
			//Process all trials
			for (int i = 0; i < trialNumber.length; i++) {                            
                                //Save current tone
                                int currentTone = tonePlayed[i] - 1;
                            
                                //Skip errors for now
                                if (currentTone == TONE_ERROR) continue;
                                
				//Increment frequency of tone played
				toneFrequency[currentTone]++;

				//Fourth time the tone is being played
				if (toneFrequency[currentTone] == TRIAL_INTERVAL) {
					//Check if response is correct
                                        //Missed response
                                        if(response[i].contains("NA")) {
                                            misses++;
                                            //Go onto the next one
                                            continue;
                                        }
                                                                                
                                        //Responded correctly                                        
                                        else if(Integer.parseInt(response[i]) == (currentTone)) hits++;

					//Otherwise, answered incorrectly; mark as wrong
					else misses++;
                                        
                                        //Reset frequency of current tone since
                                        //the participant has responded by now
					toneFrequency[currentTone] = 0;
				}

				//Every other trial
				else {

					//Participant correctly withheld responding
					if(response[i].equals("NA")) correctRejections++;
                                        
					//Otherwise, participant answered at inappropriate time
					else {
						//Reset frequency of current tone
						toneFrequency[currentTone] = 0;

						//Increment number of false alarms
						falseAlarms++; 
					}
				}
			}
                        //Calculate % accuracy
                        setAccuracy(((double) hits + (double) correctRejections) / ((double) hits + (double) misses + (double) falseAlarms + (double) correctRejections));
                        
			//Calculate d'
			calcDPrime();
                        
                        //Log results
                        logResults();
		}
                
                private void logResults() {
                        log("\t\t[Hits: " + hits + "] [Misses: " + misses + "] [Correct Rejections: " + correctRejections + "] [False Alarms: " + falseAlarms + "]");
                        log("\t\t[Sensitivity: " + sensitivity + "] [FalseErrorRate: " + falseErrorRate + "]");
                        log("\t\t[Accuracy: " + accuracy + "] [d': " + dPrime + "] [RT: " + averageRT + "]");
                }
                
                private void calcSensitivity () {
                        //Account for 0 hits and misses case
                        if (hits == 0 && misses == 0) sensitivity = 0;
                        
                        //Calculate sensitivity normally
                        else { 
                            sensN = (double) hits + (double) misses;
                            sensitivity = ((double) hits / (sensN));
                        }
                        
                        //Account for 0 and 100% cases
                        if(sensitivity == 0 || sensitivity == 1) {
                            //Adjust according to chosen method
                            switch (ZERO_RATE_METHOD) {
                                //Adjust extreme values (MacMillan & Kaplan, 1985): replace rate with 1 - (1/2n) for 100%, 1/2n for 0%
                                case 1: 
                                    if(sensitivity == 1) {
                                        sensitivity = (1 - (1 / (2 * sensN)));
                                    } else {
                                        sensitivity = (hits == 0 && misses == 0) ? 0 : (1 / (2 * (sensN)));
                                    }
                                    break;
                                //Loglinear method (Hautus, 1995): add 0.5 to number of hits
                                case 2:
                                    double hitsAdj = (double) hits + 0.5;
                                    sensN = hitsAdj + (double) misses;
                                    if(sensitivity == 1) {
                                        sensitivity = (hitsAdj / (sensN));                                        
                                    } else {
                                        sensitivity = (hits == 0 && misses == 0) ? 0 : (hitsAdj / (sensN));
                                    }
                                   break;
                            } 
                        }
                }
                
                private void calcFalseErrorRate() {
                        //Account for 0 correctRejections and falseAlarms case
                        if(correctRejections == 0 && falseAlarms == 0) falseErrorRate = 0;                    
                    
                        //Calculate falseErrorRate
                        else { 
                            falseErrorN = (double) falseAlarms + (double) correctRejections;
                            falseErrorRate = ((double) falseAlarms / (falseErrorN));
                        }
                         //Account for 0 and 100% cases
                        if (falseErrorRate == 1 || falseErrorRate == 0) {
                            //Adjust according to chosen method
                            switch (ZERO_RATE_METHOD) {
                                //Adjust extreme values (MacMillan & Kaplan, 1985): replace rate with 1 - (1/2n) for 100%, 1/2n for 0%
                                case 1:
                                    if(falseErrorRate == 1) {
                                        falseErrorRate = (1 - (1 / (2 * (double) falseErrorN)));      
                                    } else {
                                        falseErrorRate = (correctRejections == 0 && falseAlarms == 0) ? 0 : (1 / (2 * falseErrorN));
                                    }
                                    break;
                                //Loglinear method (Hautus, 1995): add 0.5 to number of false alarms
                                case 2:
                                    double falseAlarmsAdj = (double) falseAlarms + 0.5;
                                    falseErrorN = falseAlarmsAdj + (double) correctRejections;
                                    falseErrorRate = (correctRejections == 0 & falseAlarms == 0) ? 0 : ((double) falseAlarms / falseErrorN);
                                    break;
                            } 
                            
                        }
                }

		private void calcDPrime () {    
                        //Calculate sensitivity
                        calcSensitivity();
                        
                        //Calculate falseErrorRate
                        calcFalseErrorRate();
                              
                        //Do not apply d' formula if invalid
                        if(sensitivity == 0 || falseErrorRate == 0) {
                            setDPrime(Double.NaN);
                        } else if (sensitivity == 1 || falseErrorRate == 1) {
                            setDPrime(Double.NaN);                            
                        }
                        //Calculate d' according to chosen formula
                        else {
                            //Unequal variances formula
                            if (ToneMonitoringScorer.DPRIME_FORMULA == 2) {
                                double signalMean = sensN * sensitivity;
                                double noiseMean = falseErrorN * falseErrorRate;
                                double signalMeanD = signalMean - (sensN * 0.5);
                                double noiseMeanD = noiseMean - (falseErrorN * 0.5);
                                double signalVar = signalMean * (1 - sensitivity);                                
                                double noiseVar = noiseMean * (1 - falseErrorRate);
                                
                                //Pool variance
                                if(ToneMonitoringScorer.POOL_VARIANCE) {
                                    double pooledVar = (signalVar + noiseVar) / 2;
                                    setDPrime((signalMeanD - noiseMeanD) / Math.sqrt(pooledVar / 2));
                                }
                                  //Independent variance
                                  else {
                                    double zHit = signalMeanD / Math.sqrt(signalVar);
                                    double zAlarm = noiseMeanD / Math.sqrt(noiseVar);
                                    setDPrime(zHit - zAlarm);
                                }
                            }
                            //Homogeneity of variance formula
                             else {
                                double zHit = (inverseCDF(sensitivity));
                                double zAlarm = inverseCDF(falseErrorRate);
                                setDPrime(zHit - zAlarm);
                            }
                        }
		}

		//Getters and setters
		public double getDPrime() {
			return this.dPrime;
		}
		public void setDPrime(double dPrime){ 
			this.dPrime = dPrime;
		}
                public double getAccuracy() {
                        return this.accuracy;
                }
                public void setAccuracy(double accuracy) {
                        this.accuracy = accuracy;
                }
		public double getAverageRT() {
			return this.averageRT;
		}

    public String[] getResponse() {
        return response;
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getFalseAlarms() {
        return falseAlarms;
    }

    public int getCorrectRejections() {
        return correctRejections;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getFalseErrorRate() {
        return falseErrorRate;
    }
	}
