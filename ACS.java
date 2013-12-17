import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class ACS 
{
	public static ArrayList<File> fileMap;
	public static Users u;
	public static boolean root;
	public static String runningUser;
	public static String runningGroup;
	public static void main(String [] args) throws IOException
	{
		/* argument validation */
		String option;
		String fileList = null;
		String userList = null;
		root = true;
		fileMap = new ArrayList <File>();
		if (args.length == 2)
		{
			userList = args[0];
			fileList = args[1];
		}
		if (args.length == 3)
		{
			option = args[0];
			userList = args[1];
			fileList = args[2];
			validateInput(option, userList, fileList);
			root = false;								// root access is not allowed
		}
		
		/* read userList into HashMap */
		u = new Users();
		BufferedReader userReader = new BufferedReader(new FileReader(userList));
		u.addUserList(userReader);
		u.addUser("root", "root");
		userReader.close();
		
		/* read fileList into HashMap */
		String line;
		BufferedReader fileListReader = new BufferedReader(new FileReader(fileList));
		while ((line = fileListReader.readLine()) != null)
		{
			String [] split = line.split("\\s+");
			if (split.length != 3)
			{
				continue;
			}	 
			File f = new File(split[0], split[1], split[2]);
			if (!fileExists(f.getFilename()) && f.getMode() <= 4095)		// only add to list if does not exist and if mode is within range
				fileMap.add(f);
		}
		fileListReader.close();
		
		/* prompt for next input line */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String commandLine, command;
		do 
		{
			System.out.println("Input: ");
			commandLine = br.readLine();
			if (commandLine.equals("exit"))
			{
				exit();
				break;
			}
			
			// parse command from line
			String [] strings = commandLine.split("\\s+");
			command = strings[0].toLowerCase();						// executing command
			if (command.equals("read") || command.equals("write") || command.equals("execute"))
			{
				if (strings.length != 3)
				{
					System.err.println(command + " incorrect number of arguments");
					continue;
				}
				String user = strings[1].toLowerCase();
				String file = strings[2].toLowerCase();
				if (command.equals("read"))
					read(user, file);
				else if (command.equals("write"))
					write(user, file);
				else if (command.equals("execute"))
					execute(user, file);
			}
			else if (command.equals("chmod"))
			{
				if (strings.length != 4)
				{
					System.err.println("chmod incorrect number of arguments");
					continue;
				}
				String user = strings[1].toLowerCase();
				String file = strings[2].toLowerCase();
				short mode = string2Octal(strings[3].toLowerCase());
				chmod(user, file, mode);
			}
			else	// command is not a valid command
			{
				System.err.println("Invalid command");
				continue;
			}	
		}
		while (!command.equals("exit"));
	}

	/* check if write access is valid */
	private static void write(String user, String file) 
	{
		if ((user.equals("root") && root) || modeSet(user, file, 1))	// shift 1 to left to get to first read bit
		{
			System.out.println("write " + user + " " + u.getGroup(user) + " 1");
		}
		else
			System.out.println("write " + user + " " + u.getGroup(user) + " 0");
		
	}

	/* check if read access is valid */
	private static void read(String user, String file) 
	{
		if ((user.equals("root") && root) || modeSet(user, file, 2))	// shift 2 to left to get to first read bit
		{
			System.out.println("read " + user + " " + u.getGroup(user) + " 1");
		}
		else
			System.out.println("read " + user + " " + u.getGroup(user) + " 0");
	}

	/* check if execute access is valid */
	private static void execute(String user, String file) 
	{
		int setID = setIDBits(file); 	//determine value of setid bits
		String groupName = u.getGroup(user);
		String userName = user;
		if ((user.equals("root") && root) || modeSet(user, file, 0)) 	// shift 0 to left to get to first read bit
		{
			switch(setID)
			{
			case 0:
				userName = user;
				groupName = u.getGroup(user);
				break;
			case 1:		// get groupid of file for bits 01
				userName = user;
				groupName = getGroupFromFilename(file);
				break;
			case 2:	// get userid of file for bits 10
				userName = getUserFromFilename(file);
				groupName = u.getGroup(user);
				break;
			case 3:		// get userid and groupid for bits 11
				userName = getUserFromFilename(file);
				groupName = getGroupFromFilename(file);
				break;
			default:
				System.out.println("incorrect value sticky bit?");
				break;
			}
			System.out.println("execute " + userName + " " + groupName + " 1" );
		}
		else
			System.out.println("execute " + user + " " + u.getGroup(user) + " 0");
		
	}

	/* checks if setID bits are set - returns whether user (10), group (01), or both bits are set (11) */
	private static int setIDBits(String file) 
	{
		for (File f: fileMap)
		{
			if (f.getFilename().equals(file))
			{
				int mask = 3 << 10;
				mask = (mask & f.getMode()) >> 10;
				return mask;
			}
		}
		return 0;
	}

	/* modeBit is 0 (execute), 1 (write), 2(read) - indicates shifts to get first mode */
	private static boolean modeSet(String user, String file, int modeBit) 
	{
		for (File f: fileMap)
		{
			if (f.getFilename().equals(file))
			{
				int mask = (1 << modeBit);
				if ((mask & f.getMode()) > 0)											// other bit, anyone can access
					return true;
				if (((mask << 3) & f.getMode()) > 0 && 
						u.getGroup(f.getUser()).equals(u.getGroup(user)))				// group bit, if file group is same as user group
					return true;
				if (((mask << 6) & f.getMode()) > 0 && isOwner(file, user))				// owner bit, only owner has position
					return true;
				return false;
			}
		}
		return false;
	}

	/* print current state of system to state.log */
	private static void exit() throws IOException 
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter("state.log"));
		for (File f: fileMap)
		{
			bw.append(mode2String(f.getMode()) + " ");
			bw.append(f.getUser() + " ");
			bw.append(u.getGroup(f.getUser()) + " ");
			bw.append(f.getFilename());
			//TODO output to state.log instead of standard out
			bw.newLine();
		}
		System.out.println("exited");
		bw.close();
		
	}

	private static void chmod(String user, String file, short mode) 
	{
		if ((user.equals("root") && root) || isOwner(file, user))
		{
			// perform chmod and output
			for (File f: fileMap)
			{
				if (file.equals(f.getFilename()))
				{
					f.setMode(mode);
				}
			}
			System.out.println("chmod " + user + " " + u.getGroup(user) + " 1");
		}
		else
			System.out.println("chmod " + user + " " + u.getGroup(user) + " 0");
	}
	
	/* determines if a user is the owner of a file */
	private static boolean isOwner(String file, String user) 
	{
		for (File f: fileMap)
		{
			if (file.equals(f.getFilename()))
			{
				return f.getUser().equals(user);
			}
		}
		return false;							// file not found
	}

	/* validation of input for 3 args */
	private static void validateInput(String option, String userList, String fileList)
	{
		if (!option.equals("-r"))			// option incorrect format
			System.err.println("Argument validaiton error: option should be \"-r\"");
	}
	
	/* print out file list */
	private static void printFiles()
	{
		for (File f: fileMap)
		{
			f.printFile();
		}
	}
	
	/* convert from String octal to decimal */
	private static short string2Octal(String str)
	{
		short mode = 0;
		short temp;
		if (str.length() != 4)
			System.err.println("String 2 Octal str is: " + str + ", must be 4 digits long");
		
		for (int i = 0; i < str.length(); i++)
		{
			temp = (short)(str.charAt(i) - '0');
			mode |= (temp << ((3-i)*3));
		}
		return mode;
	}
	
	/* return string rwx representation */
	private static String mode2String (short mode)
	{
		StringBuffer modeString = new StringBuffer();
		int count = 0;
		for (int i = 8; i >= 0; i--)
		{
			int temp = 1 << i;
			// executable section
			if (count == 2)
			{
				// other section
				if (i == 0)
				{
					int stickymask = (1 << 9) & mode;
					if (stickymask > 0)
					{
						if ((temp & mode) > 0)
							modeString.append("t");
						else
							modeString.append("T");
					}
					else
					{
						if ((temp & mode) > 0)
							modeString.append("x");
						else
							modeString.append("-");
					}
				}
				// user or group
				else
				{
					if (i == 6)				// user
					{
						int usermask = (1 << 11) & mode;
						if (usermask > 0)
						{
							if ((temp & mode) > 0)
								modeString.append("s");
							else
								modeString.append("S");
						}
						else
						{
							if ((temp & mode) > 0)
								modeString.append("x");
							else
								modeString.append("-");
						}
						
					}
					else if (i == 3)		// group
					{
						int groupmask = (1 << 10) & mode;
						if (groupmask > 0)
						{
							if ((temp & mode) > 0)
								modeString.append("s");
							else
								modeString.append("S");
						}
						else
						{
							if ((temp & mode) > 0)
								modeString.append("x");
							else
								modeString.append("-");
						}
					}
				}
				count = 0;
				continue;
			}
			
			// read and write bits
			else
			{
				if (count == 0)
				{
					if ((mode & temp) > 0)
						modeString.append("r");
					else
						modeString.append("-");
				}
					
				if (count == 1)
				{
					if ((mode & temp) > 0)
						modeString.append("w");
					else
						modeString.append("-");
				}
					
			}
			count++;
//			System.out.println("count is " + count + ", current modestring: " + modeString.toString());
		}
		return modeString.toString();
	}
	
	/* return username from file given the filename */
	private static String getUserFromFilename (String filename)
	{
		for (File f: fileMap)
		{
			if (filename.equals(f.getFilename()))
			{
				return f.getUser();
			}
		}
		return null;
	}
	/* return groupname from file given filename */
	private static String getGroupFromFilename (String filename)
	{
		for (File f: fileMap)
		{
			if (filename.equals(f.getFilename()))
			{
				return u.getGroup(f.getUser());
			}
		}
		return null;
	}
	
	/* determine if file with filename already exists */
	private static boolean fileExists(String filename) 
	{
		for (File f: fileMap)
		{
			if (filename.equals(f.getFilename()))
			{
				return true;
			}
		}
		return false;
	}

}
