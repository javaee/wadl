@REM
@REM An example showing how to use the commandline tools included in this
@REM package.
@REM

@mkdir .\gen-src

..\..\bin\wadl2java.bat -o ./gen-src -p com.yahoo.search -c ../share/binding.xjb ../share/YahooSearch.wadl
