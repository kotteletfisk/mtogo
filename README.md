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

### Contribution

[CONTRIBUTING.md](CONTRIBUTING.md) 


### License

[GNU General Public License](LICENSE)
