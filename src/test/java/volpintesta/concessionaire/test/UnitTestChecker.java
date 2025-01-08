package volpintesta.concessionaire.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void makeTest()
    {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;

        PipedOutputStream newStdOut = new PipedOutputStream();
        System.setOut(new PrintStream(newStdOut));

        PipedInputStream newStdIn = new PipedInputStream();
        System.setIn(newStdIn);

        try
        {
            try
            {
                Class<?> mainClass = Class.forName(mainClassName);

                try (   BufferedReader testFileReader = new BufferedReader(new FileReader(testFileName));
                         PipedInputStream newStdOutInputStream = new PipedInputStream(newStdOut);
                         PipedOutputStream newStdInOutputStream = new PipedOutputStream(newStdIn);
                         BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(newStdOutInputStream));
                         PrintStream stdInWriter = new PrintStream(newStdInOutputStream)
                ){
                    try
                    {
                        Method mainMethod = mainClass.getMethod("main", String[].class);
                        mainMethod.invoke(null, (Object) mainParameters);
                    }
                    catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
                    {
                        throw new RuntimeException(e);
                    }

                    IOMode ioMode = IOMode.NONE;
                    int lineCount = 0;
                    String line = null;

                    do
                    {
                        int pieceCount = 0;
                        line = testFileReader.readLine();
                        if (line != null)
                        {
                            while (!line.isEmpty())
                            {
                                int inModeTokenIndex = line.indexOf(inModeToken);
                                int outModeTokenIndex = line.indexOf(outModeToken);
                                int firstTokenIndex = -1;
                                if (inModeTokenIndex >= 0 || outModeTokenIndex >= 0) // if a token is found, the string should be divided.
                                {
                                    // if an ioToken is found, the previous part is to be handled according to the previous mode
                                    firstTokenIndex = Math.min(Math.max(inModeTokenIndex, 0), Math.max(outModeTokenIndex, 0));
                                }

                                String piece = (firstTokenIndex >= 0) ? line.substring(0, firstTokenIndex) : line;

                                if (!piece.isEmpty())
                                {
                                    if (ioMode == IOMode.IN)
                                    {
                                        stdInWriter.print(piece);
                                    } else if (ioMode == IOMode.OUT)
                                    {
                                        char[] chars = new char[piece.length()];
                                        int readCharsCount = stdOutReader.read(chars);
                                        String readString = new String(chars, 0, readCharsCount);
                                        readString = readString.replace("\r\n", "\n");
                                        readString = readString.replace("\n", "\r\n");

                                        String message = (pieceCount == 0 && firstTokenIndex < 0)
                                                ? String.format("Error in part %d of line %d of test \"%s\": " , pieceCount + 1, lineCount + 1, testFileName)
                                                : String.format("Error at line %d of test \"%s\": ", lineCount + 1, testFileName);

                                        assertEquals(piece, readString, message);
                                    }
                                }

                                // update mode
                                ioMode = (inModeTokenIndex >= 0 && inModeTokenIndex < outModeTokenIndex) ? IOMode.IN : IOMode.OUT;

                                // remove the processed piece
                                int firstTokenLength = ioMode == IOMode.IN ? inModeToken.length() : outModeToken.length();
                                line = (firstTokenIndex >= 0) ? line.substring(firstTokenIndex + firstTokenLength) : "";
                                pieceCount++;
                            }
                        }
                        lineCount++;
                    } while(line != null);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
        catch (RuntimeException e)
        {
            // undo the binding in System
            System.setOut(originalOut);
            System.setIn(originalIn);
            throw e;
        }
        finally
        {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
    }
}
