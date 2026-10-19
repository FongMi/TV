import ast

from yapf.yapflib.yapf_api import FormatCode


def format_source(source):
    ast.parse(source, filename="spider.py")
    return FormatCode(source, filename="spider.py", style_config={
        "based_on_style": "pep8",
        "column_limit": 88,
    })[0]
