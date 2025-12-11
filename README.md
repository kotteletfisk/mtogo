# MToGo - exam project

### Made By

- Lasse Hansen - cph-lh479@stud.ek.dk
- Pelle Hald Vedsmand - cph-pv73@stud.ek.dk
- Nicolai Rosendahl - cph-nr135@stud.ek.dk

---

### About the Project

This is the repo for the 2. sem exam project by group A autumn 2025.

The project goal is an online food delivery software system that offers customers possibilities for ordering takeout from partnered suppliers and delivery of orders by employed agents.

Its implemented as a Service Oriented Architecture deployed with Docker Swarm and is currently running on a single Swarm node in a test deployment environment. It runs under the domain https://mtogo.dk, but is currently unavailable due to early development security reasons.

Development is done in a kanban inspired trunk-based workflow, and is tracked with issues in the associated GitHub projects board.

### Project Structure

[docker-stack.yml](docker-stack.yml)
The docker swarm stack configuration for the system

[services](services) directory contains application code for all developed services. each subdirectory is dedicated to a service  and is named accordingly. Each service contains its application code, CI pipeline tests, Dockerfile and reports directory for static code analysis reports etc.

[.devcontainer](.devcontainer) contains configuration and scripts for running a simulated production environment. This is useful if it is needed to run the whole stack as a local instance, for example for local exploratory testing or troubleshooting.

[.github](.github) contains different workflow configurations. This includes CI/CD pipelines, automatic labelers templated for issues etc.

[docs](docs) contains different kinds of documentation and planning for the project.

[sql](sql) contains DDL SQL scripts for common truth source for the layout of SQL databases, as well as script for inserting test data.


### Deployment

The project is continously deployed.

It runs on a CI [pipeline](.github/workflows/ci.yml).

Nightly workflow is the CD part of the pipeline and can be found [here](.github/workflows/nightly.yml). It runs nightly as a cron job.


The application is hosted on 2 servers:
- Test server
- Prod server

The CI/CD Pipeline uses Github actions and local runners, hosted on the servers, to perform actions, this includes building of images and testing.

For information on how to deploy the stack locally, check out the contributing.md found below.

### Contribution

[CONTRIBUTING.md](CONTRIBUTING.md) 


### License

[GNU General Public License](LICENSE)
