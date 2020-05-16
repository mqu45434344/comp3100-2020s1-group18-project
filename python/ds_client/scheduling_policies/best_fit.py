
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission

class BestFit(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
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

            best = None
            if sufficient:
                best = min(
                    sufficient,
                    key=lambda res: (
                        res.core_count - job.core_count,
                        res.available_time,
                    ),
                )
            else:
                best = resources[0]

            scheduler.schedule(job, best)
