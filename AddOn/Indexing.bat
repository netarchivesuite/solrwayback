<# : chooser.bat
:: launches a File... Open sort of file chooser and outputs choice(s) to the console
:: https://stackoverflow.com/a/15885133/1683264
cd %~dp0\..\indexing

@echo off
setlocal

for /f "delims=" %%I in ('powershell -noprofile "iex (${%~f0} | out-string)"') do java -Xmx2048M -Djava.io.tmpdir=tika_tmp -jar warc-indexer-3.1.0-KB-SNAPSHOT-jar-with-dependencies.jar -c config3.conf -s  "http://localhost:8983/solr/netarchivebuilder"  "%%I"


goto :EOF

: end Batch portion / begin PowerShell hybrid chimera #>

Add-Type -AssemblyName System.Windows.Forms
$f = new-object Windows.Forms.OpenFileDialog
$f.InitialDirectory = pwd
$f.Filter = "WARC Files (*.warc)|*.warc|All Files (*.*)|*.*"
$f.ShowHelp = $true
$f.Multiselect = $true
[void]$f.ShowDialog()
if ($f.Multiselect) { $f.FileNames } else { $f.FileName }


