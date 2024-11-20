// Copyright (c) 2019-2024 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazon.fusion.junit.StdioTestCase;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;

public class CliTestCase
    extends StdioTestCase
{
    protected String stdoutText;
    protected String stderrText;


    @AfterEach
    public void tearDownCli()
    {
        stdoutText = null;
        stderrText = null;
    }


    void run(String... commandLine)
        throws Exception
    {
        run(0, commandLine);
    }

    void run(int expectedErrorCode, String... commandLine)
        throws Exception
    {
        int errorCode = execute(commandLine);

        assertEquals(expectedErrorCode, errorCode, "error code");

        stdoutText = stdoutToString();
        stderrText = stderrToString();
    }

    private int execute(String... commandLine)
        throws Exception
    {
        commandLine = prependGlobalOptions(commandLine);

        CommandFactory cf = new CommandFactory(stdin(), stdout(), stderr());
        return cf.executeCommandLine(commandLine);
    }

    private String[] prependGlobalOptions(String[] commandLine)
    {
        ArrayList<String> join = new ArrayList<>();

        // This is needed to run tests in the IDE.
        join.add("--repositories");
        join.add("fusion");

        join.addAll(Arrays.asList(commandLine));
        return join.toArray(new String[0]);
    }
}
