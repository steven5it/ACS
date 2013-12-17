import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* Class to represent mapping of users to groups */
public class Users 
{
	private HashMap <String, String> userMap;
	public Users()
	{
		userMap = new HashMap <String, String>();
	}
	public void addUserList(BufferedReader br) throws IOException
	{
		String line;
		while ((line = br.readLine()) != null)
		{
			String [] split = line.split("\\s+");
			if (split.length != 2)
			{
				continue;
			}
			// add user and group to mapping
			addUser (split[0].toLowerCase(), split[1].toLowerCase());
		}
	}
	
	
	public void addUser(String user, String group)
	{
		userMap.put(user, group);
	}
	
	/* returns group for given user */
	public String getGroup (String user)
	{
		return userMap.get(user);
	}
	
	public void printMap()
	{
		for (Map.Entry<String, String> entry: userMap.entrySet())
		{
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
	
}
