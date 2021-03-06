package Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import DataStructures.List.ArrayList;
import DataStructures.List.List;
import DataStructures.Sets.DynamicSet;
import DataStructures.Sets.Set;

/**
 * The Election class
 * 
 * Where we decide who wins and is our new president base on the information provided
 * 
 * @author: Fernando J. Bermudez (@bermed28)
 * @version: 3.0
 * @since 2020-03-01
 */
public class Election{
	//PRIVATE/PUBLIC FIELDS
	
	private static int accountedBallots = 0;
	public static int blankBallot = 0;
	private static int invalidBallots;

	/**where we will hold all of our ballots*/
	private static DynamicSet<Ballot> mainBallotSet; 
	
	/**our list of sets that will change depending on who gets eliminated*/
	private static List<DynamicSet<Ballot>> ballotStoring= new ArrayList<DynamicSet<Ballot>>();
	
	/**copy of our list of sets made at the beginning for comparison purposes in elimiateSmallest1s()*/
	private static List<DynamicSet<Ballot>> ogBallotStoring = new ArrayList<DynamicSet<Ballot>>();
	
	/**ArrayLists and variables that hold the information regarding the candidates for output processing*/
	public static ArrayList<Candidate> candidates;
	private static ArrayList<Integer> eliminatedCandidates = new ArrayList<>();
	private static ArrayList<Integer> ones = new ArrayList<>();
	private static int winnerID;
	private static int winner1s;
	private static Candidate newPresident;
	
	/**Variables that stops the counting and elimination rounds & that count how may rounds have gone by in the election*/
	private static boolean winnerDetermined = false;
	private static int roundIdx = 0;
	
	
	/**List that stores the names of the eliminated candidates for output processing*/
	private static ArrayList<String> name = new ArrayList<>();

	private static final int DEFAULT_SIZE = 10;

	public static void main(String[] args) {
		
		/**
		 * Where all the magic happens
		 */
		processInputAndStoreInfo();
		votingRounds();
		processOutputInformation();
		
		/**
		 * MAKE SURE YOU STORE ballots.csv & candidates.csv IN THE inputFiles FOLDER IS 
		 * OTHERWISE THE PROGRAM WILL THROW AN ERROR BECAUSE IT CAN'T FIND THE FILE
		 */
		
	}

	/**
	 * Where we call our methods to process and store all of our information to use in our election
	 * @return nothing
	 */
	public static void processInputAndStoreInfo() {
		/**
		 * Here we insert which files we want to use and the methods take care of processing the information
		 */
		processCandidateInformation("candidates.csv");
		processBallotInformation("ballots.csv");
	}

	
	/**
	 * Where we process our information given in ballots.csv
	 * @return nothing
	 */
	public static void processBallotInformation(String csvFile) {
		mainBallotSet = new DynamicSet<>(DEFAULT_SIZE);
		BufferedReader br = null;
		String line = "";
		String comma = ",";
		try {

			br = new BufferedReader(new FileReader("inputFiles/" + csvFile));
			while ((line = br.readLine()) != null) {

				// ballotNum,c1:r1,c2:r2,...,cn:rn
				// use comma as separator

				//We separate the csv lines into an array of strings so we can use it to process information
				String[] ballotInfo = line.split(comma);   

				//After we split the line, we pass that information to create a new instance of a Ballot
				Ballot ballot = new Ballot(ballotInfo);

				if (ballot.isValidBallot()) { //if it's valid, we add it to our main set which will hold all of our valid ballots
					mainBallotSet.add(ballot);
				}

				// If invalid, we just account it for, we don't add it anywhere
				accountedBallots++;
				invalidBallots =  ((accountedBallots - blankBallot) - mainBallotSet.size());

			}

			//After we add, we reorganize the ballots (see method for more info)
			organizeBallots();

			ogBallotStoring = ballotStoring;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();


		} 
		finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}


	/**
	 * Method that stores all the ballots where a certain candidate is ranked 1 inside a set. 
	 * These sets are then stored in a list for easy access
	 * 
	 * @param none
	 * @returns nothing
	 */
	private static void organizeBallots() {


		//this is the list of sets where we are going to organize our ballots
		ArrayList<DynamicSet<Ballot>> result = new ArrayList<DynamicSet<Ballot>>();
		
		
		//In here, we go through each candidate in our race and create a new set to hold our ballots.
		for(Candidate candidate : candidates) {
			if(!eliminatedCandidates.contains(candidate.getCandidateID())) {
				DynamicSet<Ballot> setSub_i = new DynamicSet<>(DEFAULT_SIZE);
				for (Ballot ballot : mainBallotSet) {

					if (ballot.getRankByCandidate(candidate.getCandidateID()) == 1) {
						//once we create the set, we check if the top choice is that candidate we are iterating through
						setSub_i.add(ballot);
					}
				}
				result.add(setSub_i);
			}
		}

		ballotStoring = result;
	}


