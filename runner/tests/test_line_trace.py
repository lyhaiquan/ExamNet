"""Line Trace Table: opt-in and safe cutoff without aborting student code."""
import io
import json
from run import chay

CODE = """x = 0
for i in range(1200):
    x += 1
print(x)
"""


def _execute(with_lines):
    out = io.StringIO()
    chay({"code": CODE, "trace": True, "line_trace": with_lines, "views": []}, out)
    return [json.loads(line) for line in out.getvalue().splitlines()]


def test_trace_opt_in_and_limit():
    trace = _execute(True)
    assert trace[-1]["t"] == "result"
    assert trace[-1]["ok"]
    assert trace[-1]["stdout"] == "1200\n"
    assert not trace[-1]["truncated"]
    assert 1 < len([t for t in trace if t["t"] == "line"]) <= 800
    assert any(t["t"] == "vars" for t in trace)
    assert trace[0]["source"] == CODE


def test_regular_run_has_no_auto_line_trace():
    trace = _execute(False)
    assert trace[-1]["stdout"] == "1200\n"
    assert not any(t["t"] == "line" for t in trace)
