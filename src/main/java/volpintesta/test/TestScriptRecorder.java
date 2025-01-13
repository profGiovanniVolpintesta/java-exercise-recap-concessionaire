package volpintesta.test;

import java.io.*;
import java.util.Scanner;

public class TestScriptRecorder
{
    private enum ErrorCode
    {
        OK(0)
        , CANNOT_CREATE_FILE(1)
        , WRITE_ACCESS_DENIED(2)
        , CANNOT_DELETE_FILE(3)
        , WRITE_FILE_ERROR(4)
        , MISSING_ARGUMENTS(5)
        ;

        private final int value;
        private ErrorCode (int value) { this.value = value; }
        public int getValue() { return this.value; }
    }

    private static PrintWriter fileWriter = null;

    public static void main (String[] args)
    {
        if (args.length < 2)
        {
            System.err.println("Missing arguments. Required arguments: <package.MainClass> <testFilePath>");
            System.exit(ErrorCode.MISSING_ARGUMENTS.getValue());
        }
        
        String mainClassName = args[0];
        String filePath = args[1];

        File testFile = new File (filePath);
        if (testFile.exists())
        {
            if (!testFile.delete())
            {
                System.err.println("A file named \""+filePath+"\" already exists and cannot be deleted.");
                System.exit(ErrorCode.CANNOT_DELETE_FILE.getValue());
            }
        }

        try
        {
            testFile.createNewFile();
        }
        catch (IOException e)
        {
             System.err.println("I/O error during creation of file \""+filePath+"\".");
             System.exit(ErrorCode.CANNOT_CREATE_FILE.getValue());
        }
        catch (SecurityException e)
        {
            System.err.println("Write access permission denied by the security manager during the creation of file \""+filePath+"\".");
            System.exit(ErrorCode.WRITE_ACCESS_DENIED.getValue());
        }

        Thread shutdownHookThread = new Thread() {
            @Override
            public void run() {
                try
                {
                    if (fileWriter != null)
                    {
                        System.out.println("Closing file...");
                        fileWriter.flush();
                        fileWriter.close();
                        System.out.println("File closed!");
                    }
                }
                catch(Exception e)
                {
                    System.err.println("Error while closing the file\""+filePath+"\". The file could have been corrupted.");
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHookThread);

        Scanner input = new Scanner(System.in);
        try {
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
        }
        catch (IOException e)
        {
            System.err.println("Error writing test file");
            System.exit(ErrorCode.WRITE_FILE_ERROR.getValue());
        }

        String s = null;

        while (true)
        {
            try {
                s = input.nextLine();
            } catch (Exception e) {
                // Do nothing because this could happen when the program is killed with CTRL+C from command line
            }

            try {
                if (fileWriter != null && s != null)
                {
                    fileWriter.println(s);
                    s = null;
                }
            } catch (Exception e) {
                System.err.println("Error writing test file");
                System.exit(ErrorCode.WRITE_FILE_ERROR.getValue());
            }

        }


    }

}
