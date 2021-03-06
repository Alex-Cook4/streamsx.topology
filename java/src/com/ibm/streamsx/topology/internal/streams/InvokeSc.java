/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.internal.streams;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.streamsx.topology.Topology;
import com.ibm.streamsx.topology.internal.process.ProcessOutputToLogger;

public class InvokeSc {

    static final Logger trace = Topology.STREAMS_LOGGER;

    private Set<File> toolkits = new HashSet<File>();
    private final boolean standalone;
    private final String namespace;
    private final String mainComposite;
    private final File applicationDir;

    public InvokeSc(boolean standalone, String namespace, String mainComposite,
            File applicationDir) throws URISyntaxException, IOException {
        super();
        this.standalone = standalone;
        this.namespace = namespace;
        this.mainComposite = mainComposite;
        this.applicationDir = applicationDir;

        addFunctionalToolkit();
    }

    public void addToolkit(File toolkitDir) throws IOException {
        toolkits.add(toolkitDir.getCanonicalFile());
    }

    private void addFunctionalToolkit() throws URISyntaxException, IOException {
        URL location = getClass().getProtectionDomain().getCodeSource()
                .getLocation();

        // Assumption it is at lib in the toolkit.

        Path functionaljar = Paths.get(location.toURI());
        File tkRoot = functionaljar.getParent().getParent().toFile();
        addToolkit(tkRoot);
    }

    public void invoke() throws Exception, InterruptedException {
        String si = System.getenv("STREAMS_INSTALL");
        File sc = new File(si, "bin/sc");

        List<String> commands = new ArrayList<String>();

        String mainCompositeName = namespace + "::" + mainComposite;

        commands.add(sc.getAbsolutePath());
        commands.add("--optimized-code-generation");
        if (standalone)
            commands.add("--standalone");

        commands.add("-M");
        commands.add(mainCompositeName);

        commands.add("-t");
        commands.add(getToolkitPath());

        trace.info("Invoking SPL compiler (sc) for main composite: "
                + mainCompositeName);

        ProcessBuilder pb = new ProcessBuilder(commands);
        
        // Force the SPL application to use the Java provided by
        // Streams to ensure the bundle is not dependent on the
        // local JVM install path.
        pb.environment().remove("JAVA_HOME");
        
        pb.directory(applicationDir);

        Process scProcess = pb.start();
        ProcessOutputToLogger.log(trace, scProcess);
        scProcess.getOutputStream().close();
        int rc = scProcess.waitFor();
        trace.info("SPL compiler complete: return code=" + rc);
        if (rc != 0)
            throw new Exception("SPL compilation failed!");
    }

    private String getToolkitPath() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (File tk : toolkits) {
            if (!first)
                sb.append(":");
            else
                first = false;

            sb.append(tk.getAbsolutePath());
        }

        String splPath = System.getenv("STREAMS_SPLPATH");
        if (splPath != null) {
            sb.append(":");
            sb.append(splPath);
        }

        String streamsInstall = System.getenv("STREAMS_INSTALL");
        String streamsInstallToolkits = streamsInstall + "/toolkits";
        sb.append(":");
        sb.append(streamsInstallToolkits);

        trace.info("ToolkitPath:" + sb.toString());
        return sb.toString();
    }

}
