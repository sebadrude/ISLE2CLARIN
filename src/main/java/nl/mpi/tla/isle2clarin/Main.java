/*
 * Copyright (C) 2014 The Language Archive - Max Planck Institute for Psycholinguistics
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.mpi.tla.isle2clarin;

import eu.clarin.cmdi.validator.CMDIValidationHandlerAdapter;
import eu.clarin.cmdi.validator.CMDIValidationReport;
import eu.clarin.cmdi.validator.CMDIValidator;
import eu.clarin.cmdi.validator.CMDIValidatorConfig;
import eu.clarin.cmdi.validator.CMDIValidatorException;
import eu.clarin.cmdi.validator.SimpleCMDIValidatorProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import nl.mpi.tla.schemanon.Message;
import nl.mpi.tla.schemanon.SchemAnon;
import nl.mpi.translation.tools.Translator;
import nl.mpi.translation.tools.TranslatorImpl;
import nl.mpi.translation.tools.UrlStreamResolverImpl;
import org.apache.commons.io.FileUtils;

/**
 * @author Menzo Windhouwer
 */
public class Main {
    public static void main(String[] args) {
        try {
            // initialize CMDI2IMDI
            boolean validateIMDI = false;
            boolean validateCMDI = false;
            TreeSet<String> skip = new TreeSet<>();
            Translator imdi2cmdi = new TranslatorImpl(new UrlStreamResolverImpl());
            SchemAnon tron = new SchemAnon(Main.class.getResource("/IMDI_3.0.xsd"));
            // check command line
            OptionParser parser = new OptionParser( "ics:?*" );
            OptionSet options = parser.parse(args);
            if (options.has("i"))
                validateIMDI = true;
            if (options.has("c"))
                validateCMDI = true;
            if (options.has("s"))
                skip = loadSkipList((String)options.valueOf("s"));
            if (options.has("?")) {
                showHelp();
                System.exit(0);
            }
            List arg = options.nonOptionArguments();
            if (arg.size()<1 && arg.size()>2) {
                System.err.println("FTL: none or too many non-option arguments!");
                showHelp();
                System.exit(1);
            }
            if (arg.size()>1) {
                if (options.has("s")) {
                    System.err.println("FTL: -s option AND <FILE> argument, use only one!");
                    showHelp();
                    System.exit(1);
                }
                skip = loadSkipList((String)arg.get(1));
            }
            Collection<File> inputs = null;
            File in = new File((String)arg.get(0));
            if (in.isDirectory()) {
                inputs = FileUtils.listFiles(in,new String[] {"imdi"},true);
            } else if (in.isFile()) {
                inputs = loadInputList(in);
            } else {
                System.err.println("FTL: unknown type of <INPUT>!");
                showHelp();
                System.exit(1);
            }
            int i = 0;
            int s = inputs.size();
            for (File input:inputs) {
                i++;
                try {
                    String path = input.getAbsolutePath();
                    //System.err.println("DBG: absolute path["+path+"]");
                    //System.err.println("DBG: relative path["+path.replaceAll("^" + in.getAbsolutePath() + "/", "")+"]");
                    if (input.isHidden()) {
                        System.err.println("WRN:"+i+"/"+s+": file["+path+"] is hidden, skipping it.");
                        continue;
                    } else if (path.matches(".*/(corpman|sessions)/.*")) {
                        System.err.println("WRN:"+i+"/"+s+": file["+path+"] is in a corpman or sessions dir, skipping it.");
                        continue;
                    } else if (skip.contains(path.replaceAll("^" + in.getAbsolutePath() + "/", ""))) {
                        System.err.println("WRN:"+i+"/"+s+": file["+path+"] is in the skip list, skipping it.");
                        continue;
                    } else if (skip.contains(path)) {
                        System.err.println("WRN:"+i+"/"+s+": file["+path+"] is in the skip list, skipping it.");
                        continue;
                    } else
                        System.err.println("DBG:"+i+"/"+s+": convert file["+path.replaceAll("^" + (String)arg.get(0) + "/", "")+"]");
                    if (validateIMDI) {
                        // validate IMDI
                        if (!tron.validate(input)) {
                            System.err.println("ERR:"+i+"/"+s+": invalid file["+input.getAbsolutePath()+"]");
                            for (Message msg : tron.getMessages()) {
                                System.out.println("" + (msg.isError() ? "ERR: " : "WRN: ") + i+"/"+s+": " + (msg.getLocation() != null ? "at " + msg.getLocation() : ""));
                                System.out.println("" + (msg.isError() ? "ERR: " : "WRN: ") + i+"/"+s+": " + msg.getText());
                            }
                        } else
                            System.err.println("DBG:"+i+"/"+s+": valid file["+input.getAbsolutePath()+"]");
                    }
                    // IMDI 2 CMDI
                    File output = new File(input.getAbsolutePath().replaceAll("\\.imdi$", ".cmdi"));
                    PrintWriter out = new PrintWriter(output.getAbsolutePath());
                    Map<String, Object> params = new HashMap<>();
                    params.put("formatCMDI", Boolean.FALSE);
                    imdi2cmdi.setTransformationParameters(params);
                    out.print(imdi2cmdi.getCMDI(input.toURI().toURL(), ""));
                    out.close();
                    System.err.println("DBG:"+i+"/"+s+": wrote   file["+output.getAbsolutePath()+"]");
                    if (validateCMDI) {
                        CMDIValidatorConfig.Builder builder = new CMDIValidatorConfig.Builder(output, new Handler());
                        CMDIValidator validator = new CMDIValidator(builder.build());
                        SimpleCMDIValidatorProcessor processor = new SimpleCMDIValidatorProcessor();
                        processor.process(validator);
                    }
                } catch(Exception ex) {
                    System.err.println("ERR:"+i+"/"+s+":"+input+":"+ex);
                    ex.printStackTrace(System.err);
                }
            }
        } catch(Exception ex) {
            System.err.println("FTL: "+ex);
            ex.printStackTrace(System.err);
        }
    }
    
