#!/bin/sh
# AST2J : A simple visitor generator for Java
# Copyright (c) 2000-2017 Takuo Watanabe <takuo@acm.org>

# If you put the jar file in the same directory as this script
jardir=$(cd $(dirname "${0}") && pwd)

# If you put the jar file in a specific directory
# jardir=/usr/local/share/ast2j

java -jar "${jardir}/AST2J.jar" $*
