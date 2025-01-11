package volpintesta.concessionaire.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import volpintesta.concessionaire.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UnitTestChecker {

    private enum IOMode
    {
        NONE, IN, OUT
    }

    private static final String inModeToken = "[IN]";
    private static final String outModeToken = "[OUT]";

    private static final String mainClassName = "volpintesta.concessionaire.MainClass2";
    private static final String[] mainParameters = new String[]{};
    private static final long mainTimeToInterruptMillisecs = 3000; // Wait time to let the output to be checked after the main is finished.
    private static final String testFileName = "unit-tests\\UnitTest1.utest";

    Method mainMethod = null;
    private Thread mainThread = null;
    private Throwable __uncaughtMainThreadException = null;
    private synchronized void setUncaughtMainThreadException(Throwable e) { __uncaughtMainThreadException = e; }
    public synchronized Throwable getUncaughtMainThreadException() { return __uncaughtMainThreadException; }

    private Thread mainTimerThread = null;

    private Thread checkerThread = null;

    private void executeMainClass()
    {
        try
        {
            mainMethod.invoke(null, (Object) mainParameters);
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void executeMainTimerThread()
    {
        if (mainThread != null && mainThread.isAlive())
        {
            try {
                Thread.sleep(!mainThread.isInterrupted() ? mainTimeToInterruptMillisecs : 10);
            } catch (InterruptedException e) { /* do nothing*/ }

            while (mainThread.isAlive()) {
                if (!mainThread.isInterrupted()) {
                    mainThread.interrupt(); // interrupt wait operations to let the main thread to continue.
                }

                try {
                    Thread.sleep(10); // wait for the main thread to become not alive
                } catch (InterruptedException e) { /* do nothing*/ }
            }
        }

        if (checkerThread != null && checkerThread.isAlive() && !checkerThread.isInterrupted())
        {
            Throwable mainThreadException = getUncaughtMainThreadException();
            if ((mainThread != null && mainThread.isInterrupted()) || mainThreadException != null)
            {
                // If some problems occurred with the main thread, interrupt any eventual wait operations in the checker thread
                // to let it launch the correct controls.
                checkerThread.interrupt();
            }
            else
            {
                // Otherwise, the main has ended without error, but the checker thread is still pending, maybe
                // in an I/O operation waiting for an input that will never come. It has to be interrupted, but after
                // some time to ensure it's not running some heavy computations.
                try {
                    Thread.sleep(mainTimeToInterruptMillisecs);
                } catch (InterruptedException e) { /* do nothing*/ }

                checkerThread.interrupt();
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
                if (t == mainThread)
                    setUncaughtMainThreadException(e);
            }
        });

        mainThread = new Thread() {
            @Override public void run() {
                executeMainClass();
                System.out.flush(); // make sure output have been flushed to be correctly checked
            }
        };

        mainTimerThread = new Thread() {
            @Override public void run() {
                executeMainTimerThread();
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
                readIoScriptFile(testFileName, newStdInOutputStream, expectedOutOutputStream);
                newStdInOutputStream.flush(); // Do not close the streams to prevent the raising of different exceptions than during the normal execution.
                newStdInOutputStream.close();
                expectedOutOutputStream.flush();
                expectedOutOutputStream.close();

                // launch the main in another thread
                mainThread.start();
                mainTimerThread.start();
                // TODO: Gestire la terminazione anomala del main, e inserire un timeout per quando il main non risponde

                // start receiving the main output and check it against the expected output
                boolean interruptedIO = false;
                boolean earlyEndedIO = false;
                String expected;
                String actual = null;
                while ((expected = expectedOutReader.readLine()) != null)
                {
                    try {
                        actual = newStdOutReader.readLine();
                    } catch (InterruptedIOException e) {
                        // The timer thread has stopped the current thread.
                        interruptedIO = true;
                        break; // exit from this check and serve this case later differentiating the error.
                    } catch (IOException e) {
                        // The timer thread has stopped the current thread.
                        earlyEndedIO = true;
                        break; // exit from this check and serve this case later differentiating the error.
                    }
                    assertEquals(expected, actual);
                    actual = null;
                }

                boolean tooLongMainFunction = false;
                try {
                    actual = newStdOutReader.readLine();
                    if (actual != null)
                    {
                        tooLongMainFunction = true;
                    }
                } catch (InterruptedIOException e) {
                    // The timer thread has stopped the current thread.
                    interruptedIO = true;
                } catch (IOException e) {
                    // The timer thread has stopped the current thread.
                    earlyEndedIO = true;
                }

                // wait for the main method to end
                try {
                    mainThread.join();
                } catch (InterruptedException e) { /* do nothing */ }

                Throwable mainThreadException = getUncaughtMainThreadException();
                if (mainThreadException != null) {
                    if (mainThreadException instanceof IllegalArgumentException) {
                        Assertions.fail("The main in class \"" + mainClassName + "\" has been declared with the wrong arguments.");
                    } else if (mainThreadException instanceof NullPointerException) {
                        Assertions.fail("The main in class \"" + mainClassName + "\" is not static.");
                    } else if (mainThreadException instanceof RuntimeException) {
                        RuntimeException runtimeException = (RuntimeException) mainThreadException;
                        Throwable innerException1 = runtimeException.getCause();
                        if (innerException1 instanceof IllegalAccessException) {
                            Assertions.fail("The main in class \"" + mainClassName + "\" is not accessible.");
                        } else if (innerException1 instanceof InvocationTargetException) {
                            Assertions.fail("Uncaught exception during main execution.", innerException1.getCause());
                        }
                    }
                }
                else if (mainThread != null && mainThread.isInterrupted())
                {
                    // If the main thread was interrupted without an exception, it means it remained blocked in an
                    // operation that did not raise any error. Probably it was blocked in some input because the script
                    // ended too soon.
                    Assertions.fail("The main function has been interrupted because it was unresponsive.");
                }
                else if (interruptedIO || earlyEndedIO)
                {
                    // If there is no exception, and the main thread has not been interrupted, but this thread is,
                    // the main finished while this thread was still waiting for some output.
                    // Let's check if there is some more expected output to receive, in which case there was an error.
                    if (expected != null && (actual == null || !expected.equals(actual) || expectedOutReader.readLine() != null))
                        Assertions.fail("The main function ended without completing the script execution.");
                }
                else if (tooLongMainFunction)
                {
                    Assertions.fail("The main function continued its execution after the I/O script ended its execution.");
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
            // todo: force the main to stop its execution
            // redirect standard input and output to the original streams
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
    }

    /**
     * Reads an IO script file looking for [IN] and [OUT] tokens which change the I/O mode between INPUT and OUTPUT.
     * [IN] and [OUT] tokens are then removed and the following strings are enqueued in the correct OutputStream.
     * Both in the input and in the output the exact characters sequence is preserved, including whitespace characters,
     * with the only exception of line separators (\r, \n, \r\n) which are substituted with system specific ones.
     * @param ioScriptFilePath the name of the script file
     * @param outputChannel a OutputStream used to write the outputs
     * @param inputChannel a OutputStream used to write the inputs
     * @throws FileNotFoundException if the specified file is not found
     * @throws IOException if there are IO errors while reading the file
     */
    public static void readIoScriptFile (String ioScriptFilePath, OutputStream inputChannel, OutputStream outputChannel)
            throws FileNotFoundException, IOException
    {
        if (ioScriptFilePath != null && !ioScriptFilePath.isEmpty())
        {
            try (BufferedReader testFileReader = new BufferedReader(new FileReader(ioScriptFilePath)))
            {
                BufferedWriter inputChannelWriter = inputChannel != null ? new BufferedWriter(new OutputStreamWriter(inputChannel)) : null;
                BufferedWriter outputChannelWriter = outputChannel != null ? new BufferedWriter(new OutputStreamWriter(outputChannel)) : null;
                String fileContent = "";
                IOMode ioMode = IOMode.NONE;
                boolean endedReadingFile = false;
                while (!endedReadingFile)
                {
                    String fileReaderLine = testFileReader.readLine(); // this removes the line separator from the end of the line
                    if (fileReaderLine != null)
                        fileContent += fileReaderLine + System.lineSeparator(); // restore the lost line separator
                    else
                        endedReadingFile = true;

                    // The next IO token is used to find the end of the current IO string.
                    // If the file is ended, the end of the line is used
                    IOMode nextIoMode = IOMode.NONE;
                    do
                    {
                        int inModeTokenIndex = fileContent.indexOf(inModeToken);
                        int outModeTokenIndex = fileContent.indexOf(outModeToken);
                        nextIoMode = (inModeTokenIndex < 0 && outModeTokenIndex < 0) ? IOMode.NONE
                                : (inModeTokenIndex < 0) ? IOMode.OUT
                                : (outModeTokenIndex < 0) ? IOMode.IN
                                : (inModeTokenIndex < outModeTokenIndex) ? IOMode.IN : IOMode.OUT;

                        // nextIoMode can be NONE if the file is ended because the last io string ends when the file end is reached.
                        // If, conversely, nextIoMode is NONE but the file is not ended yet, nothing has to be done
                        // because the file should be continued reading to fine the next IO token.
                        if (nextIoMode != IOMode.NONE || endedReadingFile)
                        {
                            String ioString;
                            if (nextIoMode != IOMode.NONE)
                            {
                                // an IO token has been found, to it is used as IO string delimiter.
                                int tokenIndex = nextIoMode == IOMode.IN ? inModeTokenIndex : outModeTokenIndex;
                                String token = nextIoMode == IOMode.IN ? inModeToken : outModeToken;
                                ioString = fileContent.substring(0, tokenIndex);
                                fileContent = fileContent.substring(tokenIndex + token.length());
                            }
                            else
                            {
                                // in this case the file is ended and there is are no more tokens,
                                // so the whole remaining string is used
                                ioString = fileContent;
                                fileContent = "";
                            }

                            // if ioMode is NONE, the first string is without a preceding IO token, so it's skipped
                            if (ioMode == IOMode.IN && inputChannelWriter != null)
                            {
                                try {
                                    inputChannelWriter.write(ioString);
                                    if (ioMode != nextIoMode)
                                        inputChannelWriter.flush();
                                } catch (IOException e) {
                                    inputChannelWriter = null; // do not close the stream because it has not been created in this method, but stop writing
                                }
                            }
                            else if (ioMode == IOMode.OUT && outputChannelWriter != null)
                            {
                                try {
                                    outputChannelWriter.write(ioString);
                                    if (ioMode != nextIoMode)
                                        outputChannelWriter.flush();
                                } catch (IOException e) {
                                    outputChannelWriter = null; // do not close the stream because it has not been created in this method, but stop writing
                                }
                            }

                            ioMode = nextIoMode; // prepare for the next IO string. If nextToMode is NONE, the file is also ended.
                        }
                    } while (nextIoMode != IOMode.NONE);
                }
            }
        }
    }

}