    private static void showHelp() {
        System.err.println("INF: isle2clarin <options> -- <INPUT> <SKIP>?");
        System.err.println("INF: <INPUT>   directory to recurse for IMDI files, or file with file paths (one per line) to process");
        System.err.println("INF: <FILE>    file with file paths (one per line) to skip during processing (deprecated, better use the -s option)");
        System.err.println("INF: isle2clarin options:");
        System.err.println("INF: -i        enable IMDI validation (optional)");
        System.err.println("INF: -c        enable CMDI validation (optional)");
        System.err.println("INF: -s=<FILE> file with file paths (one per line) to skip during processing (optional)");
    }
    
    private static TreeSet<String> loadSkipList(String file) throws Exception {
        TreeSet<String> skip = new TreeSet<>();
        File sfile = new File(file);
        if (sfile.exists()) {
            BufferedReader sin = new BufferedReader(new InputStreamReader(new FileInputStream(sfile)));
            String line;
            while ((line = sin.readLine()) != null) {
                //if (line.startsWith("/")) {
                    line = line.trim();
                    System.err.println("DBG: skip["+line+"]");
                    skip.add(line);
                //}
            }
        }
        return skip;
    }
    
    private static Collection<File> loadInputList(File file) throws Exception {
        TreeSet<File> input = new TreeSet<>();
        if (file.exists()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                //System.err.println("DBG: input["+line+"]");
                File inf = new File(line);
                if (inf.exists())
                    input.add(inf);
                else
                    System.err.println("ERR: file["+inf.getAbsolutePath()+"] doesn't exist!");
            }
        }
        return input;
    }
    
    private static class Handler extends CMDIValidationHandlerAdapter {
        @Override
        public void onValidationReport(final CMDIValidationReport report)
                throws CMDIValidatorException {
            final File file = report.getFile();
            int skip = 0;
            switch (report.getHighestSeverity()) {
            case INFO:
                System.err.println("DBG: file["+file+"] is valid");
                break;
            case WARNING:
                System.err.println("WRN: file ["+file+"] is valid (with warnings):");
                for (CMDIValidationReport.Message msg : report.getMessages()) {
                    if (msg.getMessage().contains("Failed to read schema document ''")) {
                        skip++;
                        continue;
                    }
                    if ((msg.getLineNumber() != -1) &&
                            (msg.getColumnNumber() != -1)) {
                        System.err.println(" ("+msg.getSeverity().getShortcut()+") "+msg.getMessage()+" [line="+msg.getLineNumber()+", column="+msg.getColumnNumber()+"]");
                    } else {
                        System.err.println(" ("+msg.getSeverity().getShortcut()+") "+msg.getMessage());
                    }
                }
                break;
            case ERROR:
                System.err.println("ERR: file ["+file+"] is invalid:");
                for (CMDIValidationReport.Message msg : report.getMessages()) {
                    if (msg.getMessage().contains("Failed to read schema document ''")) {
                        skip++;
                        continue;
                    }
                    if ((msg.getLineNumber() != -1) &&
                            (msg.getColumnNumber() != -1)) {
                        System.err.println(" ("+msg.getSeverity().getShortcut()+") "+msg.getMessage()+" [line="+msg.getLineNumber()+", column="+msg.getColumnNumber()+"]");
                    } else {
                        System.err.println(" ("+msg.getSeverity().getShortcut()+") "+msg.getMessage());
                    }
                }
                break;
            default:
                throw new CMDIValidatorException("unexpected severity: " +
                        report.getHighestSeverity());
            } // switch
            if (skip>0)
                System.err.println("WRN: skipped ["+skip+"] warnings due to lax validation of foreign namespaces");
        }
    } // class Handler    
}
