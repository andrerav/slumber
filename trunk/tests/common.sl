
# Include the JDBC module
use(^no.printf.slumber.JDBC);
use(^no.printf.slumber.Text);


# Enable debugging
debug(debug() | 2);
debug(debug() | 4);
debug(debug() | 8);
debug(debug() | 24);
debug(debug() | 34);
debug(debug() | 64);


#-- BEGIN UNIT TEST FRAMEWORK
# todo: add this to a separate file when possible.

# Prints a status message indicating unit test failed, and also an error message
sub failed {
    println("Unit tests FAILED: " . $1 . " ( $+ $2 $+ ) ( $+ $3 $+ s)");
    exit();
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