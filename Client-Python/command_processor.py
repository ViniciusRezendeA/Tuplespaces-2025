from typing import List

class CommandProcessor:
    SPACE = " "
    BGN_TUPLE = "<"
    END_TUPLE = ">"
    PUT = "put"
    READ = "read"
    TAKE = "take"
    EXIT = "exit"
    GET_TUPLE_SPACES_STATE = "getTupleSpacesState"
    SLEEP = "sleep"

    def __init__(self, client_service):
        self.client_service = client_service

    def parse_input(self):
        exit_flag = False
        while not exit_flag:
            try:
                line = input("> ").strip()
                split = line.split()
                if not split:
                    continue

                command = split[0]

                if command == self.PUT:
                    print(self.put(split))
                elif command == self.READ:
                    print(self.read(split))
                elif command == self.TAKE:
                    print(self.take(split))
                elif command == self.GET_TUPLE_SPACES_STATE:
                    print(self.get_tuple_spaces_state())
                elif command == self.SLEEP:
                    self.sleep(split)
                elif command == self.EXIT:
                    exit_flag = True
                else:
                    self.print_usage()
            except EOFError:
                break

    def put(self, split: List[str]):
        if not self.input_is_valid(split):
            return self.print_usage()
        tuple_data = split[1]
        delays = self.extract_delays(split)
        return self.client_service.put(tuple_data, delays)

    def read(self, split: List[str]):
        if not self.input_is_valid(split):
            return self.print_usage()
        tuple_data = split[1]
        delays = self.extract_delays(split)
        return self.client_service.read(tuple_data, delays)

    def take(self, split: List[str]):
        if not self.input_is_valid(split):
            return self.print_usage()
        tuple_data = split[1]
        delays = self.extract_delays(split)
        return self.client_service.take(tuple_data, delays)

    def get_tuple_spaces_state(self):
        return self.client_service.get_tuple_spaces_state()

    def sleep(self, split: List[str]):
        if len(split) != 2:
            return self.print_usage()
        try:
            import time
            time.sleep(int(split[1]))
        except ValueError:
            return self.print_usage()

    def extract_delays(self, split: List[str]) -> List[int]:
        try:
            return [int(split[i]) if i < len(split) else 0 for i in range(2, 5)]
        except ValueError:
            return [0, 0, 0]

    def print_usage(self):
        print("Usage:\n"
              "- put <tuple> [delay0 delay1 delay2]\n"
              "- read <tuple> [delay0 delay1 delay2]\n"
              "- take <tuple> [delay0 delay1 delay2]\n"
              "- getTupleSpacesState\n"
              "- sleep <seconds>\n"
              "- exit\n")

    def input_is_valid(self, split: List[str]) -> bool:
        if len(split) < 2 or len(split) > 5:
            return False

        # Verifica se o tuplo começa com < e termina com >
        if not split[1].startswith(self.BGN_TUPLE) or not split[1].endswith(self.END_TUPLE):
            return False

        # Verifica se todos os delays (se existirem) são inteiros >= 0
        for delay_arg in split[2:]:
            try:
                value = int(delay_arg)
                if value < 0:
                    return False
            except ValueError:
                return False

        return True


