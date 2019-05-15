
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.rmi.*;
import java.util.Random;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import javafx.util.*;

public class Server {
	public static void main(String[] args) throws Exception {
		Set<ClientData> 	otherClients = new HashSet<>();		//client list
		List<ActiveStream>	playQueue	 = new ArrayList<>();   //play quee
		List<Melody>		musics		 = new ArrayList<>();	//list of musics
		

		try (
			ServerSocket ss = new ServerSocket(40000);
		) {
			while (true) {
				ClientData client = new ClientData(ss);
				synchronized (otherClients) {
					otherClients.add(client);
				}

				new Thread(() -> {
					while (client.sc.hasNextLine()) {
						//possible command handling
						String currentCommand = client.sc.nextLine();
						String[] commands = {"addlyrics", "add", "play", "change", "stop"};

   						//search for which command is the current command
   						Integer cIdx = 0; 
   						boolean commandFound = false;
   						while ((cIdx<commands.length) && (!commandFound)) {	//iterate on possible commands while not found it
							//handling the search loop
							//System.out.println("commands length: " + String.valueOf(commands.length));		//LOG
							//System.out.println("cIDX: " + String.valueOf(cIdx));		//LOG
   							String possibleCommand = commands[cIdx];
							++cIdx;

							//get which command
							String commandPart = currentCommand.split(" ")[0];
							if (commandPart.equals(possibleCommand)) {
								//System.out.println(possibleCommand);	//LOG

								// add new melody
								if(possibleCommand.equals("add")){
									System.out.println("add new melody command cached, waiting for melody sheet");	//LOG
									
									Melody tmpMelody = new Melody();

									String title = currentCommand.replace("add ","");
									//System.out.println("given title: "+title);	//LOG
									tmpMelody.title = title;	//save given title

									//save given sheet
									String sheet = client.sc.nextLine();
									String[] splittedSheet = sheet.trim().split("\\s+");
									Integer whichMember = 1; // it is 1 if the "sheet"; 2 if the "lengthOfSounds" 
									MelodyPart tmpMelodyPart = new MelodyPart();
									Integer idx = 0;
									while(idx<splittedSheet.length) {
										if (whichMember == 1) {
											tmpMelodyPart.sheet = splittedSheet[idx];
											++whichMember;

											//handling repetition
											if(tmpMelodyPart.sheet.equals("REP")){
												++idx;
												String[] splittedRepetition = splittedSheet[idx].trim().split(";");
												Integer fromWhere = Integer.parseInt(splittedRepetition[0]);
												Integer howManyTimes = Integer.parseInt(splittedRepetition[1]);
												//System.out.println("fromWhere: " + String.valueOf(tmpMelody.music.size() - fromWhere) + "\thow many times: " + String.valueOf(howManyTimes));	// LOG
												Integer repIdx = 0;
												Integer endOfList = tmpMelody.music.size();
												while(repIdx < howManyTimes){
													for (int repSecondIdx = tmpMelody.music.size() - fromWhere; repSecondIdx < endOfList; ++repSecondIdx) {
														tmpMelodyPart = new MelodyPart();
														tmpMelodyPart.sheet = tmpMelody.music.get(repSecondIdx).sheet;
														tmpMelodyPart.lengthOfSounds = tmpMelody.music.get(repSecondIdx).lengthOfSounds;
														//System.out.println(tmpMelodyPart);	//LOG
														tmpMelody.music.add(tmpMelodyPart);
													}
													++repIdx;
												}

												//set ready for the next sheet
												whichMember = 1;
												tmpMelodyPart = new MelodyPart();
											}
										} else {
											//System.out.println("whichMember: " +  String.valueOf(whichMember));	//LOG
											tmpMelodyPart.lengthOfSounds = Integer.parseInt(splittedSheet[idx]);
											//System.out.println(tmpMelodyPart);	//LOG
											tmpMelody.music.add(tmpMelodyPart);
											whichMember = 1;
											tmpMelodyPart = new MelodyPart();
										}
										++idx;
									}

									//System.out.println(tmpMelody.title);	//LOG
									//tmpMelody.music.forEach(part->System.out.println(part));	//LOG
									musics.add(tmpMelody);
									//System.out.println(musics.get(0));	//LOG
									commandFound = true;

								//add lyric to melody
								}else if (possibleCommand.equals("addlyrics")) {
									System.out.println("add new lyric to melody command cached");	//LOG
									
									String title = currentCommand.replace("addlyrics ","");
									int id = findMelodyIDByTitle(title, musics);
									if (id == -1) {
										//System.out.println("Can't find this melody, try an other!");	//LOG
										client.pw.println("Can't find this melody, try an other!");
										client.pw.flush();
									} else {
										System.out.println("waiting for lyric...");	//LOG

										//process the lyric
										String lyric = client.sc.nextLine();
										String[] splittedLyric = lyric.trim().split(" ");
										Integer lIdx = 0;
										//parse lyric and already created music sheet
										for (MelodyPart part : musics.get(id).music) {
											part.lyric = splittedLyric[lIdx];
											++lIdx;
										}
										//System.out.println(musics.get(0));	//LOG
									}
									commandFound = true;

								//play the selected melody for client	
								}else if (possibleCommand.equals("play")) {
									System.out.println("play the melody command cached");	//LOG
									
									//set the parameters
									String  playInstructions = currentCommand.replace("play ","");
									System.out.println("given  playInstructions: "+ playInstructions);	//LOG
									String[] splittedPlayInstructions = playInstructions.trim().split(" ");
									if (splittedPlayInstructions.length<3) {
										client.pw.println("Missing parameters! \n try like: play <tempo> <transpose> <title>");
										client.pw.flush();
									} else {
										Integer speed = Integer.parseInt(splittedPlayInstructions[0]);	
										System.out.println("speed: " + splittedPlayInstructions[0]);	//LOG
										Integer transpose = Integer.parseInt(splittedPlayInstructions[1]);
										System.out.println("transpose: " + splittedPlayInstructions[1]);	//LOG
										String title = splittedPlayInstructions[2];	
										System.out.println("title: " + splittedPlayInstructions[2]);	//LOG

										//set current playing quee
										//save the stream information
										ActiveStream tmpStream =  new ActiveStream();
										tmpStream.title = title;
										tmpStream.speed = speed;
										tmpStream.transpose = transpose;
										//set it
										
//										synchronized(playQueue)
//										{
										playQueue.add(tmpStream);	//synchronize??
										Integer playQueueId = playQueue.size() - 1;
//										}
										client.pw.println("playing " + Integer.toString(playQueueId));	//send to the client the current position in play quee
										//System.out.println("playQueue size: " + String.valueOf(playQueue.size()));	// LOG

										new Thread(() -> {
												//stream to the client
												int id = findMelodyIDByTitle(title, musics);
												if (id != -1) {
													for (MelodyPart part : musics.get(id).music) {
														if (!playQueue.get(playQueueId).stop) {	//if this stream isn't stopped yet
															if (!part.sheet.equals("R")) {
																synchronized (playQueue.get(playQueueId)) {
																	client.pw.println(transposeNote(part.sheet, playQueue.get(playQueueId).transpose) + " " + part.lyric);
																}													
																client.pw.flush();
															}	//if not pause
															//keep tempo
															Integer delay = playQueue.get(playQueueId).speed*part.lengthOfSounds; 
															//client.sleep(delay);
															try{
																java.lang.Thread.sleep(delay);
															}catch(Exception e){
																System.out.println("ERROR while streaming!");
															}
														}	//end is this stream stopped test
													}	//end for
												}	//end if (!=-1)

												client.pw.println("FIN");
												client.pw.flush();
												playQueue.get(playQueueId).stop = true;

										}).start();
									} 	//missing paramter if

									commandFound = true;

								//change attributes on selected melody	
								}else if (possibleCommand.equals("change")) {
									System.out.println("change the melody command cached");	//LOG
									
									String  playInstructions = currentCommand.replace("change ","");
									String[] splittedPlayInstructions = playInstructions.trim().split(" ");
									Integer queueId;
									Integer speed;
									Integer transpose = 0;
									queueId = Integer.parseInt(splittedPlayInstructions[0]);
									speed = Integer.parseInt(splittedPlayInstructions[1]);
									if (splittedPlayInstructions.length == 3) {
										transpose = Integer.parseInt(splittedPlayInstructions[2]);
									}

									playQueue.get(queueId).speed = speed;
									playQueue.get(queueId).transpose = transpose;

									commandFound = true;

								//stop the  melody	
								//}else if (possibleCommand.equals("stop")) {
								} else {
									System.out.println("stop the melody command cached");	//LOG
									
									String  id = currentCommand.replace("stop ","");
									playQueue.get(Integer.parseInt(id)).stop = true;

									commandFound = true;

								}
							}	//lookingAt if end
						}	//while ((cIdx<commands.length) || (commandFound)) end

						//fölösleges
						/*synchronized (otherClients) {
							for (ClientData other : otherClients) {
								other.pw.println(line);
								other.pw.flush();
							}
						}*/
						//
					}	//(client.sc.hasNextLine()) end

					synchronized (otherClients) {
						otherClients.remove(client);
						try {
							client.close();
						} catch (Exception e) {
							// won't happen
						}
					}
				}).start();
			}	//while(true)
		}	//try end
	}	//main

