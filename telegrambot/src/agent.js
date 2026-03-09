const { spawn } = require("child_process");
const { stripAnsi } = require("./formatter");

/**
 * Spawns the pi coding agent in print mode and returns its output.
 * @param {string} prompt - The prompt to send to the agent
 * @param {Object} [options]
 * @param {string} [options.command="pi"] - CLI command to invoke
 * @param {string} [options.workingDir] - Working directory for the agent
 * @param {number} [options.timeout=300] - Timeout in seconds
 * @param {string} [options.model] - Model override (e.g. "anthropic/claude-sonnet-4-20250514")
 * @returns {Promise<string>} The agent's text response
 */
function executePrompt(prompt, options = {}) {
  const cmd = options.command || "pi";
  const timeout = (options.timeout || 300) * 1000;
  const cwd = options.workingDir || process.cwd();

  const args = [];
  if (options.model) {
    args.push("--model", options.model);
  }
  args.push("-p", "--no-session", prompt);

  return new Promise((resolve, reject) => {
    let stdout = "";
    let stderr = "";
    let killed = false;

    const child = spawn(cmd, args, {
      cwd,
      env: { ...process.env },
      stdio: ["ignore", "pipe", "pipe"],
    });

    const timer = setTimeout(() => {
      killed = true;
      child.kill("SIGTERM");
    }, timeout);

    child.stdout.on("data", (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString();
    });

    child.on("error", (err) => {
      clearTimeout(timer);
      reject(new Error(`Failed to start agent: ${err.message}`));
    });

    child.on("close", (code) => {
      clearTimeout(timer);

      if (killed) {
        const partial = stripAnsi(stdout).trim();
        resolve(
          partial
            ? `[Timed out after ${options.timeout || 300}s]\n\n${partial}`
            : `Agent timed out after ${options.timeout || 300} seconds.`,
        );
        return;
      }

      const output = stripAnsi(stdout).trim();

      if (code === 0) {
        resolve(output || "(no output)");
      } else {
        const errMsg = stripAnsi(stderr).trim();
        reject(new Error(errMsg || `Agent exited with code ${code}`));
      }
    });
  });
}

module.exports = { executePrompt };
