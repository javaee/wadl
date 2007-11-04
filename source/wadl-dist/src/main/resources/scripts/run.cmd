#
# An example showing how to use the commandline tools included in this
# package.
#

mkdir .\gen-src

..\..\bin\wadl2java.cmd -o ./gen-src -p com.yahoo.search ..\share\YahooSearch.wadl