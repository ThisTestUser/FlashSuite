package com.thistestuser.unsecureswf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.thistestuser.unsecureswf.transformers.Transformers;

public class FlowDeobfuscator
{
	private File directory;
	
	public FlowDeobfuscator(String directory)
	{
		this.directory = new File(directory);
	}
	
	public void run() throws Throwable
	{
		System.out.println("Starting SecureSWF flow deobfuscator...");
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
				int params = 0;
				while((data = in.readLine()) != null)
				{
					if(data.trim().startsWith("param"))
						params++;
					if(data.trim().startsWith("refid"))
						params = 0;
					out.add(data);
					if(data.trim().equals("code"))
					{
						List<String> code = new ArrayList<>();
						while(true)
						{
							data = in.readLine();
							if(data.trim().equals("end ; code"))
								break;
							code.add(data);
						}
						if(code.size() > 2 && code.get(0).trim().equals("pushfalse")
							&& code.get(1).trim().equals("pushtrue"))
						{
							Iterator<String> iterator = code.iterator();
							while(iterator.hasNext())
							{
								String s = iterator.next();
								if(s.trim().startsWith("; 0x"))
									iterator.remove();
							}
							Transformers.transformSwap(code);
							Transformers.transformContJump(code);
							Transformers.transformPop(code);
							Transformers.transformFakeJumps(code);
							out.addAll(code);
							counter++;
						}else
						{
							Iterator<String> iterator = code.iterator();
							while(iterator.hasNext())
							{
								String s = iterator.next();
								if(s.trim().startsWith("; 0x"))
									iterator.remove();
							}
							Transformers.transformPop(code);
							Transformers.transformContJump(code);
							boolean modified = false;
							if(Transformers.transformFakeLocals(code, params))
							{
								counter++;
								modified = true;
							}
							if(Transformers.transformRedundantJump(code, params) && !modified)
							{
								counter++;
								modified = true;
							}
							out.addAll(code);
						}
						out.add(data);
					}
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
		System.out.println("Fixed " + counter + " methods");
		System.out.println("Done");
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
}
