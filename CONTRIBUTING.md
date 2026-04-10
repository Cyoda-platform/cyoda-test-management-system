# Contributing to Cyoda Test Management System

This guide describes how we work on the TMS. Development is often agent-assisted using Claude Code, but the degree of agent involvement is up to each developer. The more agentic the engineering, the greater the responsibility for supervision, preparation, and quality assurance. We suggest the workflow below, but don't prescribe it.

For coding rules and architecture, see [CLAUDE.md](CLAUDE.md). For Claude Code environment setup, see [CODING_WITH_CLAUDE.md](CODING_WITH_CLAUDE.md).

## Prerequisites

- Environment setup: [CODING_WITH_CLAUDE.md](CODING_WITH_CLAUDE.md)
- Coding rules and architecture: [CLAUDE.md](CLAUDE.md)
- Build commands: [CLAUDE.md](CLAUDE.md)

## Development Workflow

### 1. Preparation

Define the scope of the change before touching code. The better the preparation, the better the results. Consider producing a scope description, a high-level implementation sequence, or a structured backlog. The format is up to you — what matters is that the intent and boundaries of the change are clear before work begins.

### 2. Brainstorming

Use `/brainstorming` to turn the prepared scope into a validated design. This step forces explicit discussion of trade-offs, constraints, and non-goals before any implementation starts.

### 3. Planning

Use the superpowers planning flow to create an implementation plan from the validated design. The plan breaks the work into incremental steps and identifies risks early.

### 4. Implementation

Execute the plan. The superpowers workflow supports TDD, git worktrees for isolation, and incremental delivery. Work through the plan step by step, verifying each step before moving on.

### 5. Code Review

Use the superpowers code review skill to review the implementation against the plan. This checks that the code matches the design intent, follows project conventions, and maintains a green build.

### 6. Security Audit

After code review passes, run `/security-auditor` to scan the changes for vulnerabilities. This is an additional gate beyond the standard superpowers flow.

### 7. Pull Request

Create a PR against `main`. A human review is required before merging.

## Developer Responsibility

The developer supervises the agent's work and is accountable for the quality of the result. This includes non-functional aspects: performance, security, and testing. Agents will often cut corners or miss things. The human is the quality gate.

## Branch Strategy

- Branch from `main`
- PR back to `main`
