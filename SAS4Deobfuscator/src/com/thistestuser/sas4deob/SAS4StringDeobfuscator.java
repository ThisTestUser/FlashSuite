package com.thistestuser.sas4deob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SAS4StringDeobfuscator
{
	private File directory;
	
	public SAS4StringDeobfuscator(String directory)
	{
		this.directory = new File(directory);
	}
	
	public void run() throws Throwable
	{
		System.out.println("Starting string deobfuscator...");
		String chop = directory.toString().substring(0, directory.toString().length() - 2);
		File deobBin = new File(chop + "-2.bin");
		FileInputStream stream = new FileInputStream(deobBin);
		String res = "";
		int i = 0;
		while(true)
		{
			int next = stream.read();
			if(next == -1)
				break;
			res += fromCharCode((next + i) % 256);
			i++;
		}
		List<File> files = new ArrayList<>();
		String[] split = res.split("#_STR_#");
		System.out.println("Found " + split.length + " decrypted strings");
		addFiles(directory.toString(), files);
		int counter = 0;
		//Search string decryptor
		String className = null;
		String methodName = null;
		finder:
		for(File file : files)
			if(file.getName().endsWith(".class.asasm"))
			{
				BufferedReader in = new BufferedReader(new FileReader(file));
				String data = null;
				String currentMethod = null;
				try
				{
					while((data = in.readLine()) != null)
					{
						String first = data.trim();
						String[] array = first.split("\\s{2,}");
						if(array.length == 1 && array[0].equals("method"))
						{
							data = in.readLine().trim();
							if(data != null)
							{
								String[] array2 = data.split("\\s{1,}");
								if(array2.length == 2 && array2[0].equals("name"))
									currentMethod = array2[1].replace("\"", "");
							}
						}else if(array.length == 2 && array[0].equals("pushbyte") && isInteger(array[1]) 
							&& Integer.parseInt(array[1]) == 35)
						{
							List<String> next = new ArrayList<>();
							next.add(in.readLine().trim());
							next.add(in.readLine().trim());
							next.add(in.readLine().trim());
							next.add(in.readLine().trim());
							next.add(in.readLine().trim());
							next.add(in.readLine().trim());
							boolean anyFail = false;
							for(String s : next)
								if(s == null)
									anyFail = true;
								else
								{
									String[] array2 = s.split("\\s{2,}");
									if(array2.length != 2 || !array2[0].equals("pushbyte") || !isInteger(array2[1]))
										anyFail = true;
								}
							if(!anyFail)
							{
								int second = Integer.parseInt(next.get(0).split("\\s{2,}")[1]);
								int third = Integer.parseInt(next.get(1).split("\\s{2,}")[1]);
								int fourth = Integer.parseInt(next.get(2).split("\\s{2,}")[1]);
								int fifth = Integer.parseInt(next.get(3).split("\\s{2,}")[1]);
								int sixth = Integer.parseInt(next.get(4).split("\\s{2,}")[1]);
								int seventh = Integer.parseInt(next.get(5).split("\\s{2,}")[1]);
								if(second == 95 && third == 83 && fourth == 84
									&& fifth == 82 && sixth == 95 && seventh == 35)
								{
									className = file.getName().replace(".class.asasm", "");
									methodName = currentMethod;
									break finder;
								}
							}
						}
					}
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				in.close();
			}
		System.out.println("Decryptor class: " + className);
		System.out.println("Decryptor method: " + methodName);
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
					if(array.length == 2 && array[0].equals("getlex") && array[1].equals("QName(PackageNamespace(\"\"), \"" + className + "\")"))
					{
						String next = in.readLine();
						if(next != null)
						{
							String trimNext = next.trim();
							String[] array2 = trimNext.split("\\s{2,}");
							if(array2.length == 2 && isInteger(array2[1]))
							{
								String next2 = in.readLine();
								if(next2 != null)
								{
									String trimNext2 = next2.trim();
									String[] array3 = trimNext2.split("\\s{2,}");
									if(array3.length == 2 && array3[0].equals("callproperty")
										&& array3[1].equals("QName(PackageNamespace(\"\"), \"" + methodName + "\"), 1"))
									{
										StringBuilder builder = new StringBuilder(spaces);
								        for(int space = 0; space < spaces ; space++) 
								            builder.append(' ');
										out.add(builder.toString() + "pushstring          \"" + escape(split[Integer.parseInt(array2[1])]) + "\"");
										counter++;
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
								}
							}else
							{
								out.add(data);
								out.add(next);
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
		stream.close();
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
