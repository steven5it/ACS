
public class File 
{
	private String file;
	private String user;
	private short mode;
	public File()
	{
		file = null;
		user = null;
		mode = 0;
	}
	public File(String file, String user, String mode)
	{
		this.file = file;
		this.user = user;
		this.mode = string2Octal(mode);
	}
	public String getFilename() { 
		return file;
	}
	public String getUser() {
		return user;
	}
	public short getMode() {
		return mode;
	}
	
	/* if chmod is valid, then set the new mode given String */
	public void setMode(String mode) 
	{
		this.mode = string2Octal(mode);
	}
	/* if chmod is valid, then setnew mode given short */
	public void setMode(short mode)
	{
		this.mode = mode;
	}
	
	/* return boolean value representing whether bit is set at position indicated */
	public boolean isBitSet(int bit)
	{
		int temp = 1 << bit;
		return ((temp & this.mode) == 1);
	}
	
	private short string2Octal(String str)
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
	public void printFile()
	{
		System.out.println("Filename: " + file + ", user: " + user + ", mode: " + mode);
		System.out.println("Mode octal: " + int2Octal(mode));
	}
	public static int int2Octal(int octal)
	{
		int count = 0;
		int result = 0;
		while (octal != 0)
		{
			int temp = (int) ((octal%8) * Math.pow(10, count));
			count ++;
			result += temp;
			octal /= 8;
		}
		return result;
	}
}
