/******************************************************************************
 * This is a small class file created by atesin (not me) to create properly   *
 * spaced tables in Minecraft.                                                *
 *                                                                            *
 * The related Bukkit forum thread can be found here:                         *
 * http://forums.bukkit.org/threads/class-monospace-fixed-width-fonts.259459/ *
 *                                                                            *
 ******************************************************************************/

package io.github.sleepydragn1.MultiverseTeleportFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * class to keep, process and write tab formatted text on minecraft chat window and console,
 * it also support almost all format codes (in uppercase, except bold format),
 * see http://minecraft.gamepedia.com/Formatting_codes#Color_codes
 * 
 * usage: format a multiline ansi string first:
 * - each line separated with line feed "\n" (ascii 10, configurable)
 * - each line field separated with accents "`" (ascii 96, configurable)
 * - if lines does not start with a line separator first tab will be at 0px, chars beyond fields are trimmed
 * - it supports color codes (substracts 6px, use uppercases and do not use bold style to avoid width errors)
 * - create a new TabText object with your text, you can also set page height and line separator (not configurable after)
 * - you can set another tab positions and other properties if you want
 * - you can add additional characters to check if you want (but spanish ready!)
 * - you may query how much pages your text will have
 * - you may retrieve your desired page (0=all in one), optionally tell if you want it monospaced or not (default)
 * 
 * defaults:
 * - page heigth: 20 lines (according to minecraft chat window, configurable on construction)
 * - line separator: "\n" (configurable on construction before line splitting)
 * - page width: 53 chars (according to minecraft chat window, considering 6px width chars)
 * - field separator: "`"
 * - fill space: " "
 * - thin space: "`" (less visible character in minecraft chat window)
 * - use thin spaces: yes
 * - monospaced fonts: no
 * - tab positions: 10, 20, 30, 40
 * - chars to be checked: all printable ascii chars (32-126) plus spanish chars
 * 
 * @author atesin%gmail.com
 */
public class TabText{
	
	/** setted before line separation */
	private String lineSeparator = "\n";
	/** height of minecraft chat area, setted at construction to avoid possible page counting errors */
	// Changed from default value of 20
	private int pageHeight = 18;
	public String fieldSeparator = "`";
	public String fillSpace = " ";
	// Changed from default value of "."
	public String thinSpace = "\0\0";
	/** width of minecraft chat area considering 6px chars if not monospaced */
	public int pageWidth = 53;
	public boolean useThinSpace = false;
	public boolean monospace = false;
	public ArrayList<Integer> tabs = new ArrayList<Integer>(Arrays.asList(10,20,30,40));
	private int numPages;
	private String[] lines; // private to avoid on the fly changing
	private Map<Integer, String> chraracters = new HashMap<Integer, String>(){{
		put(7, "@~");
		put(6, "#$%&+-/0123456789=?ABCDEFGHJKLMNOPQRSTUVWXYZ\\^_abcdeghjmnopqrsuvwxyzñÑáéóúü");
		put(5, "\"()*<>fk{}");
		put(4, " I[]t");
		put(3, "'`lí");
		put(2, "!.,:;i|");
		put(-6, "§");
	}};
	
	// CONSTRUCTORS
	
	public TabText(String text){
		lines = text.split(lineSeparator);
		numPages = (int) Math.ceil((double)lines.length / (double)pageHeight);
	}
	
	public TabText(String text, int chatHeigth){
		lines = text.split(lineSeparator);
		this.pageHeight = chatHeigth;
		numPages = (int) Math.ceil((double)lines.length / (double)pageHeight);
	}
	
	public TabText(String text, String lineSeparator){
		lines = text.split(lineSeparator);
		this.lineSeparator = lineSeparator;
		numPages = (int) Math.ceil((double)lines.length / (double)pageHeight);
	}
	
	public TabText(String text, int chatHeigth, String lineSeparator){
		lines = text.split(lineSeparator);
		this.pageHeight = chatHeigth;
		this.lineSeparator = lineSeparator;
		numPages = (int) Math.ceil((double)lines.length / (double)pageHeight);
	}
	
	// SETTER AND GETTER METHODS
	
	public int getChatHeigth(){
		return pageHeight;
	}
	
	public int getNumPages(){
		return numPages;
	}
	
	public int getNumLines(){
		return lines.length;
	}
	
	/** just for comfort */
	public void setTabs(int[] tabs){
		ArrayList<Integer> tabs2 = new ArrayList<Integer>();
		for (int i: tabs) tabs2.add(i);
		this.tabs = tabs2;
	}
	
	// REGULAR METHODS
	
	/**
	 *  appends chars with its width to be checked too
	 *  
	 *  @param chars a list of the chars, as a string (not all works, make tests)
	 *  @param wid the horizontal space each char ocuppies including its separation, in pixels
	 */
	public void addChars(String chars, int wid){
		if (!chraracters.containsKey(wid)) chraracters.put(wid, "");
		chraracters.get(wid).concat(chars);
	}
	
	/**
	 * @param page desired page number (0=all-in-one), considering preconfigured adjusts
	 * @return desired page text
	 */
	public String getPage(int page){
		return getPage(page, this.monospace);
	}
	
