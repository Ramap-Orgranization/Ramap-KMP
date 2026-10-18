const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const vm = require('node:vm');
const { execFileSync, spawnSync } = require('node:child_process');

const scripts = __dirname;
const driver = vm.runInNewContext(fs.readFileSync(path.join(scripts, 'run_with_tools.js'), 'utf8'));

async function scenario({ loseAck = false, wrongProject = false, truncated = false, replay = false } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'menu-refresh-tools-'));
  try {
    const runDir = path.join(root, "run ' $(touch HACKED_MENU_TEST)");
    const setup = `
import sys
from datetime import datetime, timezone
from pathlib import Path
sys.path.insert(0, sys.argv[1])
from refresh_run import initialize, save, submit
from test_refresh_run import raw_for, state_fixture
from refresh_verify import VISIBLE_FIELDS
capture, state = state_fixture()
capture['captured_at'] = datetime.now(timezone.utc).isoformat()
description = "육수 ' $(touch HACKED_MENU_TEST)\\nMENU_REFRESH_RESPONSE\\n\u0060touch HACKED_MENU_TEST\u0060"
capture['menu_items'][0]['description'] = description
state['items'][0]['description'] = description
state['visible'] = [{k:r.get(k) for k in VISIBLE_FIELDS} for r in state['items']]
root, run = Path(sys.argv[2]), Path(sys.argv[3])
initialize(run, state['shops'], 'offline', mode=sys.argv[4], rehearsal='deletions')
source = root / 'input'
source.mkdir()
capture['raw_html_path'] = str(source / 'raw-menu.html')
(source / 'raw-menu.html').write_text(raw_for(capture))
save(source / 'result.json', capture)
save(root / 'state.json', state)
submit(run, source / 'result.json', '/collector')
`;
    execFileSync('python3', ['-c', setup, scripts, root, runDir, replay ? 'replay' : 'live'], { cwd: root });
    const state = JSON.parse(fs.readFileSync(path.join(root, 'state.json'), 'utf8'));
    const snapshots = new Map();
    let commits = 0;
    let calls = 0;
    let lost = false;
    const api = {
      execCommand: async ({ cmd }) => {
        if (truncated && cmd.startsWith('python3 -c ')) return { exit_code: 0, output: '{"sql":"broken' };
        const result = spawnSync('/bin/zsh', ['-c', cmd], { cwd: root, encoding: 'utf8' });
        return { exit_code: result.status, output: result.stdout };
      },
      executeSql: async ({ project_id, query }) => {
        assert.equal(project_id, 'offline');
        assert.ok(query.length);
        calls++;
        const manifest = JSON.parse(fs.readFileSync(path.join(runDir, 'manifest.json'), 'utf8'));
        const action = manifest.inflight;
        let rows;
        if (action.kind === 'audit_start') rows = [{ id: manifest.run_id, status: 'running' }];
        else if (action.kind === 'prewrite') rows = [{ state }];
        else {
          const shop = manifest.shops[action.shop_ids[0]];
          const plan = JSON.parse(fs.readFileSync(path.join(runDir, shop.directory, 'prepared/plan.json'), 'utf8'));
          const snapshot = Object.fromEntries(['shop_id', 'captured_at', 'source_url', 'parser_version', 'source_hash', 'menu_fingerprint'].map(k => [k, plan[k]]));
          Object.assign(snapshot, { id: plan.snapshot_id, raw_hash: plan.source_hash, menu_items: plan.source_items });
          if (action.kind === 'commit') {
            commits++;
            snapshots.set(snapshot.id, snapshot);
            Object.assign(state, { items: plan.expected, visible: plan.expected_visible, overrides: plan.expected_overrides });
            rows = [snapshot];
            if (loseAck && !lost) {
              lost = true;
              throw new Error('acknowledgement lost after commit');
            }
          } else if (action.kind === 'verify' || action.kind === 'recover') {
            rows = [{ verification: { state, snapshots: snapshots.has(plan.snapshot_id) ? [snapshots.get(plan.snapshot_id)] : [] } }];
          } else throw new Error('unexpected action: ' + action.kind);
        }
        return { content: [{ type: 'text', text: JSON.stringify({ result: '<untrusted-data-test>\n' + JSON.stringify(rows) + '\n</untrusted-data-test>' }) }], isError: false };
      }
    };
    const options = { script: path.join(scripts, 'refresh_run.py'), runDir,
      projectId: wrongProject ? 'another-project' : 'offline', maxSeconds: 50 };
    if (wrongProject || truncated || replay) {
      await assert.rejects(() => driver(api, options));
      assert.equal(calls, 0);
    } else {
      const result = await driver(api, options);
      assert.equal(result.status, 'awaiting_independent_review');
      assert.equal(result.verified_shops, 1);
      assert.equal(commits, 1);
      const previousCalls = calls;
      const again = await driver(api, options);
      assert.equal(again.executed, 0);
      assert.equal(calls, previousCalls);
    }
    assert.equal(fs.existsSync(path.join(root, 'HACKED_MENU_TEST')), false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

(async () => {
  await scenario();
  await scenario({ loseAck: true });
  await scenario({ wrongProject: true });
  await scenario({ truncated: true });
  await scenario({ replay: true });
  process.stdout.write('5 MCP bridge scenarios passed\n');
})().catch(error => { process.stderr.write(error.stack + '\n'); process.exitCode = 1; });
