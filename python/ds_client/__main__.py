#/usr/bin/env python3

import sys
import os.path as op

if __package__ is None:
    __package__ = op.basename(op.dirname(op.realpath(__file__)))
    sys.path[0] = op.dirname(sys.path[0])

from .job_scheduler import JobScheduler
from .scheduling_policies import (
    AllToLargest,
    FirstFit,
    BestFit,
)

def main() -> None:
    args = sys.argv[1:]
    args_set = set(args)

    algo = 'al'
    if '-a' in args_set:
        idx = args.index('-a')
        try:
            algo = args[idx + 1]
        except IndexError:
            raise Exception('a value must be specified for option -a') from None

    algos = {
        'al': AllToLargest,
        'ff': FirstFit,
        'bf': BestFit,
    }
    try:
        dispatch_policy = algos[algo]()
    except KeyError:
        raise Exception('algorithm not supported') from None

    scheduler = JobScheduler('127.0.0.1', 50000, dispatch_policy)
    scheduler.newlines = '-n' in args_set

    if not sys.flags.interactive:
        scheduler.run()

if __name__ == '__main__':
    main()
