package volpintesta.test;

import java.io.*;

public class TestScriptParser
{
    private enum IOMode
    {
        NONE, IN, OUT
    }

    public static final String inModeToken = "[IN]";
    public static final String outModeToken = "[OUT]";

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
