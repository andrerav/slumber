# This example shows how to connect to a MySQL database
# and execute a simple query. Refer to README for further 
# documentation.

use(^no.printf.slumber.JDBC);
$handle = dbConnect('com.mysql.jdbc.Driver', 'jdbc:mysql://localhost/database', 'user', 'pass');
$result = dbQuery($handle, 'select * from table');
while (dbAssign($result, %row)) {
    println(%row);
}