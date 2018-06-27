package com.thistestuser.unsecureswf;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main 
{
	public static void main(String[] args) throws Throwable
    {
		//Command line args: -directory
		Options options = new Options();

        Option directory = new Option("dir", "directory", true, "Directory of SWF ASASMs");
        directory.setRequired(true);
        options.addOption(directory);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try 
        {
            cmd = parser.parse(options, args);
        }catch(ParseException e) 
        {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
		new FlowDeobfuscator(cmd.getOptionValue("directory")).run();
    }
}
