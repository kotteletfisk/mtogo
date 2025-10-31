# How to contribute to this project

## Getting started

Clone the project repository:

```bash
git clone https://github.com/kotteletfisk/ss_ola2.git
```

or with SSH:

```bash
git clone git@github.com:kotteletfisk/ss_ola2.git
```

### Using Dev Containers

Make sure to have docker installed on your machine, to be able to run the devcontainer.

The dev container specifies a common environment for development and testing. This includes OS, Java environment, Build manager (Maven) and version control. It excludes stuff like IDE and personal dev preferences.

We use a simple container running a Debian Linux distribution with Java 21, Maven and Git.

To run the devcontainer, we need middlware to parse the configuration (devcontainer.json), build a docker image, and run it as our env.

Depending on your IDE, there are different ways to run and use the devcontainer.

#### IntelliJ IDEA
Use the following guide for starting devcontainers within IntelliJ: 

https://www.jetbrains.com/help/idea/start-dev-container-inside-ide.html

Keep in mind that you need to have IntelliJ Ultimate edition to be able to use this feature.

#### VS Code

- Install ´Dev Containers´ plugin

- Use command palette -> Build Dev Container.

- SSH credentials gets injected into the container, so we can push commits normally.
    - On Linux, this works with `ssh-agent`. If we run into problems with invalid SSH keys, we might have to add out local keys to the agent by runnin `ssh-add` (Adds keys from default .ssh directory)

## Branching rules

The branching strategy can be found in the [README.md](./README.md) file.

As for naming branches, we will use the following conventions:
- All work will be done in branches off the main branch.
- Branches will be based on issues.
- Branch names will be in the format: `issue-<issue-number>-<short-description>`.
- Example: `issue-42-add-login-feature`.
- Branch names should be all lowercase, with words separated by hyphens.
- Branches should be deleted after they have been merged into the main branch or when the referenced issue has been resolved.
- Avoid long-lived branches to minimize merge conflicts and ensure frequent integration with the main branch.

## Commit message conventions
- Start of each commit message with a short summary.
- Leave a blank line after the summary.
- Fill the commit body with a more detailed description of changes.
- Use a footer to reference related issues, using keywords and issue numbers. (e.g. "fixes #123", "closes #456", "resolves #789")

#### Example Commit Message

```
Add user authentication feature

Implemented user login and registration functionality using JWT for secure authentication.

fixes #42
```

Keep in mind that pull requests has been omitted from this project, as they do not fit well within the trunk based strategy, and will not be used.

## Code style rules
The code style rules are defined in [checkstyle.xml](./checkstyle.xml) as Checkstyle is being used as linter for the Java code.

The rules are based on the Google Java Style Guide, with some modifications to fit our preferences.

## Expected workflow
1. Pick an issue from the issue tracker.
2. Create a new branch from the main branch, following the naming conventions.
3. Make changes and commit them frequently, following the commit message conventions.
4. Push the branch to the remote repository.
5. Once the issue is resolved, merge the branch back into the main branch.
6. Delete the branch after merging.