// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Builder for acquiring a {@link FusionRuntime}.
 * <p>
 * <b>Instances of this class are not safe for use by multiple threads unless
 * they are {@linkplain #immutable() immutable}.</b>
 * <p>
 *
 * <h2>Configuration Properties</h2>
 *
 * This builder provides several configuration points that determine the
 * capabilities of the resulting runtime system.
 * <p>
 * Configuration properties follow the standard JavaBeans idiom in order to be
 * friendly to dependency injection systems. They also provide alternative
 * mutation methods that enable a more fluid style:
 *<pre>
 *    FusionRuntime runtime =
 *        FusionRuntimeBuilder.standard()
 *                            .withRepositoryDirectory(repo)
 *                            .build();
 *</pre>
 *
 * <h3>Bootstrap Repository<a id="bootrepo"/></h3>
 *
 * The most critical configuration property is the bootstrap repository,
 * a directory housing the primary resources needed by the runtime.
 * This directory <em>must</em> point to the {@code fusion} directory provided
 * by this library. It is acceptable for other libraries' repositories to be
 * merged with it.
 * <p>
 * If this property is not configured when {@link #build()} is called,
 * a default value is read from the {@linkplain System#getProperties()
 * system properties} using the key {@value #PROPERTY_BOOTSTRAP_REPOSITORY}.
 * If no such system property is configured, then the first repository
 * configured via {@link #addRepositoryDirectory(File)} is treated as
 * the bootstrap (and is validated to ensure that it is).
 * If no such repository is configured, {@link #build()} will fail.
 *
 * <h3>User Repositories</h3>
 *
 * Beyond the required bootstrap repository, additional repositories can be
 * configured via {@link #addRepositoryDirectory(File)}. This gives the runtime
 * additional places to look for modules and other resources. In general,
 * resources are discovered by searching the bootstrap repository first, then
 * searching other repositories in the order they were declared.
 *
 * <h3>Initial Current Directory</h3>
 *
 * Fusion's {@code current_directory} parameter holds the current working
 * directory used by many IO operations. The runtime can be configured with a
 * specific default value. If not configured when {@link #build()} is called,
 * the builder uses the value of the {@code "user.dir"} system property.
 */
public class FusionRuntimeBuilder
{
    /**
     * The property used to configure the <a href="#bootrepo">bootstrap
     * repository</a>: {@value}.
     */
    public static final String PROPERTY_BOOTSTRAP_REPOSITORY =
        "com.amazon.fusion.BootstrapRepository";


    /**
     * The standard builder of {@link FusionRuntime}s, with all configuration
     * properties having their default values.
     *
     * @return a new, mutable builder instance.
     */
    public static FusionRuntimeBuilder standard()
    {
        return new FusionRuntimeBuilder.Mutable();
    }


    //=========================================================================


    private File myCurrentDirectory;
    private File myBootstrapRepository;
    private File[] myRepositoryDirectories;
    private boolean myDocumenting;


    private FusionRuntimeBuilder() { }

    private FusionRuntimeBuilder(FusionRuntimeBuilder that)
    {
        this.myCurrentDirectory      = that.myCurrentDirectory;
        this.myBootstrapRepository   = that.myBootstrapRepository;
        this.myRepositoryDirectories = that.myRepositoryDirectories;
        this.myDocumenting           = that.myDocumenting;
    }


    //=========================================================================

    /**
     * Creates a mutable copy of this builder.
     *
     * @return a new builder with the same configuration as {@code this}.
     */
    public final FusionRuntimeBuilder copy()
    {
        return new FusionRuntimeBuilder.Mutable(this);
    }

    /**
     * Returns an immutable builder configured exactly like this one.
     *
     * @return this instance, if immutable;
     * otherwise an immutable copy of this instance.
     */
    public FusionRuntimeBuilder immutable()
    {
        return this;
    }

    /**
     * Returns a mutable builder configured exactly like this one.
     *
     * @return this instance, if mutable;
     * otherwise a mutable copy of this instance.
     */
    public FusionRuntimeBuilder mutable()
    {
        return copy();
    }

    void mutationCheck()
    {
        throw new UnsupportedOperationException("This builder is immutable");
    }


    //=========================================================================


    /**
     * Configures a builder from the given properties.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY}
     * </ul>
     *
     * @param props must not be null.

     * @return this builder, if it's immutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem applying the properties.
     */
    public FusionRuntimeBuilder withConfigProperties(Properties props)
        throws FusionException
    {
        FusionRuntimeBuilder b = this;

        String path = props.getProperty(PROPERTY_BOOTSTRAP_REPOSITORY);
        if (path != null)
        {
            File f = new File(path);

            if (! isValidBootstrapRepo(f))
            {
                String message =
                    "Not a Fusion bootstrap repository: " + path;
                throw new FusionException(message);
            }

            b = b.withBootstrapRepository(f);
        }

        return b;
    }


    /**
     * Configures a builder from properties at the given URL.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY}
     * </ul>
     *
     * @param resource may be null, in which case no configuration happens.

     * @return this builder, if it's immutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem reading the resource or
     * applying the properties.
     */
    public FusionRuntimeBuilder withConfigProperties(URL resource)
        throws FusionException
    {
        if (resource == null) return this;

        try (InputStream stream = resource.openStream())
        {
            if (stream == null) return this;

            Properties props = new Properties();
            props.load(stream);

            return withConfigProperties(props);
        }
        catch (IOException e)
        {
            String message =
                "Error reading properties from resource " + resource;
            throw new FusionException(message, e);
        }
    }


    /**
     * Configures a builder from properties in the given classloader resource.
     * The resource is located as follows:
     *<pre>
     *    classForLoading.getResource(resourceName)
     *</pre>
     *
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY}
     * </ul>
     *
     * @return this builder, if it's immutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem reading the resource or
     * applying the properties.
     *
     * @see Class#getResource(String)
     */
    public FusionRuntimeBuilder withConfigProperties(Class<?> classForLoading,
                                                     String resourceName)
        throws FusionException
    {
        URL url = classForLoading.getResource(resourceName);
        return withConfigProperties(url);
    }


    //=========================================================================


    /**
     * Gets the initial value of the {@code current_directory} parameter,
     * which is the working directory for Fusion code.
     *
     * @return an absolute path. May be null, which means the builder will use
     * the {@code "user.dir"} JVM system property when {@link #build()} is
     * called.
     *
     * @see #setInitialCurrentDirectory(File)
     * @see #withInitialCurrentDirectory(File)
     */
    public File getInitialCurrentDirectory()
    {
        return myCurrentDirectory;
    }


    /**
     * Sets the initial value of the {@code current_directory} parameter,
     * which is the working directory for Fusion code.
     *
     * @param directory may be null, which causes the builder to use the
     * {@code "user.dir"} JVM system property when {@link #build()} is called.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @see #getInitialCurrentDirectory()
     * @see #withInitialCurrentDirectory(File)
     */
    public void setInitialCurrentDirectory(File directory)
    {
        mutationCheck();

        if (! directory.isAbsolute())
        {
            directory = directory.getAbsoluteFile();
        }
        if (! directory.isDirectory())
        {
            String message = "Argument is not a directory: " + directory;
            throw new IllegalArgumentException(message);
        }

        myCurrentDirectory = directory;
    }


    /**
     * Declares the initial value of the {@code current_directory} parameter,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory may be null, which causes the builder to use the
     * {@code "user.dir"} JVM system property when {@link #build()} is called.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @see #getInitialCurrentDirectory()
     * @see #setInitialCurrentDirectory(File)
     */
    public final FusionRuntimeBuilder
    withInitialCurrentDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setInitialCurrentDirectory(directory);
        return b;
    }


    //=========================================================================


    private boolean isValidBootstrapRepo(File repo)
    {
        File src = new File(repo, "src");
        File fusionModule = new File(new File(src, "fusion"), "base.fusion");
        return fusionModule.isFile();
    }


    /**
     * Gets the directory configured for use as the bootstrap repository.
     * By default, this property is null.
     *
     * @return the bootstrap directory, or null if one has not been
     * configured.
     *
     * @see #setBootstrapRepository(File)
     * @see #withBootstrapRepository(File)
     */
    public File getBootstrapRepository()
    {
        return myBootstrapRepository;
    }


    /**
     * Sets the directory to be used as the bootstrap repository.
     *
     * @param directory the desired Fusion bootstrap repository.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @throws UnsupportedOperationException if this is immutable.
     * @throws IllegalArgumentException if the directory isn't a valid
     * bootstrap repository.
     *
     * @see #getBootstrapRepository()
     * @see #withBootstrapRepository(File)
     */
    public void setBootstrapRepository(File directory)
    {
        mutationCheck();

        if (directory != null)
        {
            if (! directory.isAbsolute())
            {
                directory = directory.getAbsoluteFile();
            }

            if (! directory.isDirectory())
            {
                String message = "Argument is not a directory: " + directory;
                throw new IllegalArgumentException(message);
            }

            if (! isValidBootstrapRepo(directory))
            {
                String message =
                    "Not a Fusion bootstrap repository: " + directory;
                throw new IllegalArgumentException(message);
            }
        }

        myBootstrapRepository = directory;
    }


    /**
     * Declares the directory to be used as the bootstrap repository,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory the desired Fusion bootstrap repository.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @throws IllegalArgumentException if the directory isn't a valid
     * bootstrap repository.
     *
     * @see #getBootstrapRepository()
     * @see #withBootstrapRepository(File)
     */
    public final FusionRuntimeBuilder withBootstrapRepository(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setBootstrapRepository(directory);
        return b;
    }


    //=========================================================================


    /**
     * Declares a repository from which Fusion modules are loaded.
     * Repositories are searched in the order they are declared.
     *
     * @param directory must be a path to a readable directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @throws UnsupportedOperationException if this is immutable.
     *
     * @see #withRepositoryDirectory(File)
     */
    public final void addRepositoryDirectory(File directory)
    {
        mutationCheck();

        if (! directory.isAbsolute())
        {
            directory = directory.getAbsoluteFile();
        }

        if (myRepositoryDirectories == null)
        {
            myRepositoryDirectories = new File[] { directory };
        }
        else
        {
            int len = myRepositoryDirectories.length;
            myRepositoryDirectories =
                Arrays.copyOf(myRepositoryDirectories, len + 1);
            myRepositoryDirectories[len] = directory;
        }
    }


    /**
     * Declares a repository from which Fusion modules are loaded,
     * returning a new mutable builder if this is immutable.
     * Repositories are searched in the order they are declared.
     *
     * @param directory must be a path to a readable directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @see #addRepositoryDirectory(File)
     */
    public final FusionRuntimeBuilder withRepositoryDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.addRepositoryDirectory(directory);
        return b;
    }


    //=========================================================================


    /** NOT FOR APPLICATION USE */
    boolean isDocumenting()
    {
        return myDocumenting;
    }

    /** NOT FOR APPLICATION USE */
    void setDocumenting(boolean documenting)
    {
        mutationCheck();
        myDocumenting = documenting;
    }


    //=========================================================================


    private FusionRuntimeBuilder fillDefaults()
    {
        FusionRuntimeBuilder b = this;
        if (b.myCurrentDirectory == null)
        {
            String userDir = System.getProperty("user.dir", "");
            if (userDir.isEmpty())
            {
                String message =
                    "Unable to determine working directory: " +
                    "the JDK system property user.dir is not set.";
                throw new IllegalStateException(message);
            }

            // Don't change the caller's instance
            b = b.withInitialCurrentDirectory(new File(userDir));
        }

        if (b.myBootstrapRepository == null)
        {
            String property = PROPERTY_BOOTSTRAP_REPOSITORY;
            String bootstrap = System.getProperty(property);
            if (bootstrap != null)
            {
                File file = new File(bootstrap);
                if (! file.isAbsolute())
                {
                    file = file.getAbsoluteFile();
                }

                if (! isValidBootstrapRepo(file))
                {
                    String message =
                        "Value of system property " + property +
                        " is not a Fusion bootstrap repository: " + bootstrap;
                    throw new IllegalArgumentException(message);
                }

                b = b.withBootstrapRepository(file);
            }
        }

        return b.immutable();
    }


    /**
     * Builds a new runtime based on the current configuration of this builder.
     *
     * @return a new builder instance.
     */
    public FusionRuntime build() // TODO throws FusionException
    {
        FusionRuntimeBuilder b = fillDefaults();
        return new StandardRuntime(b);
    }


    private void addBootstrapRepository(List<ModuleRepository> repos)
    {
        ModuleRepository repo = null;

        if (myBootstrapRepository != null)
        {
            // TODO FUSION-172 this shouldn't happen here
            File src = new File(myBootstrapRepository, "src");
            repo = new FileSystemModuleRepository(src);
        }
        else
        {
            // TODO FUSION-172 remove this
            repo = new JarModuleRepository();
        }

        if (repo != null) repos.add(repo);
    }


    /**
     * NOT PUBLIC!
     */
    ModuleRepository[] buildModuleRepositories()
    {
        ArrayList<ModuleRepository> repos = new ArrayList<>();

        addBootstrapRepository(repos);

        boolean needBootstrap = repos.isEmpty();

        if (myRepositoryDirectories != null)
        {
            for (File f : myRepositoryDirectories)
            {
                if (needBootstrap)
                {
                    if (! isValidBootstrapRepo(f))
                    {
                        String message =
                            "The first repository is not a Fusion bootstrap " +
                            "repository: " + f;
                        throw new IllegalArgumentException(message);
                    }
                    needBootstrap = false;
                }

                // TODO Fusion-172 remove this
                File src = new File(f, "src");
                if (src.isDirectory())
                {
                    f = src;
                }

                repos.add(new FileSystemModuleRepository(f));
            }
        }

        int size = repos.size();
        if (size == 0)
        {
            throw new IllegalStateException("No repositories have been declared");
        }

        return repos.toArray(new ModuleRepository[size]);
    }


    //=========================================================================


    private static final class Mutable extends FusionRuntimeBuilder
    {
        public Mutable() { }

        public Mutable(FusionRuntimeBuilder that)
        {
            super(that);
        }

        @Override
        public FusionRuntimeBuilder immutable()
        {
            return new FusionRuntimeBuilder(this);
        }

        @Override
        public FusionRuntimeBuilder mutable()
        {
            return this;
        }

        @Override
        void mutationCheck()
        {
        }
    }
}
