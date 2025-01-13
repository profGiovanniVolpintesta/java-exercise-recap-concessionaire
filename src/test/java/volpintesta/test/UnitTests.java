package volpintesta.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnitTests
{
    private static final long mainFunctionExecutionTimeout = 3000; // Wait time to let the output to be checked after the main is finished.
    private static final int maxInterruptionAttempts = 100;

    private void makeTest(String mainClassName, String[] mainArgs, String testScriptFilePath)
    {
        TestScriptExecutor executor = new TestScriptExecutor(mainClassName, mainArgs, testScriptFilePath);
        executor.makeTest();
        if (executor.hasFailed())
        {
            Throwable failureError = executor.getFailureError();
            if (failureError != null)
                Assertions.fail(executor.getFailureMessage(), failureError);
            else
                Assertions.fail(executor.getFailureMessage());
        }
    }

    @Test
    public void Test1()
    {
        String mainClassName = "volpintesta.concessionaire.MainClass2";
        String[] mainFunctionArgs = new String[]{};
        String testScriptFilePath = "unit-tests\\UnitTest1.utest";

        makeTest(mainClassName, mainFunctionArgs, testScriptFilePath);
    }
}
