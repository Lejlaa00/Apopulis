# Git Best Practices

## Commonly Used Git Commands

### `git init`
Initializes a new Git repository. Use this command when starting a new project.

### `git clone <repository-url>`
Clones an existing repository from a remote server. Use this command to get a copy of an existing project.

### `git add <file>`
Stages changes to be committed. Use this command to add files to the staging area before committing.

### `git commit -m "commit message"`
Commits the staged changes with a message. Use this command to save your changes to the local repository.

### `git push`
Pushes the committed changes to a remote repository. Use this command to share your changes with others.

### `git pull`
Fetches and merges changes from a remote repository to your local repository. Use this command to update your local repository with the latest changes.

### `git status`
Displays the state of the working directory and the staging area. Use this command to see which changes have been staged, which haven't, and which files aren't being tracked by Git.

### `git branch`
Lists all branches in the repository. Use this command to see the current branches.

### `git checkout <branch>`
Switches to the specified branch. Use this command to change branches.

### `git merge <branch>`
Merges the specified branch into the current branch. Use this command to combine changes from different branches.

## Branching and Merging

### When to Use Branches
- **Feature Development**: Create a new branch for each feature you are working on. This keeps the main branch clean and stable.
- **Bug Fixes**: Create a new branch for each bug fix. This allows you to work on fixes without affecting the main branch.
- **Experimentation**: Create a new branch for experimental changes. This allows you to try out new ideas without affecting the main branch.

### When to Merge Branches
- **Completed Features**: Merge feature branches into the main branch when the feature is complete and tested.
- **Bug Fixes**: Merge bug fix branches into the main branch when the fix is complete and tested.
- **Stable Changes**: Merge experimental branches into the main branch when the changes are stable and tested.

## Best Practices

### Commit Messages
- **Be Descriptive**: Write clear and concise commit messages that describe the changes made.
- **Use Present Tense**: Use present tense in commit messages (e.g., "Add feature" instead of "Added feature").
- **Reference Issues**: Reference relevant issues or tickets in commit messages (e.g., "Fixes #123").

### Branching Strategy
- **Use a Consistent Naming Convention**: Use a consistent naming convention for branches (e.g., `feature/feature-name`, `bugfix/bug-name`).
- **Keep Branches Short-Lived**: Keep branches short-lived to reduce the complexity of merging.
- **Regularly Sync with Main Branch**: Regularly sync your branches with the main branch to avoid large merge conflicts.

### Code Reviews
- **Peer Reviews**: Have your code reviewed by peers before merging into the main branch.
- **Automated Tests**: Use automated tests to ensure code quality and catch issues early.

### Commit Message Format
Use the following format for commit messages to maintain consistency:

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **Type**: The type of change (e.g., `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`).
- **Scope**: The scope of the change (e.g., `component`, `module`, `file`).
- **Subject**: A brief description of the change.
- **Body**: (Optional) A detailed description of the change.
- **Footer**: (Optional) References to issues or tickets.

### Commit Message Types
- **feat**: A new feature for the user.
- **fix**: A bug fix.
- **docs**: Documentation only changes.
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).
- **refactor**: A code change that neither fixes a bug nor adds a feature.
- **test**: Adding missing tests or correcting existing tests.
- **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation.

### Scope
The scope provides additional context about the change. It should be a noun describing what is affected by the change. Possible options for the scope include:
- **component**: A specific component in the application.
- **module**: A specific module in the application.
- **file**: A specific file in the application.
- **api**: Changes related to the API.
- **auth**: Changes related to authentication.
- **config**: Changes related to configuration files.
- **deps**: Changes related to dependencies.
- **docs**: Changes related to documentation.
- **tests**: Changes related to tests.

Example:

```
feat(auth): add login functionality

Implement login functionality using JWT authentication.

Fixes #45
```

By following these best practices, you can maintain a clean and organized Git repository, making collaboration easier and more efficient.
