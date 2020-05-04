
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from ..job_scheduler import JobScheduler

from ..job_dispatch_policy import JobDispatchPolicy
from ..models import JobSubmission

class AllToLargest(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        largest_server_type = max(
            scheduler.servers,
            key=lambda srv: srv.core_count,
        ).type

        while True:
            received = scheduler.inquire('REDY')
            if received == 'NONE':
                break

            job = JobSubmission.from_received_line(received)

            scheduler.inquire(f"SCHD {job.id} {largest_server_type} 0")
