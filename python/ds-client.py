#/usr/bin/env python3.7
from __future__ import annotations
from typing import Optional, Mapping, Any, List

import sys
import socket
from getpass import getuser
from dataclasses import dataclass
from xml.etree import ElementTree as ET


@dataclass
class Server:
    @classmethod
    def from_xml_elem_attr_dict(cls, data: Mapping[str, Any]):
        return cls(
            type=data['type'],
            limit=int(data['limit']),
            bootup_time=int(data['bootupTime']),
            rate=float(data['rate']),
            core_count=int(data['coreCount']),
            memory=int(data['memory']),
            disk=int(data['disk']),
        )

    type: str
    limit: int
    bootup_time: int
    rate: float
    core_count: int
    memory: int
    disk: int

@dataclass
class JobSubmission:
    @classmethod
    def from_received_line(cls, line: str):
        parts = line.split()
        if parts[0] != 'JOBN':
            raise ValueError

        return cls(
            submit_time=int(parts[1]),
            job_id=int(parts[2]),
            estimated_runtime=int(parts[3]),
            core_count=int(parts[4]),
            memory=int(parts[5]),
            disk=int(parts[6]),
        )

    submit_time: int
    job_id: int
    estimated_runtime: int
    core_count: int
    memory: int
    disk: int


class JobDispatchPolicy:
    def dispatch(self, scheduler: JobScheduler) -> None:
        raise NotImplementedError

class AllToLargest(JobDispatchPolicy):
    def dispatch(self, scheduler: JobScheduler) -> None:
        largest_server_type = max(
            scheduler.servers, key=lambda srv: srv.core_count
        ).type

        while True:
            scheduler.send('REDY')
            incoming = scheduler.receive()
            if incoming == 'NONE':
                break

            jobn = JobSubmission.from_received_line(incoming)
            scheduler.send(f"SCHD {jobn.job_id} {largest_server_type} 0")
            scheduler.receive()


class JobScheduler:
    def __init__(self,
        address: str,
        port: int,
        dispatch_policy: Optional[JobDispatchPolicy] = None,
    ) -> None:
        self._sock = sock = socket.socket()
        sock.connect((address, port))
        sock.settimeout(1)

        if dispatch_policy is None:
            dispatch_policy = AllToLargest()
        self.dispatch_policy = dispatch_policy

        self.servers: List[Server] = []

    def close(self) -> None:
        self._sock.close()

    def send(self, message: str) -> None:
        data = message.encode()
        self._sock.send(data)

    def receive(self) -> str:
        data = self._sock.recv(4096)
        return data.decode()

    def parse_for_servers(self, filename: str) -> List[Server]:
        tree = ET.parse(filename)
        root = tree.getroot()
        servers = []
        for elem in root.findall('servers/server'):
            server = Server.from_xml_elem_attr_dict(elem.attrib)
            servers.append(server)
        return servers

    def run(self) -> None:
        self.send('HELO')
        self.receive()
        self.send('AUTH ' + getuser())
        self.receive()

        self.servers.extend(self.parse_for_servers('system.xml'))

        self.dispatch_policy.dispatch(self)

        self.send("QUIT")
        self.receive()
        self.close()


scheduler = JobScheduler('127.0.0.1', 50000)
if not sys.flags.interactive:
    scheduler.run()
