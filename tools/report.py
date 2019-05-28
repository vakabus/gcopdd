import os
import os.path
from typing import List, Set, Generator


class CompilationUnit(object):
    def __init__(self, directory_name: str, data_suffix: str):
        self.directory_name = directory_name
        self.data_suffix = data_suffix
    
    def get_id(self) -> str:
        with open(os.path.join(self.directory_name, f"compilationRequest.{self.data_suffix}"), "r") as f:
            return f.read()
    
    def get_collected_dump_types(self) -> List[str]:
        # FIXME cache directory list access??
        files = os.listdir(self.directory_name)
        return [n[:n.index(".")] for n in files if n.endswith(self.data_suffix)]
    
    def get_dump_data_file(self, dump_type: str):
        return open(os.path.join(self.directory_name, dump_type + "." + self.data_suffix), "r")
    
    def __repr__(self):
        return f"CompilationUnit[{self.data_suffix}]"


class Report(object):
    def __init__(self, directory_name: str):
        self.directory_name = directory_name
    
    def get_compilation_units(self) -> Set[CompilationUnit]:
        files = os.listdir(self.directory_name)
        suffixes = { p[p.index(".")+1:] for p in files }
        return { CompilationUnit(self.directory_name, s) for s in suffixes }
    
    def get_data_files_iter(self, dump_type: str):
        for cu in self.get_compilation_units():
            try:
                with cu.get_dump_data_file(dump_type) as f:
                    yield f
            except FileNotFoundError:
                pass
    
    def get_aggregated_node_types(self) -> Set[str]:
        types = set()
        for f in self.get_data_files_iter("nodelist"):
            for typ in f:
                types.add(typ.strip())
        
        types.remove('')
        return types


