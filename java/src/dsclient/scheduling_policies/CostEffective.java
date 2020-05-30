package dsclient.scheduling_policies;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import dsclient.JobDispatchPolicy;
import dsclient.JobScheduler;
import dsclient.enums.ServerState;
import dsclient.models.JobSubmission;
import dsclient.models.Resource;

public class CostEffective implements JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException {
        while (true) {
            String received = scheduler.inquire("REDY");
            if (received.equals("NONE")) {
                break;
            }

            JobSubmission job = JobSubmission.fromReceivedLine(received);

            List<Resource> resources = scheduler.fetchCapableResources(job);

            Resource best = resources.stream().max(
                Comparator.<Resource>comparingInt(
                    res -> -(res.coreCount - job.coreCount)
                ).thenComparingInt(
                    res -> (res.state == ServerState.IDLE) ? 1 : 0
                ).thenComparingInt(res -> -res.availableTime)
            ).get();

            scheduler.schedule(job, best);
        }
    }
}
