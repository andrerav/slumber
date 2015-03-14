# Introduction #

In many cases, the overhead of connecting to and querying a database is the one big bottleneck in an application. This wiki page will provide some basic information about how you can improve performance with the Slumber JDBC bridge, in the form of a "do's and dont's" list.

# Before you start #

Tuning the SQL server is the first, most obvious, and likely most difficult step. Contact your database vendor, or use google to find information about tuning your database for optimal performance. Also, make sure to optimize your queries. These initial steps might give you a bigger performance boost than you expect.

# 1. Use the best fetch function #

The JDBC bridge basically provides two different types of result set fetching:
  * _Buffered_ fetch functions will load all the rows from the database into memory (buffer) at once. This will potentially use a lot of memory, but is generally the fastest method.
  * _Non-buffered_ functions will fetch one single row at the time from the SQL server, which will provide the best performance for very large result sets.

# 2. Use prepared statements #

In many cases, prepared statements will outperform "plain" statements if the same statements are executed several times in a row, only with variation in parameters.

Additionally, prepared statements adds strong protection against SQL injections. Also see Security.





