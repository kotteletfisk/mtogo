# How to Contribute

This is a guideline for developers working on the MTOGO project. The project is developed with a CI approach, meaning contributers should aim to integrate their changes into the trunk branch (main) as often as possible.

## Branching rules

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

#### Pull Requests

Branch protection is enabled on the main branch to enfore integration by pull requests. This is to make sure that all relevant test cycles are run successfully before merging.

1. Commit your changes
3. Merge main remote into your local branch
2. Push to remote branch
3. Open PR on main
4. After successful test run add describing commit message in line with template
5. Merge PR and delete branch

## Code style rules

The rules are based on the Google Java Style Guide:
https://google.github.io/styleguide/javaguide.html

Style checkers are expected to enforce this.

## Testing

All code written is required live up to the [test plan](docs/QA/test_plan.pdf).
Test are expected to be written by developers alongside developed code, for example by [TDD](https://agilealliance.org/glossary/tdd/).

## Logging

All stdout and stderr are implemented via logging frameworks, where logging levels can be controlled at runtime of the service.

A service should default to INFO level, but should be controllable by environment variables. For example:

`LOG_LEVEL=debug java -jar app.jar`

In java services, we use frameworks that are compatible with the SLF4J api. For example `logback` or `log4j2`

## Adding Services

Adding a service (a standalone buildable image) requires some careful steps:

1. Add a directory in the services/ directory with the name of the new service.
2. Set up the new service as a project in the new directory. An `example-service` Maven project:
   - `mvn archetype:generate -DgroupId=com.mtogo.example -DartifactId=example-service -DjavaCompilerVersion=21 -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.5`
3. Add a Dockerfile for building the service in the root of the new service directory
4. Add the directory path to the CI pipeline:
   - under the `detect-changes` job in `.github/workflows/ci.yml`,
     add your new service as a paths-filter argument.
     Example:

```yaml
uses: dorny/paths-filter@v3
with:
  list-files: "json"
  filters: |
    example-service:
      - 'services/example-service/**'
```

5. If your service introduces a new programming language in the repo, you have to extend the CI pipeline to support it:

   - in the `test-and-build` job, add a detection mechanism as a conditional branch (use `elif` after java). Set the `lang` variable accordingly:

   ```yaml
         - name: Detect Service Language
       id: detect
       run: |
         svc_dir="services/${{ matrix.service }}"
         if [ -f "$svc_dir/pom.xml" ]; then
           lang="java"
         else
           echo "No supported language detected in $svc_dir"
           exit 1
         fi
         echo "language=$lang" >> $GITHUB_OUTPUT
   ```

   - Add steps matching the `lang` output, that builds correct environment and runs the test suites correctly:

   ```yaml
   # Java / Maven
   - name: Set up JDK
     if: ${{ steps.detect.outputs.language == 'java' }}
     uses: actions/setup-java@v4
     with:
       java-version: "21"
       distribution: "temurin"
       cache: maven

   - name: Test with Maven
     if: ${{ steps.detect.outputs.language == 'java' }}
     run: mvn --batch-mode --update-snapshots verify
     working-directory: services/${{ matrix.service }}
   ```

## Expected workflow

1. Pick an issue from the issue tracker.
2. Create a new branch from the main branch, following the naming conventions.
3. Make changes and commit them frequently, following the commit message conventions.
4. Merge changes from main into your working branch.
5. Run tests locally using the dev container config in the repo. Make sure they pass.
6. Push the branch to the remote repository.
7. Open a pull request on main, if any changes are found on services, relevant tests will run. If they pass, merge the changes. Else it's back to the drawing board!
8. Once the issue is resolved, delete working branch.

## Local Deployment

If it is needed to run a local deployment of the stack for i. e service testing in the contexxt of other services, you can use the included devcontainer
configuration that simulates the current deployment environment:

1. Make sure Docker is installed and running on your local machine
2. Download the .env file from the test/prod server (according to need). Set needed variables fx `LOG_LEVEL=debug`
3. Use a devcontainer tool in your IDE. vscode example: `ms-vscode-remote.remote-containers`
4. Open devcontainer in project root. Post start script will generate necessary service keys
5. Copy stack file. fx `docker-stack-test.yml`, and change desired services to images you want to test
6. Deploy stack
7. PROFIT $$$