	/**
	 * Where we process our information given in candidates.csv
	 * @return nothing
	 */
	public static void processCandidateInformation(String csvFile) {
		candidates = new ArrayList<>();
		eliminatedCandidates = new ArrayList<>();
		BufferedReader br = null;
		String line = "";
		String comma = ",";
		try {

			br = new BufferedReader(new FileReader("inputFiles/" + csvFile));
			while ((line = br.readLine()) != null) {

				// Here we do the same thing as the ballot processing, only that we do it with our candidates file
				// We split the line in an array, we create an instance of candidate, and add it to our candidate list
				// use comma as separator
				String[] candidateInfo = line.split(comma);
				Candidate curCandidate = new Candidate(candidateInfo);
				candidates.add(curCandidate);

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}


	}


	private static void votingRounds() {

		//Here's where the fun begins

		while (winnerDetermined == false) {
			//If no winner is declared, we start round 1 of our voting sessions
			for (DynamicSet<Ballot> candSet : ballotStoring) {
				//First we check if a certain candidate has more than 50% of the votes throughout all ballots
				if (candSet.size() > (mainBallotSet.size() / 2)) {
					//If that is the case, we declare said candidate the winner and save his ID and amount of 1's for future output
					winnerDetermined = true;
					winnerID = candSet.get(0).getCandidateByRank(1);
					winner1s = candSet.size();
					electPresident();
					return;
				}

			}

			//If no one has more than 50% of the votes it's time to take someone to the chopping block and eliminate them from the race
			eliminateSmallest1s();

			//After we eliminate someone, we continue to the next round
			roundIdx++;
		}

	}

	private static void electPresident() {
		//Pretty straightforward, if the chosen candidate is in our list of candidates, then he is declared our new president
		for (Candidate c : candidates) {
			if (c.getCandidateID() == winnerID) {
				newPresident = c;
			}
		}
	}


	/**
	 * This is the method we delegated with the task of eliminating a candidate from the race
	 * @param none
	 * @returns nothing
	 * 
	 */
	
	private static void eliminateSmallest1s() {
		

		/** 
		 * First step, save the candidate with the largest amount of 1's for comparison purposes
		 * (like when finding the smallest int in an array back in CIIC3011 where you would save the largest number to later compare and replace it
		 * with the smallest one)
		 * 
		 * Also, we set our smallestID variable to 1, given that the first candidateID is always 1
		 */
		Set<Ballot> smallest = ballotStoring.get(ballotStoring.size() - 1);
		int smallestID = 1;

		/**
		 * Then we do what is mentioned in the comment above, we compare the largest set to see if there is a set smaller than this one, 
		 * if true, then said set is now the smallest. 
		 */
		for (int i = 0; i < ballotStoring.size(); i++) {
			if (ballotStoring.get(i).size() < smallest.size()) {
				smallest = ballotStoring.get(i);
			}
			
		}
		
		/**
		 * After saving the smallest set, we have to look for the candidates that is the "owner" of smallest
		 * What we do is we iterate through the unedited copy of our list of sets:
		 * 
		 * 		if i + 1 (we're using position to track the ID) is not an eliminated candidate, and smallest is equal to a set inside
		 * 		our original ballot (this means we're in the right index){
		 * 
		 * 			we change smallestID to i + 1
		 * 
		 * 		}
		 */
		for (int i = 0; i < ogBallotStoring.size(); i++) {
			if((eliminatedCandidates.contains(i + 1) == false) && (ogBallotStoring.get(i).isEqual((smallest)))) {
				smallestID = i + 1;
			}
		}
		
		/**
		 * Now we check if there are two sets that have the same amount of 1's (but while making sure we are not comparing the same set 
		 * in terms of elements inside the set, that's why we use isEqual()). If there are two candidate sets with the least amount of 1's,
		 * we go into a tieBreaker by comparing the least amount of 2's, and so on and so forth until a winner is declared between the two
		 * (see method for more information) and smallest becomes the winner of that tieBreaker
		 * 
		 * 
		 * for the ID, we just do the same thing we do if there is not a tie, given that we iterate through and unedited list of sets
		 * we stored as reference
		 */
		
		int i = 0;
		for (Set<Ballot> set : ballotStoring) {
			
			if (set.size() == smallest.size() && !(set.isEqual(smallest))) {
				smallest = tieBreaker(2, 0, 0, set, smallest);
				break;
			}
		
			i++;
				
		}
		
		for (int j = 0; j < ogBallotStoring.size(); j++) {
			if((eliminatedCandidates.contains(j + 1) == false) && 
					(ogBallotStoring.get(j).isEqual(smallest))) {
				smallestID = j + 1;
			}
		}


		/**
	     * When we have our information, we then add said eliminated candidate to the eliminatedCandidate list.
		 * This list holds by position the ID of the candidate was eliminated
		 * 
		 * Then we add the size of the smallest set (because the size yields the amount of 1s said candidate has) to our
		 * ones list, which we use to compare and track by index what set belongs to which candidate
		 */
		
		eliminatedCandidates.add(smallestID);
		ones.add(smallest.size());


		/**When we saved that information, we then proceed to eliminate said candidate from EVERY ballot saves inside our main ballot list*/
		for (Ballot ballot : mainBallotSet) {
			ballot.eliminate(smallestID);

		}

		/*
		 * After elimination said candidate, we have to organize our ballots once again because we upgraded the ranks of some 
		 * candidates in the process of elimination
		 * 
		 * WE DON NOT RE-ORGANIZE ogBallotStoring, WE ONLY RE-ORGANIZE ballotStoring
		 * 
		 */
		organizeBallots();

	}


	/**
	 * The Tie Breaker: Method that returns a set that contains the least amount of 1's, 2's, rank n or the set of the candidate with the highest ID
	 * 
	 *  @param: randIndex: index that determines which ranks to compare, countS1/S2: counters to see who wins the tie, 
	 *  		S1: Set 1, S2: Set 2 (we will be comparing S1 and S2)
	 *  
	 *  @returns: the set with the smallest amount of 1's, 2's, rank n or the set of the candidate with the highest ID
	 *  
	 */
	private static Set<Ballot> tieBreaker(int rankIndex, int countS1, int countS2, Set<Ballot> S1, Set<Ballot> S2) {
		Set<Ballot> result = null;

		//First we go through each ballot in our main ballot set counting how many of rankIndex S1 and S2 have in those ranks
		for (Ballot ballotS1 : S1) {
			for (Ballot mainballot : mainBallotSet) {
				if (ballotS1.getCandidateByRank(1) == mainballot.getCandidateByRank(rankIndex)) {
					countS1++;
				}
			}
		} 

		for (Ballot ballotS2 : S2) {
			for (Ballot mainballot : mainBallotSet) {
				if (ballotS2.getCandidateByRank(1) == mainballot.getCandidateByRank(rankIndex)) {
					countS2++;
				}
			}
		} 


		//Here we check if the counts are equal, or if one is less than the other to make a decision
		if (countS1 == countS2) {
			//if they're equal, it means we have to check with another rank 

			if (rankIndex < ballotStoring.size()) {

				//What we do is we increase the rankIndex by 1 and call recursively the method again but with the increased index
				return tieBreaker(rankIndex + 1, 0, 0, S1, S2);
			} 

			//If the rankIndex is the same as the total number of candidates, that means we have to make a decision based off the tied candidate's ID

			if(rankIndex == ballotStoring.size()){
				//If S1's candidate has higher ID than S2's candidate, then he is the Set returned and the tie is over. Same goes vice versa
				if (S1.get(0).getCandidateByRank(1) > S2.get(0).getCandidateByRank(1)) {
					result = S1;
				} else {
					result = S2;
				}
			}

			//If the counts aren't equal, it means we found our winner of the tie, whoever has the smallest count is the set we return to eliminate 
		} else if(countS1 < countS2) {
			result = S1;
		} else if(countS2 < countS1) {
			result = S2;
		}

		return result;
	}


	/**
	 * Writes to output file all of the information regarding this election:
	 * 			How many Ballots were casted
	 * 			How many blank/Invalid Ballots were there
	 * 			By round, who got eliminated and with how many 1's 
	 * 			The winner of the election and with how many 1's the candidate won
	 * 
	 * @param: none
	 * @returns: nothing
	 */
	public static void processOutputInformation() {
		
		/**
		 * In here, we just iterate through our eliminated candidate's ID's so 
		 * we can extract their name from our original candidates list
		 */
		for (Integer i : eliminatedCandidates) {
			for (Candidate c : candidates) {
				if (c.getCandidateID() == i) {
					name.add(c.getName());
				}
			}
		}

		Writer writer = null;
		try {	

			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("results.txt"), "utf-8"));

			//writer.write() writes anything we tell it to to the output file mentioned above
			writer.write("Number of ballots: " + accountedBallots + '\n');
			writer.write("Number of blank ballots: " + blankBallot +'\n');
			writer.write("Number of invalid ballots: " + invalidBallots + '\n');


			/**
			 * By round, we write the eliminated candidate and with how many 1's the candidate lost by
			 * using the information we saved earlier in the rounds in ones 
			 * and elimiatedCandidates (that was used to form the names list)
			 */
			for (int i = 0; i < roundIdx; i++) {

				writer.write("Round " + (i + 1) + ": " + name.get(i) + " was eliminated with " + ones.get(i) + " #1's" + '\n');

			}

			//We write to the file who won and with how many 1's
			writer.write("Winner: " + newPresident.getName() + " wins with " + winner1s + " #1's");



		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(writer != null) {
					writer.close();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}