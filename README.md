# Gitlet
In this project you'll be implementing a version-control system that mimics some of the basic features of the popular system Git. Ours is smaller and simpler, however, so we have named it Gitlet.

A version-control system is essentially a backup system for related collections of files. The main functionality that Gitlet supports is:
- Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.
- Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.
- Viewing the history of your backups. In Gitlet, you view this history in something called the log.
- Maintaining related sequences of commits, called branches.
- Merging changes made in one branch into another.

The point of a version-control system is to help you when creating complicated (or even not-so-complicated) projects, or when collaborating with others on a project. You save versions of the project periodically. If at some later point in time you accidentally mess up your code, then you can restore your source to a previously committed version (without losing any of the changes you made since then). If your collaborators make changes embodied in a commit, you can incorporate (merge) these changes into your own version.
