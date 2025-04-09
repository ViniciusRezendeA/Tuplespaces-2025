import sys
import os

# Adiciona o caminho dos arquivos gerados
proto_path = os.path.abspath("../Contract/target/generated-sources/protobuf/python")
if proto_path not in sys.path:
    sys.path.insert(0, proto_path)

import grpc
from TupleSpaces_pb2_grpc import *
from TupleSpaces_pb2 import *
from ReplicaServer_pb2 import *


class ClientService:
    def __init__(self, host_port: str, client_id: int):
        self.channel = grpc.insecure_channel(host_port)
        self.stub = TupleSpacesStub(self.channel)
        self.client_id = client_id

    def put(self, tuple_data: str, delays: list[int]) -> str:
        request = PutRequest(newTuple=tuple_data)
        metadata = self._build_metadata(delays)
        try:
            self.stub.put(request, metadata=metadata)
            return "OK"
        except grpc.RpcError as e:
            return f"Error: {e.details()}"

    def read(self, tuple_data: str, delays: list[int]) -> str:
        request = ReadRequest(searchPattern=tuple_data)
        metadata = self._build_metadata(delays)
        try:
            response = self.stub.read(request, metadata=metadata)
            return f"OK\n{response.result}"
        except grpc.RpcError as e:
            return f"Error: {e.details()}"

    def take(self, tuple_data: str, delays: list[int]) -> str:
        request = TakeRequest(searchPattern=tuple_data)
        metadata = self._build_metadata(delays)
        try:
            response = self.stub.take(request, metadata=metadata)
            return f"OK\n{response.result}"
        except grpc.RpcError as e:
            return f"Error: {e.details()}"

    def get_tuple_spaces_state(self):
        request = GetTupleSpacesStateRequest()
        try:
            response = self.stub.getTupleSpacesState(request)
            return "OK\n" + "\n".join(response.tuple)
        except grpc.RpcError as e:
            return f"Error: {e.details()}"

    def _build_metadata(self, delays: list[int]):
        keys = ["delay-replica-0", "delay-replica-1", "delay-replica-2"]
        metadata = [(key, str(val)) for key, val in zip(keys, delays)]
        metadata.append(("client-id", str(self.client_id)))
        return metadata
