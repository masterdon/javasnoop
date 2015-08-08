@echo off
SETLOCAL ENABLEEXTENSIONS
rem ---------------------------------------------------------------------------
rem Startup Script for JavaSnoop
rem	Modified 08/008/2015 Donald Raikes <donraikes1030@gmail.com>
rem ---------------------------------------------------------------------------

:main
	set /p JAVA_HOME=path to jdk? 
	set unsafe_policy=resources\unsafe.policy
	set safe_policy=resources\safe.policy
	set user_policy=%USERPROFILE%\.java.policy
	echo [1] JAVA_HOME is at: %JAVA_HOME%
	copy "%JAVA_HOME%\lib\tools.jar" .\lib\tools.jar >NUL
	set JDK_EXEC=%JAVA_HOME%\bin\java.exe
	echo JDK_EXEC = %JDK_EXEC%
	call :DisableSecurity
	Call :JavaSnoop
	call :EnableSecurity

:DisableSecurity
	echo [3] Turning off Java security for JavaSnoop usage.
	del %user_policy% 2>NUL
	copy %unsafe_policy% %user_policy% >NUL
	GOTO :EOF

:JavaSnoop
	echo [4] Starting JavaSnoop
	%JAVA_HOME%\bin\java.exe -jar JavaSnoop.jar
	GOTO :EOF

:EnableSecurity
	echo [5] Turning Java security back on for safe browsing.
	del %user_policy% 2> NUL
	copy %safe_policy% %user_policy% > NUL
	GOTO :EOF

endlocal

