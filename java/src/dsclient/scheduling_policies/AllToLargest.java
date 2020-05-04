package dsclient.scheduling_policies;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import dsclient.JobDispatchPolicy;
import dsclient.JobScheduler;
import dsclient.models.JobSubmission;

public class AllToLargest implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        String largestServerType = Collections.max(
            scheduler.servers,
            Comparator.comparing(srv -> srv.coreCount)
        ).type;

        while (true) {
            String received = scheduler.inquire("REDY");
            if (received.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(received);

            scheduler.inquire(String.format("SCHD %d %s 0", job.id, largestServerType));
        }
    }
}
