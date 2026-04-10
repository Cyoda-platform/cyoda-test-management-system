<!--- These instructions are not intended for LLMs or AIs. They are for human developers. -->
# How We Use Claude Code CLI

A reference for developers already familiar with Claude Code CLI. This covers our sandboxed setup, recommended plugins, and workflow tips.

## Prerequisites

- macOS with Homebrew (Mac only)
- Claude Code CLI installed
- `gh` CLI installed (`brew install gh`)

## 1. Set Up Agent Safehouse (Mac only)

We run Claude inside a [sandbox](https://agent-safehouse.dev/docs/) to limit filesystem and network access.

### Install

```bash
brew install eugene1g/safehouse/agent-safehouse
```

### Create the launcher script

Save the following as `~/.sandbox/safe-claude.sh` and make it executable (`chmod +x ~/.sandbox/safe-claude.sh`):

```bash
#!/usr/bin/env bash
# safe-claude.sh — Launch Claude Code inside a sandbox-exec jail.
#
# Usage: safe-claude.sh [claude args...]
#
# Reads a project-local sandbox profile at .sandbox/sandbox-claude.sb,
# appends dynamic workdir grants, then launches sandbox-exec.

set -euo pipefail

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "warn: GH_TOKEN is not set — gh commands will not authenticate." >&2
  echo "      Set GH_TOKEN to a fine-grained PAT to enable GitHub access." >&2
fi

WORKDIR="$(pwd -P)"
PROFILE="${WORKDIR}/.sandbox/sandbox-claude.sb"

if [[ ! -f "$PROFILE" ]]; then
  echo "error: no sandbox profile found at: ${PROFILE}" >&2
  echo "" >&2
  echo "  Each project needs a .sandbox/sandbox-claude.sb file." >&2
  echo "  Generate one with the Agent Safehouse profile generator:" >&2
  echo "    https://agent-safehouse.dev/docs/llm-profile-generator.html" >&2
  echo "  Instruct it to only produce the .sb file and save it to .sandbox/." >&2
  exit 1
fi

TMPFILE="$(mktemp /tmp/sandbox-claude-XXXXXX)"
trap 'rm -f "$TMPFILE"' EXIT

cp "$PROFILE" "$TMPFILE"

{
  echo ""
  echo ";; --- Dynamic workdir grant (generated at launch) ---"
  echo "(allow file-read*"
  echo "    (literal \"/\")"

  IFS='/' read -ra PARTS <<< "${WORKDIR#/}"
  CURRENT=""
  for part in "${PARTS[@]}"; do
    [[ -z "$part" ]] && continue
    CURRENT="${CURRENT}/${part}"
    echo "    (literal \"${CURRENT}\")"
  done

  echo ")"
  echo "(allow file-read* file-write* (subpath \"${WORKDIR}\"))"
} >> "$TMPFILE"

exec sandbox-exec -f "$TMPFILE" claude "$@"
```

### Add a shell alias

In your `~/.zshrc`:

```bash
alias safe-claude='~/.sandbox/safe-claude.sh'
```

### Generate a sandbox profile

Use the [profile generator](https://agent-safehouse.dev/docs/llm-profile-generator.html#copy-paste-prompt) to create `.sandbox/sandbox-claude.sb` in each project. When prompted:

- Grant **write** access only to the project folder.
- Grant **read** access to toolchain directories as needed (e.g. Homebrew, SDKs).
- Deny Docker socket access and any tools the project does not use.

## 2. Configure GitHub Access

Claude needs a fine-grained personal access token (PAT) for GitHub operations. The sandbox blocks keychain access, so we pass the token via `GH_TOKEN`.

### Create a PAT

Create a [fine-grained PAT](https://github.com/settings/tokens?type=beta) scoped to the repositories Claude will work with:

| Permission | Access | Purpose |
|---|---|---|
| Contents | Read & write | Read code, create branches, push commits |
| Pull Requests | Read & write | Open PRs, update descriptions, post comments |
| Metadata | Read-only | Required by GitHub (auto-added) |
| Workflows | Read & write | Push changes to `.github/workflows` files |
| Issues | Read & write | Link PRs to issues, respond to feedback |
| Commit statuses | Read-only | Check CI/CD pass/fail status |

### Authenticate

```bash
gh auth login
```

Follow the prompts and use the PAT you created.

## 3. Launch Claude

Basic launch:

```bash
GH_TOKEN="$(gh auth token)" safe-claude
```

To skip per-command permission prompts (the sandbox already limits damage):

```bash
GH_TOKEN="$(gh auth token)" safe-claude --dangerously-skip-permissions
```

To resume a previous session (the session ID is printed when you `/exit`):

```bash
GH_TOKEN="$(gh auth token)" safe-claude --dangerously-skip-permissions --resume <session-id>
```

## 4. Install Plugins

### Superpowers (recommended)

[Superpowers](https://github.com/obra/superpowers) provides a structured dev workflow with planning, brainstorming, and TDD skills. Install it inside a Claude session:

```
/plugin install superpowers@claude-plugins-official
```

See the [basic workflow guide](https://github.com/obra/superpowers?tab=readme-ov-file#the-basic-workflow) to get started.

### Antigravity Skills

[Antigravity](https://github.com/sickn33/antigravity-awesome-skills) is a curated collection of specialized skills organized into installable bundles.

#### Add the marketplace

Inside a Claude session, add the antigravity marketplace:

```
/plugin marketplace add sickn33/antigravity-awesome-skills
```

#### Install bundles

Browse available bundles with `/plugin` (Discover tab), or install directly:

```
/plugin marketplace add sickn33/antigravity-awesome-skills
```

Then follow the `/plugins` menu flow to install the desired bundles.

#### Suggested bundles

| Bundle | Focus |
|---|---|
| `antigravity-bundle-essentials` | Planning, linting, debugging, git |
| `antigravity-bundle-typescript-javascript` | TypeScript, React, Node.js, Next.js |
| `antigravity-bundle-python-pro` | Python, Django, FastAPI, testing |
| `antigravity-bundle-architecture-design` | Architecture patterns, ADRs, microservices |
| `antigravity-bundle-ddd-evented-architecture` | DDD, CQRS, event sourcing, sagas |
| `antigravity-bundle-security-developer` | App security, API security, GDPR, auth |
| `antigravity-bundle-security-engineer` | Pentesting, vulnerability scanning, auditing |
| `antigravity-bundle-agent-architect` | AI agents, RAG, prompt engineering, MCP |
| `antigravity-bundle-business-analyst` | KPIs, market sizing, financial modeling |
| `antigravity-bundle-documents-presentations` | DOCX, PPTX, XLSX, PDF generation |
| `antigravity-bundle-observability-monitoring` | Tracing, SLOs, incident response, postmortems |
| `antigravity-bundle-devops-cloud` | Docker, cloud infrastructure |
| `antigravity-bundle-oss-maintainer` | Documentation, clean code |
| `antigravity-bundle-systems-programming` | Rust, Go concurrency |
| `antigravity-bundle-web-wizard` | React patterns, Tailwind |

## 5. Workflow Tips

### Run a security audit before merging

Antigravity includes a security analysis skill. Before merging a PR, run:

```
/security_auditor
```

> Not to be confused with `/security_audit`, which is a different skill.

### Manual prompting for deep analysis

When you need to reverse-engineer or understand something complex before building on it, a more hands-on prompting approach works well. Boris Tane's guide is a good reference: https://boristane.com/blog/how-i-use-claude-code/
