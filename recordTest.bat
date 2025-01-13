@ECHO OFF

IF "%~1" == "" GOTO :Missing_Params
IF "%~2" == "" GOTO :Missing_Params
IF NOT "%~3" == "" GOTO :Missing_Params

IF EXIST ".\build\classes\java\main" (
SET CLASSPATH=".\build\classes\java\main"
) ELSE (
SET CLASSPATH=".\build\classes"
)

SET MAINCLASS=volpintesta.TestScriptRecorder

@ECHO ON
java -classpath %CLASSPATH% %MAINCLASS% "%~1" "%~2"
@ECHO OFF

GOTO :End

:Missing_Params
echo Some parameters are missing. Call the command using the following parameters:
echo %~nx0 ^<package.MainClass^> ^<TestFilePath^>

:End
@ECHO ON