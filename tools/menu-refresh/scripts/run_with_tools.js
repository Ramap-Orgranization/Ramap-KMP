(async function runMenuRefresh(api, options) {
  // Loaded as local code in functions.exec. SQL/results stay inside the tool calls;
  // only compact progress is returned to the conversation.
  const quote = value => "'" + String(value).replace(/'/g, "'\\''") + "'";
  const script = options.script;
  const runDir = options.runDir;
  const projectId = options.projectId;
  if (!script || !runDir || !projectId) throw new Error('script, runDir and projectId are required');
  const base = `python3 ${quote(script)}`;
  const runArg = ` --run ${quote(runDir)}`;
  const maxActions = options.maxActions ?? 40;
  const deadline = Date.now() + (options.maxSeconds ?? 45) * 1000;
  const command = async (arguments_, stdin) => {
    let cmd = base + ' ' + arguments_ + runArg;
    if (stdin !== undefined) {
      // JSON.stringify escapes embedded newlines; the quoted delimiter cannot
      // appear as a line in the one-line JSON payload. No shell interpolation.
      cmd += " <<'MENU_REFRESH_RESPONSE'\n" + JSON.stringify(stdin) + '\nMENU_REFRESH_RESPONSE';
    }
    const result = await api.execCommand({ cmd, max_output_tokens: 5000 });
    if (result.session_id || result.exit_code !== 0) throw new Error('runner command did not finish successfully');
    return JSON.parse(result.output);
  };
  if (options.resume) await command('recover');
  let last;
  let executed = 0;
  while (executed < maxActions && Date.now() < deadline) {
    const action = await command('next');
    if (action.kind === 'idle' || action.kind === 'await_result') return { ...action, executed };
    if (action.mode !== 'live' || action.project_id !== projectId) {
      throw new Error('database transport target differs from the live run');
    }
    const read = await api.execCommand({
      cmd: `python3 -c 'import hashlib,json,pathlib,sys;p=pathlib.Path(sys.argv[1]);s=p.read_text();assert hashlib.sha256(s.encode()).hexdigest()==sys.argv[2];print(json.dumps({"sql":s}))' ${quote(action.sql_path)} ${quote(action.sql_hash)}`,
      max_output_tokens: 100000
    });
    if (read.session_id || read.exit_code !== 0 || read.original_token_count > 100000 || /Warning: truncated output|tokens truncated/.test(read.output)) {
      throw new Error('SQL could not be read in full; recover the action and use CLI transport');
    }
    const sql = JSON.parse(read.output).sql;
    let response;
    try {
      response = await api.executeSql({ project_id: projectId, query: sql });
    } catch (error) {
      last = await command(`accept --action-id ${quote(action.id)} --failed`);
      executed++;
      continue;
    }
    last = await command(`accept --action-id ${quote(action.id)}`, response);
    executed++;
  }
  return { kind: 'yielded', ...last, executed };
})
