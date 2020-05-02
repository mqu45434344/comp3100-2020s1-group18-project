
from __future__ import annotations
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from .job_scheduler import JobScheduler

from .models import Resource, JobSubmission

class JobDispatchPolicy:
    def dispatch(self, scheduler: JobScheduler) -> None:
        raise NotImplementedError

class AllToLargest(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        largest_server_type = max(
            scheduler.servers, key=lambda srv: srv.core_count
        ).type

        while True:
            incoming = scheduler.inquire('REDY')
            if incoming == 'NONE':
                break

            job = JobSubmission.from_received_line(incoming)
            scheduler.inquire(f"SCHD {job.id} {largest_server_type} 0")

class FirstAvailableWithSufficientResources(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        largest_server_type = max(
            scheduler.servers, key=lambda srv: srv.core_count
        ).type

        while True:
            incoming = scheduler.inquire('REDY')
            if incoming == 'NONE':
                break

            job = JobSubmission.from_received_line(incoming)

            scheduler.inquire("RESC Avail %d %d %d" % (job.core_count, job.memory, job.disk))
            incoming = scheduler.inquire('OK')

            if incoming == '.':
                # No server available with sufficient resources.
                # Fall back to the largest server type with ID 0.
                scheduler.inquire("SCHD %d %s 0" % (job.id, largest_server_type))

            else:
                first_available = Resource.from_received_line(incoming)

                while incoming != '.':
                    incoming = scheduler.inquire('OK')

                scheduler.inquire("SCHD %d %s %d" % (job.id, first_available.type, first_available.id))
