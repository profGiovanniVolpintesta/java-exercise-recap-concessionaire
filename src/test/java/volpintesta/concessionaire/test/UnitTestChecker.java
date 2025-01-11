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
    private static final String testFileName = "unit-tests\\UnitTest1.utest";

    Method mainMethod = null;
    private Thread mainThread = null;

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

    @Test
    public void makeTest()
    {
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

        mainThread = new Thread() {
            @Override public void run() {
                executeMainClass();
                System.out.flush(); // make sure output have been flushed to be correctly checked
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
                newStdInOutputStream.flush();
                //newStdInOutputStream.close(); // Do not close the streams because the main will be interrupted later
                expectedOutOutputStream.flush();
                expectedOutOutputStream.close();

                // launch the main in another thread
                mainThread.start();
                // TODO: Gestire la terminazione anomala del main, e inserire un timeout per quando il main non risponde

                // start receiving the main output and check it against the expected output
                String expected;
                while ((expected = expectedOutReader.readLine()) != null)
                {
                    String actual = newStdOutReader.readLine();
                    assertEquals(expected, actual);
                }

                // wait for the main method to end
                try {
                    mainThread.join();
                } catch (InterruptedException e) { /* do nothing */ }
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
