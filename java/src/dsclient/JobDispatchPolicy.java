package dsclient;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

interface JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException;
}

class AllToLargest implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        String largestServerType = Collections.max(
            scheduler.servers, Comparator.comparing(srv -> srv.coreCount)
        ).type;

        while (true) {
            String incoming = scheduler.inquire("REDY");
            if (incoming.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(incoming);
            scheduler.inquire(String.format("SCHD %d %s 0", job.id, largestServerType));
        }
    }
}

class FirstAvailableWithSufficientResources implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        String largestServerType = Collections.max(
            scheduler.servers, Comparator.comparing(srv -> srv.coreCount)
        ).type;

        while (true) {
            String incoming = scheduler.inquire("REDY");
            if (incoming.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(incoming);

            scheduler.inquire(String.format("RESC Avail %d %d %d", job.coreCount, job.memory, job.disk));
            incoming = scheduler.inquire("OK");

            if (incoming.equals(".")) {
                // No server available with sufficient resources.
                // Fall back to the largest server type with ID 0.
                scheduler.inquire(String.format("SCHD %d %s 0", job.id, largestServerType));
            } else {
                Resource firstAvailable = Resource.fromReceivedLine(incoming);

                while (!incoming.equals(".")) {
                    incoming = scheduler.inquire("OK");
                }
                scheduler.inquire(String.format("SCHD %d %s %d", job.id, firstAvailable.type, firstAvailable.id));
            }
        }
    }
}