	//find melody in music List by title
	public static Integer findMelodyIDByTitle(String title, List<Melody> musics){
		//System.out.println("findMelodyIDByTitle ON"); 	//LOG
		Integer idx = 0;
		Boolean found = false;

		while((!found)&&(idx<musics.size())){
			if (musics.get(idx).title.equals(title)) {
				found = true;
			}
			++idx;
		}
		
		if (found) {
			return idx-1;
		}

		return -1;
	}

	//calculate transpose of the note
	public static String transposeNote(String note, Integer transposeValue){
		HashMap<String, Integer> notes = new HashMap<String, Integer>();  
		notes.put("C", 0);
		notes.put("C#", 1);
		notes.put("Db", 1);
		notes.put("D", 2);
		notes.put("D#", 3);
		notes.put("Eb", 3);
		notes.put("E", 4);
		notes.put("E#", 5);
		notes.put("F", 5);
		notes.put("F#", 6);
		notes.put("Gb", 6);
		notes.put("G", 7);
		notes.put("G#", 8);
		notes.put("Ab", 8);
		notes.put("A", 9);
		notes.put("A#", 10);
		notes.put("Bb", 10);
		notes.put("B", 11);
		notes.put("Cb", 11);
		notes.put("R", 0);

		//find the value of the note
		Integer newValue;
		if (note.contains("/")) {
			String[] noteParts = note.split("/");
			newValue = notes.get(noteParts[0]) + Integer.parseInt(noteParts[1]) + transposeValue;	//this vill be the little part
		}else{
			newValue = notes.get(note) + transposeValue;	//this vill be the little part
		}

		if (newValue<0) {
			Integer tmp = 12 + transposeValue;
			
			//search for key 
			for (Map.Entry<String, Integer> entry : notes.entrySet()) {
	            if (entry.getValue() == tmp) {
	                //return entry.getKey() + "/" + newValue;
	                return entry.getKey() + "/-1";
	            }
        	}
		}

		//search for key if little part is > 12
		if (newValue>11) {
			newValue = newValue - 12; 		
			for (Map.Entry<String, Integer> entry : notes.entrySet()) {
				if (entry.getValue() == newValue) {
		        	return entry.getKey() + "/+1";
		    	}
	        }
		}

		//search for key if little part is > 0
		for (Map.Entry<String, Integer> entry : notes.entrySet()) {
			if (entry.getValue() == newValue) {
	        	return entry.getKey();
	    	}
        }
        return "basiclly this should never happen?";  	
	}	

}	//class Server


