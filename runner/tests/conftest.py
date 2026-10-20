import pathlib
import sys

# Cho test import được `viz` và `run` như khi chạy trong container (/runner trên sys.path).
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))
