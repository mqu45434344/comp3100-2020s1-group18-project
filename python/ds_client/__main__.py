#/usr/bin/env python3

import sys
import os.path as op

if __package__ is None:
    __package__ = op.basename(op.dirname(op.realpath(__file__)))
    sys.path[0] = op.dirname(sys.path[0])

from .job_scheduler import JobScheduler
from .scheduling_policies import AllToLargest

def main() -> None:
    args = sys.argv[1:]
    args_set = set(args)

    scheduler = JobScheduler('127.0.0.1', 50000, AllToLargest())
    scheduler.newlines = '-n' in args_set

    if not sys.flags.interactive:
        scheduler.run()

if __name__ == '__main__':
    main()
