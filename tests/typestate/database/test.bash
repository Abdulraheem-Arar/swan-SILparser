#!/usr/bin/env bash

# This tests a realistic database example.
# The typestate counts the number of method calls (limited to 10 calls).

source ../../tester.bash
TYPESTATE_SPEC=spec.json
import "SQLite.SQLite.swift.sil"
test_directory
