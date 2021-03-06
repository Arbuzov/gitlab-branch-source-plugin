/*
 * The MIT License
 *
 * Copyright 2015 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.gitlab_branch_source;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolves the URI of a GitLab repositort from the API URI, owner and repository name.
 */
public abstract class RepositoryUriResolver {

    /**
     * Resolves the URI of a repository.
     *
     * @param apiUri     the API URL of the GitLab server.
     * @param owner      the owner of the repository.
     * @param repository the name of the repository.
     * @return the GIT URL of the repository.
     */
    @Nonnull
    public abstract String getRepositoryUri(@Nonnull String apiUri, @Nonnull String owner, @Nonnull String repository);

    /**
     * Helper method that returns the hostname of a GitLab server from its API URL.
     *
     * @param apiUri the API URL.
     * @return the hostname of a GitLab server
     */
    @Nonnull
    public static String hostnameFromApiUri(@CheckForNull String apiUri) {
        if (apiUri != null) {
            try {
                URL endpoint = new URL(apiUri);
                int p = endpoint.getPort();
                return endpoint.getHost() + (p>0 ? ":"+p : "");
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        return "gitlab.com";
    }
}