	/**
	 * @param page desired page number (0=all-in-one), considering preconfigured adjusts
	 * @param monospace true if fonts are fixed width (e.g.: console)
	 * @return desired page text
	 */
	public String getPage(int page, boolean monospace){
				
		int chatWidthPx = (monospace)? pageWidth: pageWidth * 6;
		
		// get bounds if user wants pages
		int fromLine = (--page) * pageHeight;
		int toLine = (fromLine + pageHeight > lines.length)? lines.length: fromLine + pageHeight;
		if (page < 0){
			fromLine = 0;
			toLine = lines.length;
		}
		
		// prepare lines iteration
		String tabLines = "";
		String tabLine;
		String line;
		String fields[];
		String field;
		int fieldLen;
		int toLen;
		int colWid;
		
		// iterate each line
		for (int linePos = fromLine; linePos < toLine; ++linePos){
			
			line = lines[linePos];
			
			// prepare fields iteration
			tabLine = "";
			fields = line.split(fieldSeparator);
			
			// start iterating each field
			for (int fieldPos = 0; fieldPos < fields.length; ++fieldPos){
				
				// get field
				field = fields[fieldPos];
				fieldLen = pxLength(field, monospace);
				
				// get width limit
				toLen = (fieldPos > tabs.size()-1)? chatWidthPx: (tabs.get(fieldPos) * (monospace? 1: 6));
				
				// find if must truncate field
				colWid = toLen - pxLength(tabLine, monospace);
				if (fieldLen > colWid) tabLine += pxSubStr(field, colWid, monospace);
				else tabLine += field;
				
				// if end line is near
				if (toLen >= chatWidthPx){
					if (pxLength(tabLine+fillSpace, monospace) > chatWidthPx) break;
					tabLine += fillSpace;
					continue;
				}
				
				// (add a thin space to adjust horizontal position if required)
				if (!monospace && useThinSpace && pxLength(tabLine, monospace) % 4 > 1) tabLine += thinSpace;
				
				// add spaces to fill column width as needed
				while (pxLength(tabLine, monospace) < toLen) tabLine += fillSpace;
			}
			tabLines += ((linePos == 0)? "": lineSeparator) + tabLine;
		}
		return tabLines;
	}
	
	/**
	 * @param str string to be checked
	 * @param monospace true if fixed width fonts will be used
	 * @return string width in pixels
	 */
	private int pxLength(String str, boolean monospace){
		
		if (monospace) return 2*(str.replace("§", "").length()) - str.length();
		
		int len = 0;
		for (int strPos = 0; strPos < str.length(); ++strPos){
			for (int px: chraracters.keySet()){
				if (chraracters.get(px).indexOf(str.charAt(strPos)) >= 0){
					len += px;
					break;
				}
			}
		}
		return len;
	}
	
	/**
	 * 
	 * @param str input string
	 * @param len desired string length in pixels or in chars if monospace (exclusive)
	 * @param monospace true if fonts with fixed width (console)
	 * @return stripped string
	 */
	private String pxSubStr(String str, int len, boolean monospace){
		
		int len2 = str.length();
		
		// in reverse in case string have color codes
		while (len2 > 0){
			// if is already well sized
			if (pxLength(str, monospace) <= len) return str;
			// if not then decrease and continue
			--len2;
			str = str.substring(0, len2);
		}
		return str;
	}
	
	void sortByFields(int... args){
		// dont work useless
		if (args.length == 0) return;
		
		// create a new temp array
		ArrayList<String> tempArray = new ArrayList<String>();
		String tempLines;
		String[] tempFields;
		
		// build lines and add to array, with index at last
		for (int i = 0; i < lines.length; ++i){
			tempFields = lines[i].split(fieldSeparator);
			tempLines = "";
			for (int by: args) tempLines += fillSpace+tempFields[by];
			tempArray.add(tempLines+fieldSeparator+i);
		}
		// sort temp array
		Collections.sort(tempArray);
		
		// build a new array with ordered indexes
		tempLines = "";
		for (String line: tempArray){
			tempLines += (tempLines.length() == 0)? "": lineSeparator;
			tempLines += lines[Integer.parseInt(line.substring(line.indexOf(fieldSeparator)+1))];
		}
		// replace lines array
		lines = tempLines.split(lineSeparator);
	}
	
	void sortByNumField(int by, boolean asc){
		// create a new temp array
		ArrayList<String> tempArray = new ArrayList<String>();
		String tempLines = "000000";
		String[] tempFields;
		
		// build lines and add to array, with index at last
		for (int i = 0; i < lines.length; ++i){
			tempFields = lines[i].split(fieldSeparator);
			tempArray.add(tempLines.substring(tempFields[by].length())+tempFields[by]+fieldSeparator+i);
		}
		// sort temp array
		if (asc) Collections.sort(tempArray);
		else Collections.sort(tempArray, Collections.reverseOrder());
		
		// build a new array with ordered indexes
		tempLines = "";
		for (String line: tempArray){
			tempLines += (tempLines.length() == 0)? "": lineSeparator;
			tempLines += lines[Integer.parseInt(line.substring(line.indexOf(fieldSeparator)+1))];
		}
		// replace lines array
		lines = tempLines.split(lineSeparator);
	}
}