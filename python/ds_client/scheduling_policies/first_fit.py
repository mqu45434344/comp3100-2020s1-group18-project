
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission

class FirstFit(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        server_types = scheduler.servers.copy()
        server_types.sort(key=lambda srv: srv.core_count)
        server_type_order = {srv.type: i for i, srv in enumerate(server_types)}

        while True:
            received = scheduler.inquire('REDY')
            if received == 'NONE':
                break

            job = JobSubmission.from_received_line(received)

            resources = scheduler.fetch_capable_resources(job)
            sufficient = [
                res for res in resources
                if (
                    res.core_count >= job.core_count
                    and res.disk >= job.disk
                    and res.memory >= job.memory
                )
            ]

            sufficient.sort(key=lambda res: server_type_order[res.type])
            first = (sufficient if sufficient else resources)[0]

            scheduler.schedule(job, first)
