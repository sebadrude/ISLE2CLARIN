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
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import nl.mpi.tla.schemanon.Message;
import nl.mpi.tla.schemanon.SaxonUtils;
import nl.mpi.tla.schemanon.SchemAnon;
import nl.mpi.translation.tools.Translator;
import nl.mpi.translation.tools.TranslatorImpl;
import org.apache.commons.io.FileUtils;

/**
 * @author Menzo Windhouwer
 */
public class Main {
    public static void main(String[] args) {
        try {
            // initialize CMDI2IMDI
            Translator imdi2cmdi = new TranslatorImpl();
            SchemAnon tron = new SchemAnon(Main.class.getResource("/IMDI_3.0.xsd"));
            // check command line
            if (args.length<1 || args.length>2) {
                System.err.println("FTL: missing arguments: <input dir> <skip list>?");
                System.exit(1);
            }
            String dir = args[0];
            TreeSet<String> skip = new TreeSet<String>();
            if (args.length>1) {
                File sfile = new File(args[1]);
                if (sfile.exists()) {
                    BufferedReader sin = new BufferedReader(new InputStreamReader(new FileInputStream(sfile)));
                    String line = null;
                    while ((line = sin.readLine()) != null) {
                        //if (line.startsWith("/")) {
                            line = line.trim();
                            System.err.println("DBG: skip["+line+"]");
                            skip.add(line);
                        //}
                    }
                }
            }
            File fdir = new File(dir);
            Collection<File> inputs = FileUtils.listFiles(fdir,new String[] {"imdi"},true);
            for (File input:inputs) {
                try {
                    String path = input.getAbsolutePath();
                    System.err.println("DBG: absolute path["+path+"]");
                    System.err.println("DBG: relative path["+path.replaceAll("^" + fdir.getAbsolutePath() + "/", "")+"]");
                    if (input.isHidden()) {
                        System.err.println("WRN: file["+path+"] is hidden, skipping it.");
                        continue;
                    } else if (path.matches(".*/(corpman|sessions)/.*")) {
                        System.err.println("WRN: file["+path+"] is in a corpman or sessions dir, skipping it.");
                        continue;
                    } else if (skip.contains(path.replaceAll("^" + fdir.getAbsolutePath() + "/", ""))) {
                        System.err.println("WRN: file["+path+"] is in the skip list, skipping it.");
                        continue;
                    } else if (skip.contains(path)) {
                        System.err.println("WRN: file["+path+"] is in the skip list, skipping it.");
                        continue;
                    } else
                        System.err.println("DBG: convert file["+path.replaceAll("^" + dir + "/", "")+"]");
                    // validate IMDI
                    if (!tron.validate(input)) {
                        System.err.println("ERR: invalid file["+input.getAbsolutePath()+"]");
                        for (Message msg : tron.getMessages()) {
                            System.out.println("" + (msg.isError() ? "ERR: " : "WRN: ") + (msg.getLocation() != null ? "at " + msg.getLocation() : ""));
                            System.out.println("" + (msg.isError() ? "ERR: " : "WRN: ") + msg.getText());
                        }
                    } else
                        System.err.println("DBG: valid file["+input.getAbsolutePath()+"]");
                    // IMDI 2 CMDI
                    File output = new File(input.getAbsolutePath().replaceAll("\\.imdi", ".cmdi"));
                    PrintWriter out = new PrintWriter(output.getAbsolutePath());
                    out.print(imdi2cmdi.getCMDI(input.toURI().toURL(), ""));
                    out.close();
                    System.err.println("DBG: wrote file["+output.getAbsolutePath()+"]");
                    CMDIValidatorConfig.Builder builder = new CMDIValidatorConfig.Builder(output, new Handler());
                    CMDIValidator validator = new CMDIValidator(builder.build());
                    SimpleCMDIValidatorProcessor processor = new SimpleCMDIValidatorProcessor();
                    processor.process(validator);
                } catch(Exception ex) {
                    System.err.println("ERR:"+input+":"+ex);
                    ex.printStackTrace(System.err);
                }
            }
        } catch(Exception ex) {
            System.err.println("FTL: "+ex);
            ex.printStackTrace(System.err);
        }
    }
    
    private static class Handler extends CMDIValidationHandlerAdapter {
        @Override
        public void onValidationReport(final CMDIValidationReport report)
                throws CMDIValidatorException {
            final File file = report.getFile();
            switch (report.getHighestSeverity()) {
            case INFO:
                System.err.println("DBG: file["+file+"] is valid");
                break;
            case WARNING:
                System.err.println("WRN: file ["+file+"] is valid (with warnings):");
                int skip = 0;
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
                if (skip>0)
                    System.err.println("WRN: skipped ["+skip+"] warnings due to lax validation of foreign namespaces");
                break;
            case ERROR:
                System.err.println("ERR: file ["+file+"] is invalid:");
                for (CMDIValidationReport.Message msg : report.getMessages()) {
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
        }
    } // class Handler    
}
