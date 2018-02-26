/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

/**
 *
 * @author nextjuanplz
 */
	public class OutputLog implements Comparable<OutputLog> {
		//Fields
                private final String fileName;
                private final String participantNumberCondition;
		private final int participant;
		private double dPrime;
                private double pctAccuracy;
		private double avgRT;
                
                //Processed
		private int hits;
		private int misses;
		private int falseAlarms;
		private int correctRejections;
		private double sensitivity;
		private double falseErrorRate;

		//Constructor
		public OutputLog(String fileName, String participantCondition, ProcessedData processedData) {
			this.fileName = fileName;
                        this.participantNumberCondition = participantCondition;
                        this.participant = Integer.parseInt(participantCondition.replaceAll("[\\D]", ""));
                        fetchData(processedData);            
		}

		//Methods
                private void fetchData (ProcessedData processedData) { 
                        this.hits = processedData.getHits();
                        this.misses = processedData.getMisses();
                        this.correctRejections = processedData.getCorrectRejections();
                        this.falseAlarms = processedData.getFalseAlarms();
                        this.sensitivity = processedData.getSensitivity();
                        this.falseErrorRate = processedData.getFalseErrorRate();
                        this.pctAccuracy = processedData.getAccuracy();
			this.dPrime = processedData.getDPrime();
			this.avgRT = processedData.getAverageRT();
                }
                
                @Override
		public String toString() {
			return (fileName + "," + 
                                participantNumberCondition + "," +
                                hits + "," +
                                misses + "," +
                                correctRejections + "," +
                                falseAlarms + "," +
                                sensitivity + "," +
                                falseErrorRate + "," +                                                             
                                dPrime + "," +
                                pctAccuracy + "," +
                                avgRT);
		}
                @Override
                public int compareTo(OutputLog o) {
                    //Sort from smallest to largest participant number
                    if (this.getParticipantNumber() > o.getParticipantNumber()) return 1;
                    else if (this.getParticipantNumber() < o.getParticipantNumber()) return -1;
                    else return 0;
                    
                }
                
                //Getters
                public int getParticipantNumber() {
                    return this.participant;
                }
	}
