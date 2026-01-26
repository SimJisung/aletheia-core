# CLAUDE.md - AI Assistant Guidelines for aletheia-core

This document provides guidance for AI assistants working on the **aletheia-core** repository, part of the Mnemosyne-Protocol project.

## Project Overview

**aletheia-core** is the core component of the Mnemosyne-Protocol project. The name "Aletheia" (Greek: ἀλήθεια) means "truth" or "disclosure" - suggesting this project deals with truth, verification, or authentic data handling.

### Repository Status

This repository is in early development. The project structure is being established.

## Codebase Structure

```
aletheia-core/
├── CLAUDE.md          # AI assistant guidelines (this file)
├── README.md          # Project overview
└── [TBD]              # Project structure to be established
```

### Planned Directory Conventions

When building out this project, follow these conventions:

| Directory | Purpose |
|-----------|---------|
| `src/` | Main source code |
| `tests/` or `__tests__/` | Test files |
| `docs/` | Documentation |
| `scripts/` | Build and utility scripts |
| `config/` | Configuration files |

## Development Workflow

### Git Workflow

1. **Branch Naming Convention**:
   - Feature branches: `feature/<description>`
   - Bug fixes: `fix/<description>`
   - AI-assisted work: `claude/<description>-<session-id>`

2. **Commit Messages**:
   - Use clear, descriptive commit messages
   - Start with a verb in imperative mood: "Add", "Fix", "Update", "Remove"
   - Keep the first line under 72 characters
   - Reference issues when applicable

3. **Pull Requests**:
   - Provide clear descriptions of changes
   - Link related issues
   - Ensure all tests pass before merging

### Code Review Guidelines

- Review for correctness, readability, and maintainability
- Ensure code follows established patterns in the codebase
- Check for security vulnerabilities
- Verify adequate test coverage for new code

## Key Conventions

### Code Style

When establishing the codebase, follow these principles:

1. **Consistency**: Match existing patterns in the codebase
2. **Clarity**: Write self-documenting code with clear naming
3. **Simplicity**: Prefer simple solutions over complex ones
4. **Documentation**: Document complex logic and public APIs

### Security Considerations

- Never commit secrets, API keys, or credentials
- Use environment variables for sensitive configuration
- Validate and sanitize all external inputs
- Follow OWASP security guidelines

### Testing Requirements

- Write tests for new functionality
- Maintain existing test coverage
- Include both unit tests and integration tests where appropriate
- Test edge cases and error conditions

## For AI Assistants

### Before Making Changes

1. **Read before writing**: Always read existing files before modifying them
2. **Understand context**: Explore related files to understand patterns
3. **Check dependencies**: Understand how changes affect other parts of the codebase

### When Implementing Features

1. **Start simple**: Begin with minimal viable implementation
2. **Avoid over-engineering**: Only add complexity when necessary
3. **Follow existing patterns**: Match the style of surrounding code
4. **Test your changes**: Ensure changes work as expected

### When Debugging

1. **Reproduce the issue**: Understand the problem before fixing
2. **Check logs and error messages**: They often point to the root cause
3. **Make minimal changes**: Fix only what's broken
4. **Add regression tests**: Prevent the bug from recurring

### Things to Avoid

- Don't add unnecessary features or "improvements"
- Don't refactor code that isn't related to the current task
- Don't add comments to code you didn't change
- Don't create documentation files unless specifically requested
- Don't guess - investigate to find the truth

### Git Operations

- Always verify the current branch before committing
- Use specific file additions rather than `git add -A`
- Never force push to shared branches without explicit permission
- Create new commits rather than amending previous work

## Project-Specific Notes

### Mnemosyne-Protocol Context

The project name references Mnemosyne, the Greek goddess of memory. Combined with "Aletheia" (truth), this suggests the project may involve:
- Memory/data persistence
- Truth verification or validation
- Authentic record-keeping

*This section should be updated as the project's specific purpose becomes clearer.*

## Quick Reference

### Common Commands

```bash
# Git operations
git status                    # Check current state
git diff                      # View unstaged changes
git log --oneline -10         # View recent commits

# Development commands (to be established)
# npm install / pip install / cargo build  # Install dependencies
# npm test / pytest / cargo test           # Run tests
# npm run build / make build               # Build project
```

### Important Files

| File | Description |
|------|-------------|
| `README.md` | Project introduction and setup instructions |
| `CLAUDE.md` | AI assistant guidelines (this file) |
| `[package.json/Cargo.toml/etc.]` | Dependencies and project configuration |
| `[.gitignore]` | Files excluded from version control |

---

*This document should be updated as the project evolves and new conventions are established.*
