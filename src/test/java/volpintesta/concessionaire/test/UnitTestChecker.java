package volpintesta.concessionaire.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UnitTestChecker
{
    private static final String mainClassName = "volpintesta.concessionaire.MainClass2";
    private static final String[] mainParameters = new String[]{};
    private static final long mainFunctionExecutionTimeout = 3000; // Wait time to let the output to be checked after the main is finished.
    private static final int maxInterruptionAttempts = 100;
    private static final String testFileName = "unit-tests\\UnitTest1.utest";

    private Thread mainFunctionThread = null;
    private Thread timeoutControllerThread = null;
    private Thread checkerThread = null;

    Method mainMethod = null;

    private SyncField<Boolean> mainFunctionTimeoutReached = new SyncField<Boolean> (this, false);
    private SyncField<Boolean> timeoutThreadAbortRequested = new SyncField<Boolean> (this, false);
    private SyncField<Boolean> mainFunctionAbortRequested = new SyncField<Boolean> (this, false);
    private SyncField<Boolean> mainExecutionEnded = new SyncField<Boolean> (this, false);

    private SyncField<Throwable> mainFunctionInvokingException = new SyncField<Throwable> (this, null);
    private SyncField<Throwable> mainFunctionCodeException = new SyncField<Throwable> (this, null);
    private SyncField<Throwable> mainFunctionThreadUnexpectedError = new SyncField<Throwable> (this, null);

    private void executeMainClass()
    {
        try { mainMethod.invoke(null, (Object) mainParameters); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { mainExecutionEnded.setValue(true); }
    }

    private void executeTimeoutControllerThread()
    {
        int mainFunctionInterruptionAttempts = 0;

        boolean mainTimeoutReached = false;
        mainFunctionTimeoutReached.setValue(false);

        long mainFunctionExecutionTime = 0;
        while (mainFunctionThread != null && mainFunctionThread.isAlive()
                && !mainFunctionAbortRequested.getValue()
                && mainFunctionExecutionTime < mainFunctionExecutionTimeout)
        {
            try {
                Thread.sleep(50);
                mainFunctionExecutionTime += 50;
            } catch (InterruptedException e) {
                // Someone interrupted the sleep of this thread. Stop waiting for the main timeout.
                break;
            }
        }
        if (mainFunctionExecutionTime >= mainFunctionExecutionTimeout)
        {
            mainTimeoutReached = true;
            mainFunctionTimeoutReached.setValue(true); // put the information in a synchronized container
        }

        // IMPORTANT: from here on, this thread must complete its execution even if it's interrupted
        // because the following is important thread synchronization logic.
        // Note that even if the sleep call is interrupted by another thread, the interruption flag of the thread
        // is cleared after the InterruptedException handling, so the interruption attempt should be registered stored
        // in another variable, if it is needed.

        // If the main function timeout has been reached and the main function thread hasn't ended yet,
        // here it will be forcefully and repeatedly interrupted.
        if (mainTimeoutReached || mainFunctionAbortRequested.getValue())
        {
            while (mainFunctionThread != null && mainFunctionThread.isAlive()
                    && mainFunctionInterruptionAttempts < maxInterruptionAttempts)
            {
                mainFunctionThread.interrupt();
                mainFunctionInterruptionAttempts++;

                // Wait 1 millisecond to let the main function thread end gracefully
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) { /* do nothing */ }
            }
        }

        // Wait some time to let the main function thread end gracefully
        // and to be sure any eventually raised exception has enough time to be caught and processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) { /* do nothing */ }

        // If the main function timeout has been reached, whether the main function was successfully interrupted or not,
        // and if this thread's sleep didn't be interrupted by the checker thread after it consumed the output,
        // it means the checker thread could still be waiting for some expected output to be received from the
        // main function, so it should be interrupted because it will never be received.
        // Note that if the main function ended on its own, the checker thread would have interrupted this thread
        // after processing the output, so the timeout wouldn't have been reached.
        // Finally, the checker thread should not be interrupted if it has asked this thread to stop,
        // otherwise its subsequent join() call used to wait this thread to end gracefully would be early interrupted.
        if ((mainExecutionEnded.getValue() || mainTimeoutReached || mainFunctionAbortRequested.getValue())
                && !timeoutThreadAbortRequested.getValue())
        {
            while (checkerThread != null && checkerThread.isAlive()
                    && !timeoutThreadAbortRequested.getValue())
            {
                checkerThread.interrupt();

                // Wait 1 millisecond to let the main function thread end gracefully
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) { /* do nothing */ }
            }
        }
    }

    private void handleMainThreadException (Throwable e)
    {
        if (e != null)
        {
            if (e instanceof IllegalArgumentException // instance method called on an object of the wrong type; errors in wrapping/unwrapping primitive arguments; not corresponding actual and formal parameters
                    || e instanceof NullPointerException // the specified object is null and the method is an instance method
                    || e instanceof ExceptionInInitializerError // the initialization provoked by the method failed
            ){
                mainFunctionInvokingException.setValue(e);
            }
            else if (e instanceof RuntimeException && e.getCause() != null) // Check if method.invoke threw an exception that was wrapped inside a RuntimeException.
            {
                if (e.getCause() instanceof IllegalAccessException) // Method object is enforcing Java language access control and the underlying method is inaccessible.
                    mainFunctionInvokingException.setValue(e.getCause());
                else if (e.getCause() instanceof InvocationTargetException && e.getCause().getCause() != null)  // The underlying method throws an exception
                {
                    if (!mainFunctionAbortRequested.getValue() && !mainFunctionTimeoutReached.getValue()) // ignore exceptions called after a forced interruption
                        mainFunctionCodeException.setValue(e.getCause().getCause());
                }
                else
                    mainFunctionThreadUnexpectedError.setValue(e.getCause());
            } else {
                mainFunctionThreadUnexpectedError.setValue(e);
            }
        }
    }

    @Test
    public void makeTest()
    {
        checkerThread = Thread.currentThread();

        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;

        PipedOutputStream newStdOut = new PipedOutputStream();
        PipedInputStream newStdIn = new PipedInputStream();
        PipedOutputStream expectedOutOutputStream = new PipedOutputStream();

        try
        {
            Class<?> mainClass = Class.forName(mainClassName);
            mainMethod = mainClass.getMethod("main", String[].class);
        }
        catch (ClassNotFoundException e)
        {
            Assertions.fail("MainClass \""+mainClassName+"\" not found.");
        }
        catch (NoSuchMethodException e)
        {
            Assertions.fail("No main method found inside the MainClass \""+mainClassName+"\".");
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (t == mainFunctionThread) handleMainThreadException(e);
            }
        });

        mainFunctionThread = new Thread() {
            @Override public void run() {
                executeMainClass();
                System.out.flush(); // make sure output have been flushed to be correctly checked
            }
        };

        timeoutControllerThread = new Thread() {
            @Override public void run() {
                executeTimeoutControllerThread();
            }
        };

        try
        {
            try (    PipedOutputStream newStdInOutputStream = new PipedOutputStream(newStdIn);
                     BufferedReader newStdOutReader = new BufferedReader(new InputStreamReader(new PipedInputStream(newStdOut)));
                     BufferedReader expectedOutReader = new BufferedReader(new InputStreamReader(new PipedInputStream(expectedOutOutputStream)));    )
            {
                // redirect standard input and output into piped streams to intercept data
                System.setOut(new PrintStream(newStdOut));
                System.setIn(newStdIn);

                // Read the script file dividing input and output.
                // Enqueue the whole input to the new input stream so the main can read it with its own timing.
                // Store the whole output in a dedicated stream where it can later be read from to check the main output.
                UnitTestScriptParser.readIoScriptFile(testFileName, newStdInOutputStream, expectedOutOutputStream);
                newStdInOutputStream.flush(); // Do not close the streams to prevent the raising of different exceptions than during the normal execution.
                expectedOutOutputStream.flush();
                expectedOutOutputStream.close();

                // launch the main in another thread
                mainFunctionThread.start();
                timeoutControllerThread.start();

                // start receiving the main output and check it against the expected output
                boolean outputCheckFailed = false;
                boolean testNotEnded = false;
                String expectedOutput;
                String failureExpectedOutput = null;
                String failureActualOutput = null;
                while ((expectedOutput = expectedOutReader.readLine()) != null)
                {
                    try {
                        String actualOutput = newStdOutReader.readLine();
                        if (actualOutput == null)
                        {
                            mainFunctionAbortRequested.setValue(true);
                            testNotEnded = true;
                            break;
                        }

                        boolean testOk = expectedOutput.equals(actualOutput);

                        if (!testOk)
                        {
                            // notify the failure. In this case the data is not cleared to use it later in the assertion message
                            outputCheckFailed = true;
                            failureExpectedOutput = expectedOutput;
                            failureActualOutput = actualOutput;
                            mainFunctionAbortRequested.setValue(true);
                            break;
                        }
                    } catch (Exception e) {
                        // An IOException can be raised if the stream is closed
                        // An InterruptedIOException is called if the timeout thread interrupted a read call
                        // In any case, the check is finished,
                        testNotEnded = true;
                        break;
                    }
                }

                // wait for the main method to end
                try {
                    mainFunctionThread.join();
                } catch (InterruptedException e) { /* do nothing */ }

                timeoutThreadAbortRequested.setValue(true);
                try {
                    timeoutControllerThread.join();
                } catch (InterruptedException e) { /* do nothing */ }

                Throwable raisedException = null;
                if ((raisedException = mainFunctionInvokingException.getValue()) != null)
                {
                    Assertions.fail("A main function was found in class \""+mainClassName+"\", but it could not be called because it was not declared appropriately. " +
                            "See documentation of invoke(Object target, Object params) method inside java.lang.reflect.Method class for further information about the raised exception.", raisedException);
                }
                else if ((raisedException = mainFunctionCodeException.getValue()) != null)
                {
                    Assertions.fail("Uncaught exception during the execution of the main function code.", raisedException);
                }
                else if ((raisedException = mainFunctionThreadUnexpectedError.getValue()) != null)
                {
                    Assertions.fail("An unexpected error happened during the execution of the executor program. " +
                            "You are welcome to reproduce a dump of this error and to send it to the code's author to help improving this project quality.", raisedException);
                }
                else if (mainFunctionTimeoutReached.getValue())
                {
                    Assertions.fail("The main function execution timeout has been reached and the function has been interrupted.");
                }
                else if (testNotEnded)
                {
                    Assertions.fail("The main function execution ended before the test was completed.");
                }
                else if (outputCheckFailed)
                {
                    Assertions.assertEquals(failureExpectedOutput, failureActualOutput, "Expected output defined in the test script differs from the actual one obtained executing the main function.");
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        finally
        {
            // In case of any error, before throwing the caught exception,
            // redirect standard input and output to the original streams
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
    }

}
