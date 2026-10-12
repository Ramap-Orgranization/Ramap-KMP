import shutil
import subprocess
import unittest
from pathlib import Path


class ToolsBridgeTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which('node'), 'Node.js is needed only for the MCP adapter tests')
    def test_mcp_adapter_queue_recovery_target_and_shell_data_boundaries(self):
        result = subprocess.run(['node', str(Path(__file__).with_name('test_run_with_tools.js'))],
            capture_output=True, text=True, timeout=50)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
