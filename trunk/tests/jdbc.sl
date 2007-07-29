#-----------------------------------------------------------------
#
# JDBC Sleep module unit tests. Requires a MySQL database "jdbctest"
# located on localhost using default port. This database must be
# accessible using a user "jdbcuser" with password "jdbc" which
# must have all priviledges on the database.
#
# -----------------------------------------------------------------
#
# Additionally, the following tables and data are required:
#
# CREATE TABLE `files` (
#   `id` int(11) NOT NULL auto_increment,
#   `user_id` int(11) NOT NULL default '0',
#   `filename` varchar(60) NOT NULL default '',
#   PRIMARY KEY  (`id`)
# ) ENGINE=MyISAM;
#
# INSERT INTO `files` VALUES (1, 1, 'report.doc');
# INSERT INTO `files` VALUES (2, 2, 'documentation.pdf');
#
# CREATE TABLE `users` (
#   `id` int(11) NOT NULL auto_increment,
#   `name` varchar(255) NOT NULL default '',
#   `address` varchar(255) NOT NULL default '',
#   `zip` int(11) NOT NULL default '0',
#   PRIMARY KEY  (`id`)
# ) ENGINE=MyISAM;
#
# INSERT INTO `users` VALUES (1, 'Richard Peterson', 'California', 5997);
# INSERT INTO `users` VALUES (2, 'Arthur Niles', 'New York', 3386);
#
# CREATE TABLE `junk` (
#   `id` int(11) NOT NULL auto_increment,
#   `text` longtext NOT NULL,
#   `number` int(11) NOT NULL default '0',
#   PRIMARY KEY  (`id`)
# ) ENGINE=MyISAM;
# -----------------------------------------------------------------
#
# Copyright (c) Andreas Ravnestad 2005



# Include the JDBC module
use(^no.printf.slumber.JDBC);

# Enable debugging
debug(debug() | 4);
debug(debug() | 34);


#-- BEGIN UNIT TEST FRAMEWORK
# todo: add this to a separate file when possible.

# Prints a status message indicating unit test failed, and also an error message
sub failed {
    println("Unit tests FAILED: " . $1 . " ( $+ $2 $+ ) ( $+ $3 $+ s)");
}
sub success {
    println("Unit test SUCCEEDED: " . $1 . " ( $+ $2 $+ s)");
}

# Determines if a test failed or succeeded
# Usage: assert("TestName")
# Takes a subroutine as parameter and executes it.
# if it returns 1, then the test is considered succeeded.
# Otherwise the test is considered a failure, and the
# returned value from the test is assumed to be an
# error message.
sub _assert {
    local('$result $t');
    $t = ticks();
    $result = eval("return " . $1 . "();");
    $t = (ticks() - $t) / 1000.0;
    if ($result == 1) {
        success($1, $t);
    }
    else {
        failed($1, $result, $t);
    }

}
#-- END UNIT TEST FRAMEWORK

# Returns a new connection handle to the database
sub getConnectionHandle {
    local('$err $handle');
    $handle = dbConnect('com.mysql.jdbc.Driver', 'jdbc:mysql://localhost/jdbctest', 'jdbcuser', 'jdbc');
    return $handle;
}

