# Contributing to Sovereign Rise

Thank you for your interest in contributing to Sovereign Rise! We welcome contributions from the community.

## 🤝 How to Contribute

### Reporting Bugs
1. Check if the bug has already been reported in [Issues](https://github.com/yourusername/SovereignRise/issues)
2. If not, create a new issue with a clear title and description
3. Include steps to reproduce the bug
4. Add screenshots if applicable
5. Mention your environment (Android version, device, etc.)

### Suggesting Features
1. Check if the feature has already been suggested
2. Create a new issue with the `enhancement` label
3. Clearly describe the feature and its benefits
4. Explain why this feature would be useful to most users

### Pull Requests

#### Before Starting
1. Fork the repository
2. Clone your fork locally
3. Create a new branch for your feature/fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```

#### During Development
1. Follow the existing code style and conventions
2. Write meaningful commit messages
3. Test your changes thoroughly
4. Update documentation if needed
5. Add comments for complex logic

#### Submitting PR
1. Push your changes to your fork
2. Create a Pull Request to the `main` branch
3. Provide a clear description of your changes
4. Link related issues
5. Wait for review and address feedback

## 💻 Development Guidelines

### Kotlin Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs

### TypeScript Code Style
- Follow the ESLint configuration
- Use TypeScript types properly (avoid `any`)
- Write async/await instead of promises
- Add JSDoc comments for complex functions

### Commit Messages
Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

Example:
```
feat: add dark mode support for analytics screen
fix: resolve crash on task deletion
docs: update API endpoint documentation
```

### Testing
- Write unit tests for business logic
- Test edge cases
- Run all tests before submitting PR:
  ```bash
  # Android
  ./gradlew test
  
  # Backend
  cd backend && npm test
  ```

## 📝 Code Review Process

1. At least one maintainer will review your PR
2. Address any requested changes
3. Once approved, a maintainer will merge your PR
4. Your contribution will be credited in the release notes

## 🐛 Development Setup

Refer to the [README.md](README.md) for detailed setup instructions.

## 📋 Priority Areas

We especially welcome contributions in these areas:
- Bug fixes
- Performance improvements
- UI/UX enhancements
- Test coverage
- Documentation improvements
- Accessibility features

## ❓ Questions?

Feel free to reach out by:
- Opening an issue with the `question` label
- Contacting maintainers directly

## 📜 Code of Conduct

Be respectful and inclusive. We're all here to build something great together.

---

Thank you for contributing to Sovereign Rise! 🚀

