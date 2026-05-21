How I pushed the baseline to GitHub

Commands run locally in `/home/veryl/Hades/1project/hercules`:

```bash
git remote add origin https://github.com/NyxUltor/Hercules.git
git fetch origin
git rebase origin/main
git push -u origin main
```

Notes:
- The remote had an initial README/LICENSE; I rebased local changes on top of `origin/main` and then pushed.
- Use `git status` and `git log` to inspect current state before further pushes.
- Prefer feature branches for new work:

```bash
git checkout -b feat/restore-autosave
# make changes
git add .
git commit -m "feat(autosave): restore from draft"
git push -u origin feat/restore-autosave
```