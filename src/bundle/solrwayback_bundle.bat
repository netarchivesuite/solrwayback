@echo off
REM Check if an argument was provided
IF "%~1"=="" (
    echo Usage: %~nx0 ^{start^|stop^}
    exit /b 1
)

REM Main case handling
IF /I "%~1"=="start" (
    echo Starting solr...
    call solr-9\bin\solr.cmd start -c -m 4g

    echo Starting SolrWayback in tomcat...

    REM Set CATALINA_HOME to the "tomcat-9" folder inside the current directory
    set "CATALINA_HOME=%cd%\tomcat-9"

    call tomcat-9\bin\startup.bat

    echo.
    echo Started SolrWayback
    GOTO :eof
)

IF /I "%~1"=="stop" (
    echo Stopping SolrWayback in tomcat...

    REM Set CATALINA_HOME to the "tomcat-9" folder inside the current directory
    set "CATALINA_HOME=%cd%\tomcat-9"
    call tomcat-9\bin\shutdown.bat

    echo Stopping solr...
    call solr-9\bin\solr.cmd stop -all

    echo.
    echo Stopped SolrWayback
    GOTO :eof
)

REM Invalid option
echo Invalid option: %~1
echo Usage: %~nx0 ^{start^|stop^}
exit /b 2
