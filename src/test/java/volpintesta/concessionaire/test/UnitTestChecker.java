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
                        mainMethod.invoke(null, mainParameters);
                    }
                    catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
                    {
                        throw new RuntimeException(e);
                    }

                    IOMode ioMode = IOMode.NONE;
                    int lineCount = 0;

                    while (testFileReader.ready())
                    {
                        String line = testFileReader.readLine();
                        int pieceCount = 0;
                        while(line != null && !line.isEmpty())
                        {
                            int inModeTokenIndex = line.indexOf(inModeToken);
                            int outModeTokenIndex = line.indexOf(outModeToken);
                            if (inModeTokenIndex >= 0 || outModeTokenIndex >= 0)
                            {
                                // if an ioToken is found, the previous part is to be handled according to the previous mode
                                int firstTokenIndex = Math.min(Math.max(inModeTokenIndex, 0), Math.max(outModeTokenIndex, 0));
                                String piece = line.substring(0, firstTokenIndex);
                                if (!piece.isEmpty())
                                {
                                    if (ioMode == IOMode.IN)
                                    {
                                        stdInWriter.print(piece);
                                    }
                                    else if (ioMode == IOMode.OUT)
                                    {
                                        char[] chars = new char[piece.length()];
                                        int readCharsCount = stdOutReader.read(chars);
                                        String readString = new String (chars, 0, readCharsCount);

                                        assertTrue(!readString.contains("\n") && !readString.contains("\r")
                                                , String.format("Error in substring %d of line %d of test \"%s\": expected \"%s\" - found a early line break."
                                                        , pieceCount+1, lineCount+1, testFileName, piece));

                                        assertEquals(readString, piece
                                                , String.format("Error in substring %d of line %d of test \"%s\": expected \"%s\" - found \"%s\" "
                                                        , pieceCount+1, lineCount+1, testFileName, piece, readString));
                                    }
                                }
                                // TODO: Cambiare mode
                                // TODO: gestire il resto della stringa considerando l'"a capo" per l'ultimo pezzo, sia in input che in output.
                            }
                            pieceCount++;
                        }
                        lineCount++;
                    }
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



        // action
        MainClass2.main(null);

        // assertion
        // assertEquals("Hello world!\n", bos.toString());



    }

    public static void main (String[] args)
    {
        UnitTestChecker test = new UnitTestChecker();
        test.makeTest();
    }
}