sub verifyUserData {
    local('@data');
    @data = $1;
    if (@data[0]['id'] != 1) { return "Invalid user data"; }
    if (@data[1]['id'] != 2) { return "Invalid user data"; }
    return 1;
}
sub verifyUserDataArray {
    local('@data');
    @data = $1;
    if (@data[0][0] != 1) { return "Invalid user data"; }
    if (@data[1][0] != 2) { return "Invalid user data"; }
    return 1;
}
# Tests that we are able to open and close a connection to the database
sub test1 {
    local('$result $handle $status %row $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    dbClose($handle);
    if (checkError($err)) {
        return $err;
    }
    return 1;
}

# Uses dbFetch()
sub test2 {
    local('$result $handle $status %row @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    while (1) {
        %row = dbFetch($result);
        if (%row) {
            push(@dataStack, %row);
        }
        else {
            break;
        }
    }
    $status = verifyUserData(@dataStack);
    return $status;
}

# Uses dbAssign()
sub test3 {
    local('$result $handle $status %row @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    while (dbAssign($result, %row)) {
        push(@dataStack, %row);
    }
    $status = verifyUserData(@dataStack);
    return $status;
}

# Uses dbFetchArray()
sub test4 {
    local('$result $handle $status @row @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    while (1) {
        @row = dbFetchArray($result);
        if (@row) {
            push(@dataStack, @row);
        }
        else {
            break;
        }
    }
    $status = verifyUserDataArray(@dataStack);
    return $status;
}

# Uses dbAssignArray()
sub test5 {
    local('$result $handle $status @row @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    while (dbAssignArray($result, @row)) {
        push(@dataStack, @row);
    }
    $status = verifyUserDataArray(@dataStack);
    return $status;
}

# Uses dbFetchBuffered()
sub test6 {
    local('$result $handle $status @rows @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    @dataStack = dbFetchBuffered($result);
    $status = verifyUserData(@dataStack);
    return $status;
}

# Uses dbFetchBufferedArray()
sub test7 {
    local('$result $handle $status @rows @dataStack $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    @dataStack = dbFetchBufferedArray($result);
    $status = verifyUserDataArray(@dataStack);
    return $status;
}

# Inserts data into the junk table and verifies that it's there
# and then deletes it all again. This also verifies that the
# value returned from dbUpdate() is valid.
sub test8 {
    local('$result $handle $status @rows @dataStack $affectedRows $upd $rand %row $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $rand = int(rand(10000));
    $upd = "insert into junk (text, number) values( $+ 'ABCDF',' $+ $rand $+ ' $+ )";
    $affectedRows = dbUpdate($handle, $upd);
    if (checkError($err)) { return $err; }
    if ($affectedRows != 1) {
        return "row not inserted or dbUpdate() returned erronous value";
    }
    $result = dbQuery($handle, "select * from junk where number = $rand");
    if (checkError($err)) { return $err; }
    %row = dbFetch($result);
    if (%row['number'] ne $rand) {
        return "fetched number is not equal to inserted number";
    }
    $upd = "delete from junk where number = $rand limit 1";
    $affectedRows = dbUpdate($handle, $upd);
    if (checkError($err)) { return $err; }
    if ($affectedRows != 1) {
        return "row not deleted";
    }
    return 1;
}

# Verifies that dbFetchedRows() works as expected
sub test9 {
    local('$result $handle $status @rows @dataStack $row @row %row $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $result = dbQuery($handle, 'select * from users');
    if (checkError($err)) { return $err; }
    $status = "unknown";
    @dataStack = dbFetchBuffered($result);
    $status = verifyUserData(@dataStack);
    if (dbFetchedRows() != 2) {
        return "dbFetchedRows() returned invalid number after using dbFetchBuffered()";
    }
    $result = dbQuery($handle, 'select * from users');
    %row = dbFetch($result);
    if (dbFetchedRows() != 0) {
        return "dbFetchedRows() returned invalid number after using dbFetch()";
    }
    $result = dbQuery($handle, 'select * from users');
    @row = dbFetchArray($result);
    if (dbFetchedRows() != 0) {
        return "dbFetchedRows() returned invalid number after using dbFetchArray()";
    }
    return $status;
}

# Concurrency test
sub test10 {
    println("Performing concurrency test..");
    local('$i $id $err');
    $i = 0;
    while ($i < 10) {
        fork(&test10_branch);
        println("Forking out " . $i);
        if (checkError($err)) { return "Forking error: " . $err; }
        $i++;
    }
    println("Done, check for errors above");
    return 1;

}
sub test10_branch {
    local('$result $i $handle @dataset $id $err');
    $id = rand(1000);
    $handle = getConnectionHandle();
    if (checkError($err)) { println("Branch error: " . $err); }
    $i = 0;
    while ($i < 10) {
        $result = dbQuery($handle, 'select * from users');
        if (checkError($err)) { println("Branch error: " . $err); }
        @dataset = dbFetchBuffered($result);
        if (checkError($err)) { println("Branch error: " . $err); }
        $i++;
    }
}

# Stress test, insert and delete a bunch of rows
sub test11 {
    local('$handle $result $err $q $affectedRows $i $rand');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    while ($i < 2000) {
        $rand = rand(10000);
        $q = "insert into junk (text, number) values( $+ 'GHIJKL',' $+ $rand $+ ' $+ )";
        $affectedRows = dbUpdate($handle, $q);
        if (checkError($err)) { return $err; }
        if ($affectedRows != 1) {
            return "error while inserting row into table junk during stress test";
        }
        $q = "delete from junk where number = $rand limit 1";
        $affectedRows = dbUpdate($handle, $q);
        if (checkError($err)) { return $err; }
        if ($affectedRows != 1) {
            return "error while deleting row during stress test";
        }
        $i++;
    }
    return 1;
}

# Inserts a large number of rows, then fetches them again, then delets them.
# Both insertion and deletion is timed
sub test12 {
    local('$handle $result $rand $i $query $affectedRows @rows $err');
    $handle = getConnectionHandle();
    if (checkError($err)) { return $err; }
    $i = 0;
    while ($i < 4000) {
        $rand = rand(10000);
        $query = "insert into junk (text, number) values('MNOPQRS', ' $+ $rand $+ ')";
        dbUpdate($handle, $query);
        if (checkError($err)) { return $err; }
        $i++;
    }
    println("Inserted 4000 rows");

    $query = "select * from junk";
    $result = dbQuery($handle, $query);
    if (checkError($err)) { return $err; }
    @rows = dbFetchBuffered($result);
    if (checkError($err)) { return $err; }
    foreach %row (@rows) {
        if (%row['text'] ne 'MNOPQRS') {
            return 'Row data mismatch';
        }
    }

    $query = "delete from junk where 1";
    $affectedRows = dbUpdate($handle, $query);
    if (checkError($err)) { return $err; }
    println("Deleted $affectedRows rows");

    return 1;
}

# Prepared statements test
sub test13 {
    local('$handle $stmt $result @rows $err');
    $handle = getConnectionHandle();
	
    $stmt = dbPrepare($handle, 'select * from users where zip = ? limit 1');
    $result = dbSet($stmt, 1, 5997);
    
    # Check for errors 
    if (checkError($err)) { return $err; }
   
	# Check if value successfully set
    if ($result == 0) {
    	return 0;    	
    }
    
    # Execute statement
    $result = dbExecute($stmt);
    
    # Check for errors 
    if (checkError($err)) { return $err; }
    
    # Fetch rows
    @rows = dbFetchBuffered($result);
    if (checkError($err)) { return $err; }
    foreach %row (@rows) {
        if (%row['address'] ne 'California') {
            return 'Row data mismatch';
        }
    }
    
    return 1;
}

#Blob test


_assert("test1");
_assert("test2");
_assert("test3");
_assert("test4");
_assert("test5");
_assert("test6");
_assert("test7");
_assert("test8");
_assert("test9");
_assert("test10");
_assert("test11");
_assert("test12");
_assert("test13");

