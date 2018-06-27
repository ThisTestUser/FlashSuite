package com.thistestuser.deirrfuscate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Deirrfuscator
{
	private File directory;
	
	public Deirrfuscator(String directory)
	{
		this.directory = new File(directory);
	}
	
	public void run() throws Throwable
	{
		System.out.println("Starting deirrfuscator...");
		List<File> files = new ArrayList<>();
		addFiles(directory.toString(), files);
		int counter = 0;
		for(File file : files)
		{
			BufferedReader in = new BufferedReader(new FileReader(file));
			String data = null;
			List<String> out = new ArrayList<>();
			try
			{
				while((data = in.readLine()) != null)
				{
					String trimData = data.trim();
					String[] array = trimData.split("\\s{2,}");
					int spaces = data.indexOf(data.trim());
					if(array.length == 2 && array[0].equals("findpropstrict") && array[1].equals("QName(PackageNamespace(\"\"), \"irrcrpt\")"))
					{
						String next = in.readLine();
						String next2 = in.readLine();
						if(next2 != null)
						{
							String trimNext = next.trim();
							String[] array1 = trimNext.split("\\s{2,}");
							String trimNext2 = next2.trim();
							String[] array2 = trimNext2.split("\\s{2,}");
							if(array1.length == 2 && array1[0].equals("pushstring")
								&& array2.length == 2 && isInteger(array2[1]))
							{
								String next3 = in.readLine();
								if(next3 != null)
								{
									String trimNext3 = next3.trim();
									String[] array3 = trimNext3.split("\\s{2,}");
									if(array3.length == 2 && array3[0].equals("callproperty")
										&& array3[1].equals("QName(PackageNamespace(\"\"), \"irrcrpt\"), 2"))
									{
										String decryptedString = decryptCipher(array1[1].substring(1, array1[1].length() - 1), Integer.parseInt(array2[1]));
										StringBuilder builder = new StringBuilder(spaces);
								        for(int space = 0; space < spaces ; space++) 
								            builder.append(' ');
										out.add(builder.toString() + "pushstring          \"" + escape(decryptedString) + "\"");
										counter++;
									}else
									{
										out.add(data);
										out.add(next);
										out.add(next2);
										out.add(next3);
									}
								}else
								{
									out.add(data);
									out.add(next);
									out.add(next2);
								}
							}else
							{
								out.add(data);
								out.add(next);
								out.add(next2);
							}
						}else
							out.add(data);
					}else
						out.add(data);
				}
			}catch(Exception e)
			{
				e.printStackTrace();
				continue;
			}
			in.close();
			//Clear file
			PrintWriter pw = new PrintWriter(file);
			pw.close();
	        //Then write
			FileWriter fw = new FileWriter(file, false);
	        BufferedWriter writer = new BufferedWriter(fw);
	        for (String line : out)
	            writer.write(line + "\n");
	        writer.close();
		}
		System.out.println("Decrypted " + counter + " string encryption calls");
		System.out.println("Done");
	}
	
	private String decryptCipher(String str, int shift)
	{
		int index = 0;
		int stringChar = 0;
		String result = "";
		while(index < str.length())
		{
			stringChar = str.charAt(index);
			if(stringChar >= 48 && stringChar <= 57)
			{
				stringChar = stringChar - shift - 48;
				if(stringChar < 0)
					stringChar = stringChar + 10;
				stringChar = stringChar % 10 + 48;
			}else if(stringChar >= 65 && stringChar <= 90)
			{
				stringChar = stringChar - shift - 65;
				if(stringChar < 0)
					stringChar = stringChar + 26;
				stringChar = stringChar % 26 + 65;
			}else if(stringChar >= 97 && stringChar <= 122)
			{
				stringChar = stringChar - shift - 97;
				if(stringChar < 0)
					stringChar = stringChar + 26;
				stringChar = stringChar % 26 + 97;
			}
			result = result + fromCharCode(stringChar);
			index++;
		}
		return result;
	}
	
	private String escape(String str)
	{
		//Actually replaces \ with \\ and " with \"
		return str.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	private boolean isInteger(String str)
	{
		try
		{
			Integer.parseInt(str);
			return true;
		}catch(NumberFormatException e)
		{
			return false;
		}
	}
	
	public void addFiles(String directoryName, List<File> files)
	{
	    File directory = new File(directoryName);
	    File[] fList = directory.listFiles();
	    for(File file : fList)
	        if(file.isFile() && file.toString().endsWith(".asasm"))
	        	files.add(file);
	        else if (file.isDirectory())
	        	addFiles(file.getAbsolutePath(), files);
	}
	
	private String fromCharCode(int... codes) 
	{
        StringBuilder builder = new StringBuilder(codes.length);
        for(int code : codes)
            builder.append(Character.toChars(code));
        return builder.toString();
    }
}
