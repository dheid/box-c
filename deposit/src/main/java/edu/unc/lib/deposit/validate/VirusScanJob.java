/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.deposit.validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractConcurrentDepositJob;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
import fi.solita.clamav.ClamAVClient;
import fi.solita.clamav.ScanResult;

/**
 * Scans all staged files registered in the deposit for viruses.
 *
 * @author count0
 *
 */
public class VirusScanJob extends AbstractConcurrentDepositJob {
    private static final Logger log = LoggerFactory
            .getLogger(VirusScanJob.class);

    private ClamAVClient clamClient;

    public VirusScanJob() {
        super();
    }

    public void setClamClient(ClamAVClient clamClient) {
        this.clamClient = clamClient;
    }

    public VirusScanJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Running virus checks on : {}", getDepositDirectory());

        Map<String, String> failures = new HashMap<>();

        Model model = getReadOnlyModel();
        List<Entry<PID, String>> hrefs = getPropertyPairList(model, CdrDeposit.stagingLocation);

        setTotalClicks(hrefs.size());
        AtomicInteger scannedObjects = new AtomicInteger();

        for (Entry<PID, String> href : hrefs) {
            interruptJobIfStopped();

            PID objPid = href.getKey();

            if (isObjectCompleted(objPid)) {
                log.debug("Skipping already scanned file {} for {}", href.getValue(), objPid);
                scannedObjects.incrementAndGet();
                addClicks(1);
                continue;
            }

            waitForQueueCapacity();

            submitTask(() -> {
                if (isInterrupted.get()) {
                    return;
                }
                log.debug("Scanning file {} for object {}", href.getValue(), objPid);

                URI manifestURI = URI.create(href.getValue());
                File file = new File(manifestURI);

                ScanResult result;
                try {
                    result = clamClient.scanWithResult(new FileInputStream(file));
                } catch (IOException e) {
                    result = new ScanResult(e);
                }

                switch (result.getStatus()) {
                case FAILED:
                    failures.put(manifestURI.toString(), result.getSignature());
                    break;
                case ERROR:
                    Exception ex = result.getException();
                    String message = "Virus checks are producing errors for file '" + file
                            + "': " + result.getResult();
                    throw new RepositoryException(message, ex);
                case PASSED:
                    PID binPid = href.getKey();
                    PID parentPid = PIDs.get(binPid.getQualifier(), binPid.getId());
                    PremisLogger premisLogger = getPremisLogger(parentPid);
                    PremisEventBuilder premisEventBuilder = premisLogger.buildEvent(Premis.VirusCheck);

                    premisEventBuilder.addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.clamav))
                            .addEventDetail("File passed pre-ingest scan for viruses")
                            .addOutcome(true)
                            .write();

                    scannedObjects.incrementAndGet();

                    addClicks(1);
                    markObjectCompleted(objPid);

                    break;
                }
            });
        }

        waitForCompletion();

        if (failures.size() > 0) {
            StringBuilder sb = new StringBuilder("Virus checks failed for some files:\n");
            for (String uri : failures.keySet()) {
                sb.append(uri).append(" - ").append(failures.get(uri)).append("\n");
            }
            failJob(failures.size() + " virus check(s) failed.", sb.toString());
        } else {
            if (scannedObjects.get() != hrefs.size()) {
                failJob("Virus scan job did not attempt to scan all files.",
                        (hrefs.size() - scannedObjects.get()) + " objects were not scanned.");
            }

            PID depositPID = getDepositPID();
            PremisLogger premisDepositLogger = getPremisLogger(depositPID);
            premisDepositLogger.buildEvent(Premis.VirusCheck)
                    .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.clamav))
                    .addEventDetail(scannedObjects + "files scanned for viruses.")
                    .write();
        }
    }

    // unused, no results to flush
    @Override
    protected void registrationAction() {
    }
}