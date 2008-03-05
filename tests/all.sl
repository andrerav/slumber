# Turn on "strict" mode
debug(debug() 2 | 4 | 34);

# Common code for the tests
include("tests/common.sl");

# JDBC tests
include("tests/jdbc.sl");

# Text tests
include("tests/text.sl");
