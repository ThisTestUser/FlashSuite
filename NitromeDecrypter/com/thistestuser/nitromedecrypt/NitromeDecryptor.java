package com.thistestuser.nitromedecrypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class NitromeDecryptor
{
	private File directory;
	
	public NitromeDecryptor(String directory)
	{
		this.directory = new File(directory);
	}
	
	public void run() throws Throwable
	{
		System.out.println("Starting Nitrome decryptor...");
		byte[] data = Files.readAllBytes(directory.toPath());
		int i = 0;
        while(i < data.length)
        {
        	data[i] = (byte)(data[i] - (i % data.length));
        	i++;
        }
		try(FileOutputStream fos = new FileOutputStream(directory.getParentFile().toString() + "/decrypted.swf")) 
		{
			fos.write(decompress(data));
		}
		System.out.println("Done");
	}
	
	public static byte[] decompress(byte[] data) throws IOException, DataFormatException 
	{  
		Inflater inflater = new Inflater();   
		inflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
		byte[] buffer = new byte[1024];  
		while(!inflater.finished()) 
		{  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		return outputStream.toByteArray();  
	}  
}