class ClientData implements AutoCloseable {
	Socket s;
	Scanner sc;
	PrintWriter pw;

	ClientData(ServerSocket ss) throws Exception {
		s = ss.accept();
		sc = new Scanner(s.getInputStream());
		pw = new PrintWriter(s.getOutputStream());
	}

	public void close() throws Exception {
		if (s == null) return;
		s.close();
	}
}

//necessary information about an active stream
class ActiveStream{
	public String 	title = "";
	public Integer 	speed;
	public Integer 	transpose;
	public boolean 	stop = false;
	public ActiveStream(){}
}

//parts of an melody
class Melody{
	public String 			title = "";					//title of music
	public List<MelodyPart> music = new ArrayList<>();	//List of song parts with sheet, initial length, lyric
	
	public Melody(){
		title = "";
	}

	@Override
    public String toString()
    {
     	System.out.println("title: " + title);
     	music.forEach(part->System.out.println(part));
        return "";
    }
}

//parts of a single sound
class MelodyPart{
	public String  sheet = "";
	public Integer lengthOfSounds;
	public String  lyric = "???";
	
	public MelodyPart(){
		lyric = "???";
	}

	@Override
    public String toString()
    {
        return "Sheet: " + sheet + "\tlength: " + Integer.toString(lengthOfSounds) + "\tlyric: " + lyric;
    }
}