import java.net.Socket;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.sound.midi.MidiSystem;	// hozzaferes: synthesizer, sequencer
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;

import java.util.HashMap;
import java.util.Map;

public class Client {
	public static void main(String[] args) throws Exception {
	
		System.out.println("Optional commands: \n add <title> \t second line the sheet, ex: C 4 E 4 C 4 E 4 G 8 \n addlyrics <title> \t second line the lyric, ex: bo ci bo ci tar ka \n play <speed> <transpose value> <title> \n change <plyayQueeID> <speed> <transpose> \n stop <plyayQueeID> ");
		
		try (
			Socket s = new Socket("localhost", 40000);
			Scanner sc = new Scanner(s.getInputStream());
			PrintWriter pw = new PrintWriter(s.getOutputStream());

			Scanner scIn = new Scanner(System.in);
		) {

			// user -> srv
			Thread t1 = new Thread(() -> {
				while (scIn.hasNextLine()) {
					String line = scIn.nextLine();
					pw.println(line);
					pw.flush();
				}
				// pw.flush();
			});	//thread t1

			// srv -> user
			Thread t2 = new Thread(() -> {
				try{
					Synthesizer synthesizer = null;
	        		MidiChannel channel = null;
	        		synthesizer = MidiSystem.getSynthesizer();
	            	synthesizer.open();
	            	channel = synthesizer.getChannels()[0];
					
					int lastNote = 0;
					String line = sc.nextLine();
					System.out.println("Now playing: " + line);
					while (sc.hasNextLine()) {
						line = sc.nextLine();
		                if (!line.equals("FIN")) {
			                String[] split = line.split(" ");
			                String note = split[0];
			                channel.noteOff(lastNote);
		                    lastNote = getMIDIValue(note);
		                    channel.noteOn(lastNote, 100);

							System.out.println(split[1]);
		                }

						// System.out.flush();
					}
				}catch (Exception e) {
					System.out.println("Error while streaming!");
				}	//try
			});		//thread t2

			t1.start();
			t2.start();

			t1.join();
			t2.join();

		}	//try
	}	//main

	public static Integer getMIDIValue(String note){
		 
		 Integer difference = 0;
		 if(note.contains("/")){
		 	String[] noteParts = note.split("/");
		 	note = noteParts[0];
		 	difference = Integer.valueOf(noteParts[1]);
		 }

		 Integer sound = 60; 
		 switch(note) {
		 	case "C": 	sound = 60; 	break;
		 	case "C#": 	sound = 61;		break;
		 	case "Db": 	sound = 62;		break;
		 	case "D": 	sound = 63;		break; 
		 	case "D#": 	sound = 64;		break; 
		 	case "Eb": 	sound = 65;		break; 
		 	case "E": 	sound = 66;		break; 
		 	case "F": 	sound = 67;		break; 
		 	case "F#": 	sound = 68;		break; 
		 	case "Gb": 	sound = 69;		break; 
		 	case "G": 	sound = 70;		break; 
		 	case "G#": 	sound = 71;		break; 
		 	case "Ab": 	sound = 72;		break; 
		 	case "A": 	sound = 73;		break; 
		 	case "A#": 	sound = 74;		break; 
		 	case "Bb": 	sound = 75;		break; 
		 	case "B": 	sound = 76;		break; 
		}
		return sound + difference*24;
	}


}	//class