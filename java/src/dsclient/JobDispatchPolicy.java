package dsclient;

import java.io.IOException;

public interface JobDispatchPolicy {
    public void dispatch(JobScheduler scheduler) throws IOException;
}
