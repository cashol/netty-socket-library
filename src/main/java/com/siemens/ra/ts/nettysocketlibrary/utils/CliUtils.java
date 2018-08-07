package com.siemens.ra.ts.nettysocketlibrary.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliUtils {
  public static Options createCliOption() {
    Options options = new Options();
    Option portNumber = new Option("p", "port", true, "port number");
    portNumber.setRequired(true);
    options.addOption(portNumber);
    return options;
  }

  public static Options createCliOptions() {
    Options options = new Options();
    Option hostName = new Option("h", "host", true, "host name");
    hostName.setRequired(true);
    options.addOption(hostName);
    Option portNumber = new Option("p", "port", true, "port number");
    portNumber.setRequired(true);
    options.addOption(portNumber);
    return options;
  }

  public static CommandLine createCommandLine(String[] args, Options options) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    cmd = parser.parse(options, args);
    return cmd;
  }
}
