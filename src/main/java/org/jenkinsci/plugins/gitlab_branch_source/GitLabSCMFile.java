package org.jenkinsci.plugins.gitlab_branch_source;

import com.fasterxml.jackson.databind.JsonMappingException;
import jenkins.scm.api.SCMFile;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabRepositoryFile;
import org.gitlab.api.models.GitlabRepositoryTree;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class GitLabSCMFile extends SCMFile {

    private TypeInfo info;
    private final GitlabAPI api;
    private final GitlabProject repo;
    private final String ref;
    private transient Object metadata;
    private transient boolean resolved;

    GitLabSCMFile(GitlabAPI api, GitlabProject repo, String ref) {
        super();
        type(Type.DIRECTORY);
        info = TypeInfo.DIRECTORY_ASSUMED; // we have not resolved the metadata yet
        this.api = api;
        this.repo = repo;
        this.ref = ref;
    }

    private GitLabSCMFile(@Nonnull GitLabSCMFile parent, String name, TypeInfo info) {
        super(parent, name);
        this.info = info;
        this.api = parent.api;
        this.repo = parent.repo;
        this.ref = parent.ref;
    }

    private GitLabSCMFile(@Nonnull GitLabSCMFile parent, String name, GitlabRepositoryTree metadata) {
        super(parent, name);
        this.api = parent.api;
        this.repo = parent.repo;
        this.ref = parent.ref;
        if (metadata.getType().equals("tree")) {
            info = TypeInfo.DIRECTORY_CONFIRMED;
            // we have not listed the children yet, but we know it is a directory
        } else {
            info = TypeInfo.NON_DIRECTORY_CONFIRMED;
            this.metadata = metadata;
            resolved = true;
        }
    }

    private Object metadata() throws IOException {
        if (metadata == null && !resolved) {
            try {
                switch (info) {
                    case DIRECTORY_ASSUMED:
                    case DIRECTORY_CONFIRMED:
                        metadata = api.getRepositoryTree(repo, getPath(), ref, false);
                        info = TypeInfo.DIRECTORY_CONFIRMED;
                        resolved = true;
                        break;
                    case NON_DIRECTORY_CONFIRMED:
                        metadata = api.getRepositoryFile(repo, getPath(), ref);
                        resolved = true;
                        break;
                    case UNRESOLVED:
                        try {
                            metadata = api.getRepositoryFile(repo, getPath(), ref);
                            info = TypeInfo.NON_DIRECTORY_CONFIRMED;
                            resolved = true;
                        } catch (IOException e) {
                            if (e.getCause() instanceof IOException
                                    && e.getCause().getCause() instanceof JsonMappingException) {
                                metadata = api.getRepositoryTree(repo, getPath(), ref, false);
                                info = TypeInfo.DIRECTORY_CONFIRMED;
                                resolved = true;
                            } else {
                                throw e;
                            }
                        }
                        break;
                }
            } catch (FileNotFoundException e) {
                metadata = null;
                resolved = true;
            }
        }
        return metadata;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(String name, boolean assumeIsDirectory) {
        return new GitLabSCMFile(this, name, assumeIsDirectory ? TypeInfo.DIRECTORY_ASSUMED: TypeInfo.UNRESOLVED);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() throws IOException {
        List<GitlabRepositoryTree> content = api.getRepositoryTree(repo, getPath(), ref, false);
        List<SCMFile> result = new ArrayList<>(content.size());
        for (GitlabRepositoryTree c : content) {
            result.add(new GitLabSCMFile(this, c.getName(), c));
        }
        return result;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        // TODO see if we can find a way to implement it
        return 0L;
    }

    @Nonnull
    @Override
    protected Type type() throws IOException, InterruptedException {
        Object metadata = metadata();
        if (metadata instanceof List) {
            return Type.DIRECTORY;
        }
        if (metadata instanceof GitlabRepositoryFile) {
            GitlabRepositoryFile content = (GitlabRepositoryFile) metadata;
            /* TODO:
            if ("symlink".equals(content.getType())) {
                return Type.LINK;
            }
            if (content.isFile()) {
                return Type.REGULAR_FILE;
            }
            return Type.OTHER;
            */
            return Type.REGULAR_FILE;
        }
        return Type.NONEXISTENT;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        Object metadata = metadata();
        if (metadata instanceof List) {
            throw new IOException("Directory");
        }
        if (metadata instanceof GitlabRepositoryFile) {
            return new ByteArrayInputStream(api.getRawBlobContent(repo,((GitlabRepositoryFile)metadata).getBlobId()));
        }
        throw new FileNotFoundException(getPath());
    }

    private enum TypeInfo {
        UNRESOLVED,
        DIRECTORY_ASSUMED,
        DIRECTORY_CONFIRMED,
        NON_DIRECTORY_CONFIRMED;
    }

}